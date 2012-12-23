package com.lin.stride.zk;

public enum ClusterState {

	NORMAL("normal"), //正常状态
	REBUILD("rebuild"), //外部通知状态
	UPDATE("update");	//leader计算完索引，通知follower下载索引
	
	private String state;
	private ClusterState(String state) {
		this.state = state;
	}
	
	public byte[] getBytes(){
		return this.state.getBytes();
	}
	
	@Override
	public String toString() {
		return state.toLowerCase();
	}
	
}
