package com.lin.stride.zk;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class CallBackTest {

	/**
	 * Date : 2013-1-10 下午2:35:41
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 1000, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				System.out.println("Watcher : " + event.getType());
			}
		});
		
		zk.exists("/lin/stride/live_nodes", null, new StatCallback() {
			@Override
			public void processResult(int rc, String path, Object ctx, Stat stat) {
				System.out.println(rc + "\t" + path + "\t" + ctx.toString() + "\t" + stat.toString());
			}
		}, "xiaolin");
		Thread.sleep(1000);
		zk.close();
		
		
	}

}
