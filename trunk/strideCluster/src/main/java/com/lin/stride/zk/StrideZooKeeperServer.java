package com.lin.stride.zk;

public interface StrideZooKeeperServer {

	//注册,根据本地的机器名和端口,注册时,需要提供当前节点的一些状态.
	public void registerLiveNode();

	//索引服务器下线
	public void unavailableService();

	//获得当前更新的状态 "normal" "rebuilding" or "update" 
	public String getCurrentUpdateState();

	public boolean isLeader();
	
	public long getLatestDir();

	public void close();

}
