package com.lin.stride.utils;

public class ZKIndexVersionTools {
	private static final byte[] intToByteArray(int value) {
		byte[] buffer = new byte[4];
		buffer[0] = (byte) (value);
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value >> 16);
		buffer[3] = (byte) (value >> 24);
		return buffer;
	}

	private static final int bytesArrayToInt(byte[] byteArray) {
		return ((int) (byteArray[3] & 0xFF) << 24) | ((int) (byteArray[2] & 0xFF) << 16) | ((int) (byteArray[1] & 0xFF) << 8)
				| (int) (byteArray[0] & 0xFF);
	}

	private static final byte[] longToByteArray(long value) {
		byte[] buffer = new byte[8];
		buffer[0] = (byte) (value);
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value >> 16);
		buffer[3] = (byte) (value >> 24);
		buffer[4] = (byte) (value >> 32);
		buffer[5] = (byte) (value >> 40);
		buffer[6] = (byte) (value >> 48);
		buffer[7] = (byte) (value >> 56);
		return buffer;
	}

	private static long bytesToLong(byte[] bytes) {
		return ((long) (bytes[7] & 0xFF) << 56) | ((long) (bytes[6] & 0xFF) << 48) | ((long) (bytes[5] & 0xFF) << 40)
				| ((long) (bytes[4] & 0xFF) << 32) | ((long) (bytes[3] & 0xFF) << 24) | ((long) (bytes[2] & 0xFF) << 16)
				| ((long) (bytes[1] & 0xFF) << 8) | (long) (bytes[0] & 0xFF);
	}

	public static byte[] versionToBytes(int indexNum, long createTime) {
		byte[] version = new byte[12];
		byte[] in = intToByteArray(indexNum);
		byte[] ct = longToByteArray(createTime);
		//System.arraycopy(version, arg1, arg2, arg3, arg4);
		//System.arraycopy(version, arg1, arg2, arg3, arg4)
		return version;
	}

}
