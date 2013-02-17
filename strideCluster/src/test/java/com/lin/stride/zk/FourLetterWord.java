package com.lin.stride.zk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

public class FourLetterWord {
	private static Logger LOG = Logger.getLogger(FourLetterWord.class);

	public static String send4LetterWord(String host, int port, String cmd) throws IOException {
		LOG.info("connecting to " + host + " " + port);
		Socket sock = new Socket(host, port);
		BufferedReader reader = null;
		try {
			OutputStream outstream = sock.getOutputStream();
			outstream.write(cmd.getBytes());
			outstream.flush();
			sock.shutdownOutput();

			reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			return sb.toString();
		} finally {
			sock.close();
			if (reader != null) {
				reader.close();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		/*List<String> listCmd = ZooKeeperMain.getCommands();
		
		for(String cmd : listCmd){
			System.out.println(cmd);
		}*/
		
		System.out.println(send4LetterWord("127.0.0.1", 2181, "stat"));
		System.out.println("-------------------------------------------");
		//System.out.println(send4LetterWord("120.197.94.240", 2181, "conf"));
	}
	
	
	
}
