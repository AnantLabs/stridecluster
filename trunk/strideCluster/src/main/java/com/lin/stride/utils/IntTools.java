package com.lin.stride.utils;

public class IntTools {
	public static final byte[] intToByteArray(int value) {
		byte[] buffer = new byte[4];
		buffer[0] = (byte) (value);
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value >> 16);
		buffer[3] = (byte) (value >> 24);
		return buffer;
	}

	public static final int byteArrayToInt(byte[] byteArray) {
		return ((int) (byteArray[3] & 0xFF) << 24) | ((int) (byteArray[2] & 0xFF) << 16) | ((int) (byteArray[1] & 0xFF) << 8)
				| (int) (byteArray[0] & 0xFF);
	}

}
