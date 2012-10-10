package com.lin.stride.zk;

import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class ZKServerDown {

	@Test
	public void sessionLost() throws Exception{
		int flag = 0;
		
		ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 1000, new ZKWatcher());
		
		while(flag<60){
			long id = zk.getSessionId();
			System.out.println(id);
			Thread.sleep(1000);
		}
		
		zk.close();
	}
	
	class ZKWatcher implements Watcher {
		private Logger LOG = Logger.getLogger(ZKWatcher.class);

		@Override
		public void process(WatchedEvent event) {
			LOG.info("Watcher : " + event.getType());
		}
	}
	
}
