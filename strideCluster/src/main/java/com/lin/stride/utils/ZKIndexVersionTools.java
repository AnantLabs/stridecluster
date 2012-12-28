package com.lin.stride.utils;

/**
 * 索引版本转换工具类, 主要用于zk的live_nodes节点的data数据
 * index的版本使用  索引数据量(int)+ 更新时间(long) 来确定一个索引是新是旧
 * 只用更新时间可以标识哪个更新,但是为了后备,把更新的数量也添加进来.
 * @Author : xiaolin
 * @Date : 2012-12-24 下午1:51:28
 */
public class ZKIndexVersionTools {
	private ZKIndexVersionTools() {
	}
	/**
	 * int 装换为 byte[]
	 * Date : 2012-12-24 下午1:34:38
	 * @param value 等待转换的int值
	 * @return 返回的byte[]
	 */
	private static final byte[] intToByteArray(int value) {
		byte[] buffer = new byte[4];
		buffer[0] = (byte) (value);
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value >> 16);
		buffer[3] = (byte) (value >> 24);
		return buffer;
	}

	/**
	 * byte[] 转换为int.
	 * Date : 2012-12-24 下午1:35:48
	 * @param bytes
	 * @return
	 */
	private static final int bytesArrayToInt(byte[] bytes) {
		if(bytes.length<4){
			throw new IllegalArgumentException("argument byte[] length must be 4");
		}
		return ((bytes[3] & 0xFF) << 24) | ((bytes[2] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8)
				| bytes[0] & 0xFF;
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
		if(bytes.length<8){
			throw new IllegalArgumentException("argument byte[] length must be 8");
		}
		return ((long) (bytes[7] & 0xFF) << 56) | ((long) (bytes[6] & 0xFF) << 48) | ((long) (bytes[5] & 0xFF) << 40)
				| ((long) (bytes[4] & 0xFF) << 32) | ((long) (bytes[3] & 0xFF) << 24) | ((long) (bytes[2] & 0xFF) << 16)
				| ((long) (bytes[1] & 0xFF) << 8) | bytes[0] & 0xFF;
	}

	
	public static byte[] versionToBytes(int indexNum, long createTime) {
		byte[] version = new byte[12];
		byte[] in = intToByteArray(indexNum);
		byte[] ct = longToByteArray(createTime);
		System.arraycopy(in, 0, version, 0, 4);
		System.arraycopy(ct, 0, version, 4, 8);
		return version;
	}
	
	public static int bytesToIndexNum(byte[] bytes){
		if(bytes.length<12){
			throw new IllegalArgumentException("argument byte[] length must be 12");
		}
		return bytesArrayToInt(bytes);
	}
	
	public static boolean zeroVersion(byte[] bytes){
		return bytesArrayToInt(bytes) ==0 && bytesToCtime(bytes)==0L;
	}
	
	public static long bytesToCtime(byte[] bytes){
		if(bytes.length<12){
			throw new IllegalArgumentException("argument byte[] length must be 12");
		}
		byte[] time = new byte[8];
		System.arraycopy(bytes, 4, time, 0, 8);
		return bytesToLong(time);
	}
	
	public static void main(String[] args) {
		
		byte[] v = versionToBytes(128,276L);
		
		System.out.println(bytesToIndexNum(v));
		System.out.println(bytesToCtime(v));
	}

}
