package com.lin.stride.zk;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.recipes.leader.LeaderElectionAware;
import org.apache.zookeeper.recipes.leader.LeaderElectionSupport;
import org.apache.zookeeper.recipes.lock.LockListener;
import org.apache.zookeeper.recipes.lock.WriteLock;

import com.lin.stride.hdfs.HDFSFileSystem;
import com.lin.stride.index.IndexBuilder;
import com.lin.stride.index.IndexBuilderMysqlImpl;
import com.lin.stride.search.server.SwitchIndexCallBack;
import com.lin.stride.utils.ConfigReader;
import com.lin.stride.utils.IntTools;

/**
 * @author xiaolin Date:2012-09-19
 */
public class SZKServerImpl implements StrideZooKeeperServer {

	private ZooKeeper zookeeper;
	private final Logger LOG = Logger.getLogger(SZKServerImpl.class);
	private final String liveNodesPath = ConfigReader.getEntry("zk_live_nodes");
	private final String updateLockPath = ConfigReader.getEntry("zk_update_lock");
	private final String updateStatusPath = ConfigReader.getEntry("zk_update_status");
	private final String leaderElectionPath = ConfigReader.getEntry("zk_leader_election");
	private final Stat stat = new Stat();
	private String hostName;// 声明一个hostname是为了在做leader election时,发现自己是leader.
	private final LeaderElectionSupport les = new LeaderElectionSupport();
	private final AtomicBoolean isLeader = new AtomicBoolean(false);
	private final SwitchIndexCallBack switchIndexCallBack;
	private byte[] currentUpdateState;

