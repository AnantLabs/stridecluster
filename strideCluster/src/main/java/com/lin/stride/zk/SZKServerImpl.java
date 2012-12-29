package com.lin.stride.zk;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.CheckIndex.Status;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
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

import com.lin.stride.hdfs.HDFSShcheduler;
import com.lin.stride.index.IndexBuilder;
import com.lin.stride.index.IndexBuilderMysqlImpl;
import com.lin.stride.server.IndexUpdateListener;
import com.lin.stride.utils.ConfigReader;
import com.lin.stride.utils.ZKIndexVersionTools;
import com.lin.stride.utils.ZKUtils;

/**
 * @author xiaolin Date:2012-09-19
 */
public final class SZKServerImpl implements StrideZooKeeperServer {

	private final ZooKeeper zookeeper;
	private final Logger LOG = Logger.getLogger(SZKServerImpl.class);
	private final Stat stat = new Stat();
	private final String hostName;// 声明一个hostname是为了在做leader election时,发现自己是leader.
	private final LeaderElectionSupport les = new LeaderElectionSupport();
	private final AtomicBoolean isLeader = new AtomicBoolean(false);
	private final IndexUpdateListener switchIndexCallBack;
	private byte[] currentUpdateState;
	/**
	 * 本地保存一份最新更新的日期和索引数,使用时,不必每次都去zk获得.构造函数在实例化时,从zk得到该值.
	 * leader在building_upLoadIndex()计算完索引后更新
	 * follower在updateStateNodeWatcher得到更新通知的时候更新.这样能保证这个值一直是最新的.
	 */
	private byte[] latestVersion;

	/**
	 * 初始化一个server端实例,专门服务index服务器 Date : 2012-12-21 上午11:29:11
	 * 
	 * @param sicb
	 *            回调类
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public SZKServerImpl(IndexUpdateListener sicb) throws Exception {
		InetAddress inetAddress = InetAddress.getLocalHost();
		hostName = inetAddress.getHostName() + ":" + ConfigReader.INSTANCE().getServerPort();

		zookeeper = new ZooKeeper(ConfigReader.INSTANCE().getZkServers(), 3000, new Watcher() {
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
		les.setRootNodeName(ConfigReader.INSTANCE().getZkLeaderElectionPath());
		les.addListener(new LeaderElectionAware() {
			@Override
			public void onElectionEvent(org.apache.zookeeper.recipes.leader.LeaderElectionSupport.EventType eventType) {
				// EventType有很多类型,只有在选举完成后,才更新状态
				if (eventType == org.apache.zookeeper.recipes.leader.LeaderElectionSupport.EventType.ELECTED_COMPLETE) {
					try {
						if (hostName.equals(les.getLeaderHostName())) {
							//完成老leader没完成的事,或者需要reset一些东西
							isLeader.set(true);
						}
					} catch (KeeperException | InterruptedException e) {
						LOG.error(e.getMessage(), e);
					}
				}
			}

		});
		les.start();// 选举leader end 

		/*if(isLeader()){ //异常需要处理
			ZKUtils.initialPersistentPath(zookeeper);
		}*/

		latestVersion = ZKUtils.getData(zookeeper, ConfigReader.INSTANCE().getZkLiveNodePath());

