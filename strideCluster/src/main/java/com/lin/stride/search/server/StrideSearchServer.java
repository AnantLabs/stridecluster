package com.lin.stride.search.server;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.lin.stride.hdfs.HDFSFileSystem;
import com.lin.stride.search.LinIndexSearcher;
import com.lin.stride.search.request.NovelHit;
import com.lin.stride.search.request.NovelSearchRequst;
import com.lin.stride.search.request.NovelSearchResponse;
import com.lin.stride.utils.ConfigReader;
import com.lin.stride.utils.DataInputBuffer;
import com.lin.stride.utils.DataOutputBuffer;
import com.lin.stride.zk.SZKServerImpl;
import com.lin.stride.zk.StrideZooKeeperServer;

public class StrideSearchServer {

	private Logger LOG = Logger.getLogger(StrideSearchServer.class);

	private AsynchronousServerSocketChannel server;
	private ExecutorService taskExecutor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());
	private LinIndexSearcher searcher;
	private StrideZooKeeperServer zookeeperClient;
	private ServerSocket shutdownListener;
	private boolean isShutdown = false; // 服务器是否已经关闭

	public StrideSearchServer() {
		
		zookeeperClient = new SZKServerImpl(new SwitchIndexCallBack() {
			@Override
			public void clearIndexFile() throws IOException {
				searcher.clearIndexFile();	
			}
			
			@Override
			public int switchIndex() {
				return searcher.switchIndex();
			}
		});
		
		File indexStrorageDir = new File(ConfigReader.getEntry("indexStorageDir"));
		
		/*这个if判断索引目录是否为空,主要解决一个indexServer启动时,这个server下没有索引文件,导致启动失败.如果没有索引从
		 * HDFS上下载后再启动,从HDFS上下载需要判断这时节点的状态,如果状态时rebuild,那么这时复制更新的server正在重建
		 * 可能马上回替换掉正在使用的HDFS上的文件,导致下载失败.所以如果状态是rebuild,那么就等待,直到状态更新为normal或者是update
		 */
		if(indexStrorageDir.list().length<3){
			LOG.warn(indexStrorageDir.getAbsoluteFile() + " - indexFile exception , wating downLoad from HDFS !");
			while(zookeeperClient.getCurrentUpdateState().equalsIgnoreCase("rebuild")){
				try {
					Thread.sleep(1000*5);
					LOG.info("...");
				} catch (InterruptedException e) {
					LOG.error(e.getMessage(),e);
				}
			}
			HDFSFileSystem h = new HDFSFileSystem();
			try {
				h.downLoadIndex();
			} catch (IOException e) {
				LOG.error(e.getMessage(),e);
			}
			h.close();
		}
		
		searcher = new LinIndexSearcher();
		
		try {
			server = AsynchronousServerSocketChannel.open();
			server.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
			server.bind(new InetSocketAddress(Integer.parseInt(ConfigReader.getEntry("serverport", "9080"))));
			//server.setOption(SocketOption<T>., value)
			shutdownListener = new ServerSocket(Integer.parseInt(ConfigReader.getEntry("shutdownport", "8001")));
			shutdownThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		LOG.info("Server is started !");
	}

	private Thread shutdownThread = new Thread() { // 负责关闭服务器的线程
		public void start() {
			this.setDaemon(true); // 设置为守护线程（也称为后台线程）
			super.start();
		}

		public void run() {
			while (!isShutdown) {
				Socket socketShutdown = null;
				try {
					socketShutdown = shutdownListener.accept();
					DataInputStream o = new DataInputStream(socketShutdown.getInputStream());
					String command = o.readUTF();
					if (command.equals("shutdown")) {
						long beginTime = System.currentTimeMillis();
						isShutdown = true;

						close();
						
						long endTime = System.currentTimeMillis();
						LOG.info("server stopped in " + (endTime - beginTime) + " ms\r\n");
						socketShutdown.close();
						shutdownListener.close();
					} else {
						LOG.info("ooo");
						socketShutdown.getOutputStream().write("command error \r\n".getBytes());
						socketShutdown.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	public void start() {
		while (true) {
			Future<AsynchronousSocketChannel> asynchronousSocketChannelFuture = server.accept();
			try {
				final AsynchronousSocketChannel asynchronousSocketChannel = asynchronousSocketChannelFuture.get();
				Callable<String> worker = new Callable<String>() {
					@Override
					public String call() throws Exception {
						String host = asynchronousSocketChannel.getRemoteAddress().toString();
						LOG.info("Incoming connection from: " + host);
						final ByteBuffer readbuffer = ByteBuffer.allocate(512);
						final ByteBuffer writebuffer = ByteBuffer.allocate(1024);
						while (asynchronousSocketChannel.read(readbuffer).get() != -1) {

							DataInputBuffer dib = new DataInputBuffer();
							dib.reset(readbuffer.array(), 0,readbuffer.position());
							NovelSearchRequst nsr = new NovelSearchRequst();
							nsr.readDate(dib);
							readbuffer.flip();

							NovelHit[] hits = searcher.search(nsr);
							NovelSearchResponse nsp = new NovelSearchResponse(hits, (short)hits.length);
							DataOutputBuffer dataOutputBuffer = new DataOutputBuffer();
							nsp.writeData(dataOutputBuffer);
							byte[] bytes = dataOutputBuffer.getData();
							dataOutputBuffer.close();
							writebuffer.clear();
							writebuffer.put(bytes);
							writebuffer.flip();
							asynchronousSocketChannel.write(writebuffer).get();
							
							readbuffer.clear();
						}
						asynchronousSocketChannel.close();
						LOG.info(host + " was successfully served!");
						return host;
					}
				};
				taskExecutor.submit(worker);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("\n Server is shutting down ...");
				taskExecutor.shutdown();
				while (!taskExecutor.isTerminated()) {
				}
				break;
			}
		}
	}

	public void close() {
		try {
			taskExecutor.shutdown();
			while (!taskExecutor.isTerminated()) {
			}
			zookeeperClient.close();
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.info("Server is shutdown !");
	}

	public static void main(String[] args) {
		StrideSearchServer as = new StrideSearchServer();
		as.start();

	}

}
