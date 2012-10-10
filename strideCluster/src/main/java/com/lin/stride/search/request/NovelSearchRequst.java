package com.lin.stride.search.request;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.lin.stride.utils.EfficientWritable;

public class NovelSearchRequst implements EfficientWritable{
	
	private String novelName;
	private String novelAuthor;
	private String tag;
	private int startInTime;
	private int endInTime;
	private int minChapterCount;
	private int maxChapterCount;

	public String getNovelName() {
		return novelName;
	}

	public void setNovelName(String novelName) {
		this.novelName = novelName;
	}

	public String getNovelAuthor() {
		return novelAuthor;
	}

	public void setNovelAuthor(String novelAuthor) {
		this.novelAuthor = novelAuthor;
	}


	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public int getStartInTime() {
		return startInTime;
	}

	public void setStartInTime(int startInTime) {
		this.startInTime = startInTime;
	}

	public int getEndInTime() {
		return endInTime;
	}

	public void setEndInTime(int endInTime) {
		this.endInTime = endInTime;
	}

	public int getMinChapterCount() {
		return minChapterCount;
	}

	public void setMinChapterCount(int minChapterCount) {
		this.minChapterCount = minChapterCount;
	}

	public int getMaxChapterCount() {
		return maxChapterCount;
	}

	public void setMaxChapterCount(int maxChapterCount) {
		this.maxChapterCount = maxChapterCount;
	}

	@Override
	public void readDate(DataInput in) throws IOException {
		if(in.readBoolean() == true){
			novelName = in.readUTF();
		}
		if(in.readBoolean() == true){
			novelAuthor = in.readUTF();
		}
		if(in.readBoolean() == true){
			tag = in.readUTF();
		}
		startInTime = in.readInt();
		endInTime = in.readInt();
		minChapterCount = in.readInt();
		maxChapterCount = in.readInt();
	}
	
	@Override
	public void writeData(DataOutput out) throws IOException {
		if(novelName == null){
			out.writeBoolean(false);
		}else{
			 out.writeBoolean(true);
			 out.writeUTF(novelName);
		}
		if(novelAuthor == null){
			out.writeBoolean(false);
		}else{
			out.writeBoolean(true);
			out.writeUTF(novelAuthor);
		}
		if(tag == null){
			out.writeBoolean(false);
		}else{
			out.writeBoolean(true);
			out.writeUTF(tag);
		}
		out.writeInt(startInTime);
		out.writeInt(endInTime);
		out.writeInt(minChapterCount);
		out.writeInt(maxChapterCount);
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("name: ").append(novelName).append(" author: ").append(novelAuthor).append(" tag: ").append(tag);
		sb.append(" startInTime: ").append(startInTime).append(" endInTime: ").append(endInTime);
		sb.append(" minChapterCount: ").append(minChapterCount).append(" maxChapterCount: ").append(maxChapterCount);
		return sb.toString();
	}
	
}
