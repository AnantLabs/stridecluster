package com.lin.stride.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.lin.stride.search.request.NovelHit;
import com.lin.stride.search.request.NovelSearchRequst;
import com.lin.stride.search.request.NovelSearchResponse;
import com.lin.stride.utils.DataInputBuffer;
import com.lin.stride.utils.DataOutputBuffer;

public class AIOSocketChannel {

	private AsynchronousSocketChannel client;
	private ByteBuffer readbuffer = ByteBuffer.allocate(1024);
	private ByteBuffer writebuffer = ByteBuffer.allocate(1024);
	private Logger LOG = Logger.getLogger(AIOSocketChannel.class);
	
	public AIOSocketChannel(String ip, int port) {
		try {
			client = AsynchronousSocketChannel.open();
			if (client.isOpen()) {
				client.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
				client.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
				client.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
				client.setOption(StandardSocketOptions.TCP_NODELAY, true);
				client.connect(new InetSocketAddress(ip, port)).get();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public List<NovelHit> search(NovelSearchRequst request) {
		long st = System.currentTimeMillis();
		List<NovelHit> list = new ArrayList<NovelHit>();
		try {
			DataOutputBuffer dataOutputBuffer = new DataOutputBuffer();
			request.writeData(dataOutputBuffer);
			byte[] bytes = dataOutputBuffer.getData();
			dataOutputBuffer.close();
			
			writebuffer.put(bytes);
			writebuffer.flip();
			client.write(writebuffer).get();
			writebuffer.clear();
			
			client.read(readbuffer).get();
			DataInputBuffer dib = new DataInputBuffer();
			dib.reset(readbuffer.array(), 0,readbuffer.position());
			
			NovelSearchResponse resp = new NovelSearchResponse();
			resp.readDate(dib);
			readbuffer.flip();
			
			NovelHit[] hitArray = resp.getHitArray();
			for(NovelHit hit : hitArray){
				list.add(hit);
			}
			readbuffer.clear();
			dib.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		long et = System.currentTimeMillis();
		LOG.info("time:" + (et-st));
		return list;
	}

	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		AIOSocketChannel af = new AIOSocketChannel("127.0.0.1", 9080);
		NovelSearchRequst qp = new NovelSearchRequst();
		qp.setNovelName("斗");
		
		List<NovelHit> list = af.search(qp);
		for (NovelHit h : list) {
			System.out.println(h.getName());
		}
		af.close();
	}
}
