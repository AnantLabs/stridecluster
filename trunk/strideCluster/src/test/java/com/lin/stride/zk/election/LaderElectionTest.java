package com.lin.stride.zk.election;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.junit.Test;

import com.lin.stride.zk.election.LeaderElectionSupport.EventType;

public class LaderElectionTest {

	Logger LOG = Logger.getLogger(LaderElectionTest.class);

	public void test() throws Exception{
		ZooKeeper zookeeper = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 3000, new ZKWatcher());
		//String tmp = zookeeper.create("/lin/stride/leader_election", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		final LeaderElectionSupport les = new LeaderElectionSupport();
		les.setHostName("xiaolin2");
		les.setZooKeeper(zookeeper);
		les.setRootNodeName("/lin/stride/leader_election");
		les.addListener(new LeaderElectionAware() {
			@Override
			public void onElectionEvent(EventType eventType) {
				/*if(eventType==EventType.ELECTED_COMPLETE){
					try {
						System.out.println(eventType+"\t"+les.getLeaderHostName());
					} catch (KeeperException e) {
						LOG.error(e.getMessage(),e);
					} catch (InterruptedException e) {
						LOG.error(e.getMessage(),e);
					}
				}*/
				System.out.println(eventType);
			}
		});
		System.out.println("*********************");
		les.start();
		Thread.sleep(20000);
		les.stop();
		zookeeper.close();
	}

}
class ZKWatcher implements Watcher {
	private Logger LOG = Logger.getLogger(ZKWatcher.class);

	@Override
	public void process(WatchedEvent event) {
		LOG.info("Watcher : " + event.getType());
	}
}