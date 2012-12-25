package com.lin.stride.zk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;

import com.lin.stride.utils.ConfigReader;
/**
 * 
 * @Author : xiaolin
 * @Date : 2012-9-25 上午10:34:29
 */
public final class SZKClientImpl implements StrideZookeeperClient{
	private ZooKeeper zookeeper;
	private final String liveNodesPath = ConfigReader.INSTANCE().getZkLiveNodePath();
	private final BlockingQueue<List<String>> liveNodesQueue = new ArrayBlockingQueue<List<String>>(10);
	private final Logger LOG = Logger.getLogger(SZKClientImpl.class);

	public SZKClientImpl() {
		try {
			zookeeper = new ZooKeeper(ConfigReader.INSTANCE().getZkServers(), 3000, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					LOG.info(event.getState());
				}
			});
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
		}
	}
	
	@Override
	public List<String> liveNodesWatcher() {
		List<String> live_nodes = new ArrayList<String>();
		try {
			live_nodes = zookeeper.getChildren(liveNodesPath, new Watcher() {// 同步注册方法得到列表,为client端初始化indexList提供数据
						@Override
						public void process(WatchedEvent event) {
							if (event.getType() == EventType.NodeChildrenChanged) {
								List<String> childs;
								try {
									childs = zookeeper.getChildren(event.getPath(), this);// 持久监听
									liveNodesQueue.put(childs);
									LOG.info("index server change!!");
								} catch (KeeperException | InterruptedException e) {
									LOG.error(e.getMessage(), e);
								}
							}
						}
					});
		} catch (KeeperException | InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
		return live_nodes;
	}
	
	

	@Override
	public List<String> blockingGetLiveNodes() throws InterruptedException {
		return liveNodesQueue.take();
	}
	
	
	@Override
	public void close() {
		try {
			liveNodesQueue.put(new ArrayList<String>(0));
			zookeeper.close();
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
}
