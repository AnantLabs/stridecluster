package com.lin.stride.zk;

import java.util.List;

import org.apache.zookeeper.KeeperException;

public interface StrideZookeeperClient {

	//监控,监控live_nodes
	public List<String> start() throws KeeperException, InterruptedException;
	//关闭
	public void close();

}