		HDFSShcheduler hdfs = new HDFSShcheduler();
		File localRootDir = new File(ConfigReader.INSTANCE().getIndexStorageDir());
		String[] dirs = localRootDir.list();
		File latestLocalIndex = null;
		if (dirs.length != 0) {
			Arrays.sort(dirs);
			latestLocalIndex = new File(localRootDir, dirs[dirs.length - 1]);
		}
		boolean sameVersion = hdfs.latestVersion() == ZKIndexVersionTools.bytesToCtime(latestVersion);
		long zkVersion = ZKIndexVersionTools.bytesToCtime(latestVersion);
		if (isLeader()) {
			if (!sameVersion) {//如果hdfs和zk的版本不一致了,真个系统就乱了,所以一旦不一致,马上重新创建.恢复版本
				LOG.debug("version is invalid , ready rebuild index ...");
				building_upLoadIndex();
				zookeeper.setData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.UPDATE.getBytes(), -1);
			} else if (latestLocalIndex == null || zkVersion > Long.parseLong(latestLocalIndex.getName())) {
				LOG.debug("local index than hdfs old , ready download latest index ");
				hdfs.downLoadIndex(ZKIndexVersionTools.bytesToCtime(latestVersion));
			} else if (zkVersion < Long.parseLong(latestLocalIndex.getName())) {
				LOG.debug("local index than hdfs new , check local index ...");
				Directory dir = FSDirectory.open(new File(localRootDir, dirs[dirs.length - 1]));
				CheckIndex ci = new CheckIndex(dir);
				Status status = ci.checkIndex();
				if (status.clean) {
					LOG.debug("local index verify success!");
					IndexReader reader = DirectoryReader.open(dir);
					latestVersion = ZKIndexVersionTools.versionToBytes(reader.maxDoc(), Long.parseLong(latestLocalIndex.getName()));
					reader.close();
					LOG.debug("update zookeeper newest version!");
					zookeeper.setData(ConfigReader.INSTANCE().getZkLiveNodePath(), latestVersion, -1);
					zookeeper.setData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.UPDATE.getBytes(), -1);
				} else {
					building_upLoadIndex();
				}
				dir.close();
			}
		} else {
			if (!sameVersion) {
				LOG.debug("version is invalid , build empty index !");
				latestVersion = ZKIndexVersionTools.versionToBytes(0, 0);
				Directory dir = FSDirectory.open(new File(localRootDir, "0"));
				IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, new StandardAnalyzer(Version.LUCENE_40));
				iwc.setOpenMode(OpenMode.CREATE);
				IndexWriter iw = new IndexWriter(dir, iwc);
				iw.commit();
				iw.close();
				dir.close();
			} else if (latestLocalIndex == null || zkVersion > Long.parseLong(latestLocalIndex.getName())) {
				/*
				 * 如果索引比较大,leader正在重建索引,UpdateStatus=rebuild,还没有吧文件件下载完成,这时leader已经发出更新通知
				 * 那么这时这个节点还认为自己是正常的节点进行注册,导致这台节点更新失败.
				 */
				hdfs.downLoadIndex(ZKIndexVersionTools.bytesToCtime(latestVersion));
			} else if (zkVersion < Long.parseLong(latestLocalIndex.getName())) {
				LOG.debug("local index than hdfs new , build empty index !");
				latestVersion = ZKIndexVersionTools.versionToBytes(0, 0);
				Directory dir = FSDirectory.open(new File(localRootDir, "0"));
				IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, new StandardAnalyzer(Version.LUCENE_40));
				iwc.setOpenMode(OpenMode.CREATE);
				IndexWriter iw = new IndexWriter(dir, iwc);
				iw.commit();
				iw.close();
				dir.close();
			}
		}

		hdfs.close();

		updateStateNodeWatche();//监听更新状态

		switchIndexCallBack = sicb;// 得到一个 callback
	}

	@Override
	public boolean isLeader() {
		return isLeader.get();
	}

	/**
	 * index服务器向zk服务器注册,客户端如果有watcher监控live_nodes,将会得到新注册的这台服务器.
	 * 默认使用本机的ip加配置文件中的端口.
	 */
	@Override
	public void registerLiveNode() {
		try {
			if (ZKIndexVersionTools.zeroVersion(latestVersion)) {
				LOG.warn("[" + hostName + "] index invalid, node becoming invisible , wating update ......");
			} else {
				zookeeper.create(ConfigReader.INSTANCE().getZkLiveNodePath() + "/" + hostName, latestVersion, Ids.OPEN_ACL_UNSAFE,
						CreateMode.EPHEMERAL);
				LOG.info("register IndexServer - [" + hostName + "]");
			}
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 对zk_update_status节点进行监控,如果变化,做出对应操作. 如果状态为 rebuild,说明需要更新索引,
	 */
	public void updateStateNodeWatche() {
		try {
			currentUpdateState = zookeeper.getData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == EventType.NodeDataChanged) {
						try {
							currentUpdateState = zookeeper.getData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), this, stat);
							String value = new String(currentUpdateState);
							// 如果这个节点follower,那么更新,因为leader已经更新完成了.
							if (value.equals(ClusterState.UPDATE.toString()) && isLeader.get() == false) {
								//得到索引的最新版本.通过得到的版本来更新索引
								latestVersion = ZKUtils.getData(zookeeper, ConfigReader.INSTANCE().getZkLiveNodePath());
								prepareDownLoadIndex(latestVersion); // 如果得到更新通知,并且通知信息为update,那么进行索引更新.
								// 如果得到重建索引的通知,并且还是leader的话,那么这个节点负责更新索引,并上传到HDFS中.
							} else if (value.equalsIgnoreCase(ClusterState.REBUILD.toString()) && isLeader.get() == true) {
								unavailableService(); // 先把Leader服务器下线
								try {
									building_upLoadIndex();
									switchIndexCallBack.switchIndex(ZKIndexVersionTools.bytesToCtime(latestVersion));
									registerLiveNode();
									zookeeper.setData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.UPDATE.getBytes(), -1);
								} catch (Exception e) {
									LOG.error(e.getMessage(), e);
								} // 更新并Upload HDFS
							} else if (value.equals(ClusterState.NORMAL.toString()) && isLeader.get() == true) {
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
	private void prepareDownLoadIndex(final byte[] version) {
		WriteLock wlock = new WriteLock(zookeeper, ConfigReader.INSTANCE().getZkUpdateLockPath(), Ids.OPEN_ACL_UNSAFE);
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
					HDFSShcheduler hdfsTools = new HDFSShcheduler();
					hdfsTools.downLoadIndex(ZKIndexVersionTools.bytesToCtime(version));// 从HDFS上拉取数据,过程...................
					hdfsTools.close();
					boolean successful = switchIndexCallBack.switchIndex(ZKIndexVersionTools.bytesToCtime(version));
					LOG.info(hostName + " - search service startup !");
					registerLiveNode();
				} catch (IOException e) {
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
			//如果更新完发现没有人竞争竞争锁了,那么说明当前节点是最后一个更新的,更新后,要把状态恢复.
			List<String> liveLocks = zookeeper.getChildren(ConfigReader.INSTANCE().getZkUpdateLockPath(), false);
			if (liveLocks.size() == 0) {
				zookeeper.setData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.NORMAL.getBytes(), -1);
			}
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	/**
	 * 从数据库读取信息建立索引,索引构建完成后,上传索引文件到HDFS中. Date : 2012-10-12
	 * 
	 * @throws Exception
	 */
	private void building_upLoadIndex() throws Exception {
		LOG.info("begin rebuilding index ......");
		IndexBuilder ib = new IndexBuilderMysqlImpl();
		latestVersion = ib.rebuild();
		LOG.info("rebuilding index complete !");

		HDFSShcheduler hdfsTools = new HDFSShcheduler();
		// 这需要判断是否所有的机器都更新完了,如果没有,需要等待...
		hdfsTools.upLoadIndex(ZKIndexVersionTools.bytesToCtime(latestVersion));// 上传,传入要上传的新目录
		hdfsTools.close();
		zookeeper.setData(ConfigReader.INSTANCE().getZkLiveNodePath(), latestVersion, -1);
	}

	/**
	 * 下线删除当前hostName的节点就可以.
	 */
	@Override
	public void unavailableService() {
		try {
			// add 2012-12-27 因为follower启动时,可能没有注册,所以在leader发出更新通知时,没有注册的follower不用删除livenode下的自己,因为根本没有
			if(zookeeper.exists(ConfigReader.INSTANCE().getZkLiveNodePath() + "/" + hostName, false)!=null){
				zookeeper.delete(ConfigReader.INSTANCE().getZkLiveNodePath() + "/" + hostName, -1);
				LOG.info(hostName + " - search service stop !");
			}
		} catch (InterruptedException | KeeperException e) {
			LOG.error(e.getMessage(), e);
		}
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

	public static void main(String[] args) throws Exception {

		ZooKeeper zookeeper = new ZooKeeper(ConfigReader.INSTANCE().getZkServers(), 3000, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				// zk只有失效或者链接不可用时才报警
				if (event.getState() == KeeperState.Expired || event.getState() == KeeperState.Disconnected) {
					System.out.println("session Expired");
				}
			}
		});

		zookeeper.setData(ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.REBUILD.getBytes(), -1);
		zookeeper.close();
	}

	@Override
	public long getLatestDir() {
		return ZKIndexVersionTools.bytesToCtime(latestVersion);
	}

}
