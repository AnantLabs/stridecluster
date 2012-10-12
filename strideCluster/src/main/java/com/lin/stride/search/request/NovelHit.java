package com.lin.stride.search.request;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.lin.stride.utils.EfficientWritable;

public class NovelHit implements EfficientWritable {

	private String name;

	public NovelHit() {
	}

	@Override
	public void readDate(DataInput in) throws IOException {
		name = in.readUTF();
	}

	@Override
	public void writeData(DataOutput out) throws IOException {
		out.writeUTF(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}