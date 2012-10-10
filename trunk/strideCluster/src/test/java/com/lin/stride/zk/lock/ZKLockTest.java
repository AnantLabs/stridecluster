package com.lin.stride.zk.lock;

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

public class ZKLockTest {

	Logger LOG = Logger.getLogger(ZKLockTest.class);
	
	public void lock() throws Exception {
		ZooKeeper zookeeper = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 3000, new ZKWatcher());
		WriteLock wlock = new WriteLock(zookeeper, "/lin/stride/update_lock", Ids.OPEN_ACL_UNSAFE);
		//x-160369449046048770-0000000000
		final CountDownLatch latch = new CountDownLatch(1);
		wlock.setLockListener(new LockListener() {
			@Override
			public void lockReleased() {
				LOG.info("lockReleased");
			}
			@Override
			public void lockAcquired() {
				LOG.info("lockAcquired");
				try {
					for(int i=0;i<10;i++){
						LOG.info("+ "+i);
						Thread.sleep(2000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				latch.countDown();
			}
		});
		boolean ok = wlock.lock();
		latch.await();
		LOG.info("wait end");
		wlock.unlock();
		wlock.close();
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