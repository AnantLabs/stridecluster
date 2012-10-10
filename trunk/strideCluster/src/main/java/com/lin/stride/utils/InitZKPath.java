package com.lin.stride.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class InitZKPath {

	public static void main(String[] args) throws Exception{
		ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 4000, new ZKWatcher());
		InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("stride.properties");
		Properties prop = new Properties();
		try {
			prop.load(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


		String live_n = prop.getProperty("zk_live_nodes");
		String update_lock = prop.getProperty("zk_update_lock");
		String update_status = prop.getProperty("zk_update_status");
		String leader_election = prop.getProperty("zk_leader_election");
		
		zk.create("/lin", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		zk.create("/lin/stride", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		
		if(zk.exists(live_n, false)==null){
			zk.create(live_n, IntTools.intToByteArray(0), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + live_n);
		}
		
		if(zk.exists(update_lock, false)==null){
			zk.create(update_lock, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + update_lock);
		}
		
		if(zk.exists(update_status, false)==null){
			zk.create(update_status, "normal".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + update_status);
		}
		
		if(zk.exists(leader_election, false)==null){
			zk.create(leader_election, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + leader_election);
		}
		zk.close();
	}
	
}


class ZKWatcher implements Watcher {
	private Logger LOG = Logger.getLogger(ZKWatcher.class);

	@Override
	public void process(WatchedEvent event) {
		LOG.info("Watcher : " + event.getType());
	}
}