	/**
	 * 初始化一个server端实例,专门服务index服务器
	 */
	public SZKServerImpl(SwitchIndexCallBack sicb) {
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			hostName = inetAddress.getHostName() + ":" + ConfigReader.getEntry("serverport");

			zookeeper = new ZooKeeper(ConfigReader.getEntry("zk_servers"), 3000, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					// zk只有失效或者链接不可用时才报警
					if (event.getState() == KeeperState.Expired || event.getState() == KeeperState.Disconnected) {
						LOG.warn("session Expired");
					}
				}
			});
			// 选举leader
			les.setHostName(hostName);
			les.setZooKeeper(zookeeper);
			les.setRootNodeName(leaderElectionPath);
			les.addListener(new LeaderElectionAware() {
				@Override
				public void onElectionEvent(org.apache.zookeeper.recipes.leader.LeaderElectionSupport.EventType eventType) {
					// EventType有很多类型,只有在选举完成后,才更新状态
					if (eventType == org.apache.zookeeper.recipes.leader.LeaderElectionSupport.EventType.ELECTED_COMPLETE) {
						try {
							if (hostName.equals(les.getLeaderHostName())) {
								isLeader.set(true);
							}
						} catch (KeeperException | InterruptedException e) {
							LOG.error(e.getMessage(), e);
						}
					}
				}

			});
			les.start();// 选举leader end
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		switchIndexCallBack = sicb;// 得到一个 callback
		registerLiveNode();
		updateStateNodeWatch();
	}

	/**
	 * index服务器向zk服务器注册,客户端如果有watcher监控live_nodes,将会得到新注册的这台服务器.
	 * 默认使用本机的ip加配置文件中的端口.
	 */
	@Override
	public void registerLiveNode() {
		this.registerLiveNode(hostName);
	}

	/**
	 * 通过制定hostname来注册当前服务器,默认使用本机的hostname注册.
	 */
	@Override
	public void registerLiveNode(String hostName) {
		this.hostName = hostName;
		try {
			zookeeper.create(liveNodesPath + "/" + hostName, IntTools.intToByteArray(0), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 对zk_update_status节点进行监控,如果变化,做出对应操作.
	 * 如果状态为 rebuild,说明需要更新索引,
	 */
	public void updateStateNodeWatch() {
		try {
			currentUpdateState = zookeeper.getData(updateStatusPath, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == EventType.NodeDataChanged) {
						try {
							currentUpdateState = zookeeper.getData(updateStatusPath, this, stat);
							String value = new String(currentUpdateState);
							// 如果这个节点不是leader,是普通节点,那么更新,因为leader已经更新完成了.
							if (value.equalsIgnoreCase("update") && isLeader.get() == false) {
								downLoadIndex(); // 如果得到更新通知,并且通知信息为update,那么进行索引更新.
								// 如果得到重建索引的通知,并且还是leader的话,那么这个节点负责更新索引,并上传到HDFS中.
							} else if (value.equalsIgnoreCase("rebuild") && isLeader.get() == true) {
								unavailableService(); // 先把Leader服务器下线
								try {
									switchIndexCallBack.clearIndexFile();
									upLoadIndex();
									switchIndexCallBack.switchIndex();
									availableService(); // 再把leader服务器上线
									zookeeper.setData(updateStatusPath, ClusterState.UPDATE.getBytes(), -1);
								} catch (Exception e) {
									LOG.error(e.getMessage(), e);
								} // 更新并Upload HDFS
							} else if (value.equalsIgnoreCase("normal") && isLeader.get() == true) {
								// 检查一下索引是不是一致,live_nodes下面的节点的value存储数据量和日期

							}
						} catch (KeeperException | InterruptedException e) {
							LOG.error(e.getMessage(), e);
						}
					}
				}
			}, stat);
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 获得更新锁,得到锁后更新索引. 每次调用方法时,才创建一个WriteLock,如果更新频繁,可以声明一个成员变量
	 * 目前设计更新不是很频繁,所以声明为局部变量.
	 */
	private void downLoadIndex() {
		WriteLock wlock = new WriteLock(zookeeper, updateLockPath, Ids.OPEN_ACL_UNSAFE);
		final CountDownLatch latch = new CountDownLatch(1);
		wlock.setLockListener(new LockListener() {
			/**
			 * 此方法在lock.unlock执行到最后时调用
			 */
			@Override
			public void lockReleased() {
				LOG.info(hostName + " Released the updatelock");
			}

			@Override
			public void lockAcquired() {
				LOG.info(hostName + " Acquired the updatelock");
				unavailableService();// 先把服务器下线
				try {// 都放在try中,如果清除索引失败,那么这个节点就不上线,也不更新了
					switchIndexCallBack.clearIndexFile();
					HDFSFileSystem hdfsTools = new HDFSFileSystem();
					hdfsTools.downLoadIndex();// 从HDFS上拉取数据,过程...................
					hdfsTools.close();
					int rowNum = switchIndexCallBack.switchIndex();
					availableService();// 再把服务器上线
					zookeeper.setData(liveNodesPath + "/" + hostName, IntTools.intToByteArray(rowNum), -1);
					// 数据下载下来后,更新节点的value,使节点的信息为更新的数据量
				} catch (KeeperException | InterruptedException | IOException e) {
					LOG.error(e.getMessage(), e);
				}
				latch.countDown();
			}
		});
		try {
			wlock.lock();
			latch.await();
			wlock.unlock();
			wlock.close();
		} catch (KeeperException | InterruptedException e1) {
			LOG.error(e1.getMessage(), e1);
		}
		try {
			List<String> locks = zookeeper.getChildren(updateLockPath, false);
			if (locks.size() == 0) {
				zookeeper.setData(updateStatusPath, ClusterState.NORMAL.getBytes(), -1);
			}
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 从数据库读取信息建立索引,索引构建完成后,上传索引文件到HDFS中.
	 * Date : 2012-10-12
	 * @throws Exception
	 */
	private void upLoadIndex() throws Exception {
		IndexBuilder ib = new IndexBuilderMysqlImpl();
		ib.rebuild();
		HDFSFileSystem hdfsTools = new HDFSFileSystem();
		hdfsTools.upLoadIndex();// 上传
		hdfsTools.close();
	}

	/**
	 * 下线删除当前hostName的节点就可以.
	 */
	@Override
	public void unavailableService() {
		try {
			zookeeper.delete(liveNodesPath + "/" + hostName, -1);
			LOG.info(hostName + " - search service stop !");
		} catch (InterruptedException | KeeperException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 上线重新注册一下就OK.
	 */
	@Override
	public void availableService() {
		LOG.info(hostName + " - search service startup !");
		registerLiveNode();
	}

	@Override
	public String getCurrentUpdateState() {
		return new String(currentUpdateState);
	}

	@Override
	public void close() {
		les.stop();
		try {
			zookeeper.close();
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

}
