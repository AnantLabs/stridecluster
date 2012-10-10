package com.lin.stride.search.request;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


import com.lin.stride.utils.EfficientWritable;

public class NovelSearchResponse implements EfficientWritable{
	
	private short length;
	private NovelHit[] hitArray ;
	
	public NovelSearchResponse(NovelHit[] hitArray ,short length) {
		this.hitArray = hitArray;
		this.length = (short)hitArray.length;
	}
	
	public NovelSearchResponse() {
	}
	
	@Override
	public void readDate(DataInput in) throws IOException {
		this.length = in.readShort();
		NovelHit[] hitArray = new NovelHit[length];
		for(short i=0;i<length;i++){
			NovelHit h = new NovelHit();
			h.readDate(in);
			hitArray[i] = h;
		}
		this.hitArray = hitArray;
	}
	
	@Override
	public void writeData(DataOutput out) throws IOException {
		out.writeShort(length);
		for(NovelHit h : hitArray){
			h.writeData(out);
		}
	}

	public short getLength() {
		return length;
	}

	public void setLength(short length) {
		this.length = length;
	}

	public NovelHit[] getHitArray() {
		return hitArray;
	}

	public void setHitArray(NovelHit[] hitArray) {
		this.hitArray = hitArray;
	}
	
}
