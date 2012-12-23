package com.lin.stride.index;

public interface IndexBuilder {

	//创建索引，并返回索引的条数和创建的时间。
	public byte[] build() throws Exception;
	
	public byte[] rebuild() throws Exception;
	
	public void close();
	
}
