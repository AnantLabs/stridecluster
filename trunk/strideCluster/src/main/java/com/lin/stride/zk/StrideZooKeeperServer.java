package com.lin.stride.zk;


public interface StrideZooKeeperServer {

	//注册,根据本地的机器名和端口
		public void registerLiveNode();
		//根据指定的机器名进行注册
		public void registerLiveNode(String hostName);
		
		//索引服务器下线
		public void unavailableService();
		//索引服务器上线
		public void availableService();
		//获得当前更新的状态 "normal" "rebuilding" or "update" 
		public String getCurrentUpdateState();
		
		public void close();
	
}
