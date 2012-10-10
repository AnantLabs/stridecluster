package com.lin.stride;

import java.nio.ByteBuffer;

public class SerializerTest {

	public static void main(String[] args) throws Exception{
		ByteBuffer bb = ByteBuffer.allocate(1024);
		String s = "你好";
		bb.put(s.getBytes());
		bb.putInt(100);
		bb.flip();
		byte[] ba = new byte[6];
		bb.get(ba);
		System.out.println(new String(ba));
		System.out.println(bb.getInt());
	}
	
}
