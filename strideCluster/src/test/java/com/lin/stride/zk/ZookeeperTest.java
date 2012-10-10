package com.lin.stride.zk;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperTest {
	private Logger LOG = Logger.getLogger(ZookeeperTest.class);
	private ZooKeeper zk;
	private String rootName = "/lin/stride/leader_election";

	public ZookeeperTest() throws Exception {
		zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 1000, new ZKWatcher());
	}

	public void create(String pathName) throws Exception {
		String path = rootName;// + "/" + pathName;
		String tmp = zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		LOG.info("create:" + tmp);
	}

	public void list() throws Exception {
		String path =rootName;
		zk.getChildren(path, new ZKWatcher());
	}

	public void delete(String pathName) throws Exception {
		String path = "/" + rootName + "/" + pathName;
		zk.delete(path, -1);
	}

	public void close() {
		try {
			zk.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		ZookeeperTest zkt = new ZookeeperTest();
		ZookeeperTest zkt1 = new ZookeeperTest();
		ZookeeperTest zkt2 = new ZookeeperTest();
		ZookeeperTest zkt3 = new ZookeeperTest();
		zkt.list();

		zkt1.create("xiaolin");
		zkt2.create("xianglin");
		zkt3.create("linlin");
		Thread.sleep(4000);

		zkt1.close();
		zkt2.close();
		zkt3.close();

		Thread.sleep(3000);

		zkt.close();
	}

	class ZKWatcher implements Watcher {
		private Logger LOG = Logger.getLogger(ZKWatcher.class);

		@Override
		public void process(WatchedEvent event) {
			LOG.info("Watcher : " + event.getType());
			if (event.getType() == EventType.NodeChildrenChanged) {
				try {
					List<String> child = zk.getChildren(event.getPath(), this);
					for (String str : child) {
						LOG.info("*** " + str);
					}
				} catch (KeeperException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
