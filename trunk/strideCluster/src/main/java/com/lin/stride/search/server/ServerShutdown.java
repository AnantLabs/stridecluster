package com.lin.stride.search.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.lin.stride.utils.ConfigReader;

public class ServerShutdown {
	public void shutdown() {
		Socket socket = null;
		try {
			socket = new Socket("localhost", Integer.parseInt(ConfigReader.getEntry("shutdownport")));
			OutputStream socketOut = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(socketOut);
			dos.writeUTF("shutdown");// 发送关闭命令
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (socket != null){
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String args[]) {
		ServerShutdown sh = new ServerShutdown();
		sh.shutdown();
	}
}
