package com.lin.stride.zk;

import java.util.List;

public interface StrideZookeeperClient {

	//监控,监控live_nodes
	public List<String> liveNodesWatcher();
	//blocking模式的监听
	public List<String> blockingGetLiveNodes() throws InterruptedException;
	
	//关闭
	public void close();

}
