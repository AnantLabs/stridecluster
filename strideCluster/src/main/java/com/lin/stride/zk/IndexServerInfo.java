package com.lin.stride.zk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.lin.stride.utils.EfficientWritable;

public class IndexServerInfo implements EfficientWritable {

	private int rowNum;
	private long updateTime;

	public IndexServerInfo(int rowNum, long updateTime) {
		this.rowNum = rowNum;
		this.updateTime = updateTime;
	}
	
	public IndexServerInfo(){
		
	}

	@Override
	public void readDate(DataInput in) throws IOException {
		this.rowNum = in.readInt();
		this.updateTime = in.readLong();
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		out.writeInt(rowNum);
		out.writeLong(updateTime);
	}
}
