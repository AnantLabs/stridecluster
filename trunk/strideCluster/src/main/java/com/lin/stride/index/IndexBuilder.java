package com.lin.stride.index;

public interface IndexBuilder {

	public void build() throws Exception;
	
	public void rebuild() throws Exception;
	
	public void close();
	
}
