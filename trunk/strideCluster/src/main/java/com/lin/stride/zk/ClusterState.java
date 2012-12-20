package com.lin.stride.zk;

public enum ClusterState {

	NORMAL("normal"),
	REBUILDING("rebuilding"),
	UPDATE("update");
	
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
