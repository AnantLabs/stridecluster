package com.lin.stride.zk;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;

import com.lin.stride.client.ServerNodeListener;
import com.lin.stride.utils.ConfigReader;
import com.lin.stride.utils.ZKIndexVersionTools;
import com.lin.stride.utils.ZKUtils;

/**
 * 
 * @Author : xiaolin
 * @Date : 2012-9-25 上午10:34:29
 */
public final class SZKClientImpl implements StrideZookeeperClient {
	private final ZooKeeper zookeeper;
	private final String liveNodesPath = ConfigReader.INSTANCE().getZkLiveNodePath();
	private final Logger LOG = Logger.getLogger(SZKClientImpl.class);
	private final ServerNodeListener nodeListener;

	public SZKClientImpl(ServerNodeListener nodeListener) throws IOException {
		zookeeper = new ZooKeeper(ConfigReader.INSTANCE().getZkServers(), 3000, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				LOG.info(event.getState());
			}
		});
		this.nodeListener = nodeListener;
	}

	@Override
	public List<String> start() throws KeeperException, InterruptedException {
		ZKUtils.initialPersistentPath(zookeeper, liveNodesPath, ZKIndexVersionTools.versionToBytes(0, 0));
		List<String> live_nodes = zookeeper.getChildren(liveNodesPath, new Watcher() {// 同步注册方法得到列表,为client端初始化indexList提供数据
					@Override
					public void process(WatchedEvent event) {
						if (event.getType() == EventType.NodeChildrenChanged) {
							List<String> liveNodes;
							try {
								liveNodes = zookeeper.getChildren(event.getPath(), this);
								nodeListener.changeEvent(liveNodes);
							} catch (KeeperException | InterruptedException e) {
								LOG.error(e.getMessage(), e);
							}// 持久监听
							LOG.info("index server change!!");
						}
					}
				});
		return live_nodes;
	}

	@Override
	public void close() {
		try {
			zookeeper.close();
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}

}
