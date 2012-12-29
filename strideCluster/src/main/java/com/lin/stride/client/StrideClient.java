package com.lin.stride.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import com.lin.stride.search.request.NovelHit;
import com.lin.stride.search.request.NovelSearchRequst;
import com.lin.stride.zk.SZKClientImpl;
import com.lin.stride.zk.StrideZookeeperClient;

public class StrideClient {

	// index连接池,key为index注册的名称，value为这个index地址的AIO连接
	private final CopyOnWriteArrayList<AIOSocketChannel> indexServerPool = new CopyOnWriteArrayList<AIOSocketChannel>();
	private static StrideClient instance;
	private final StrideZookeeperClient zooKeeperClient;
	private final Logger LOG = Logger.getLogger(StrideClient.class);

	private StrideClient() throws IOException, KeeperException, InterruptedException, NumberFormatException, ExecutionException {
		zooKeeperClient = new SZKClientImpl(new ServerNodeListener() {

			@Override
			public void changeEvent(List<String> liveNodes) {
				try {
					updateIndexList(liveNodes);
				} catch (NumberFormatException | IOException | InterruptedException | ExecutionException e) {
					LOG.error(e.getMessage(), e);
				}
			}

		});
		List<String> indexList = zooKeeperClient.start();
		for (String server : indexList) {
			String[] addr = server.split(":");
			AIOSocketChannel asc = new AIOSocketChannel(addr[0], Integer.parseInt(addr[1]));
			indexServerPool.add(asc);
			LOG.info("***	add index Server : " + server);
		}
	}

	public synchronized static StrideClient getInstance() throws IOException, KeeperException, InterruptedException, NumberFormatException,
			ExecutionException {
		if (instance == null) {
			instance = new StrideClient();
		}
		return instance;
	}

	private void updateIndexList(List<String> liveNodes) throws NumberFormatException, IOException, InterruptedException, ExecutionException {
		int poolSize = indexServerPool.size();
		AIOSocketChannel socketChannel;
		for (int i = 0; i < poolSize; i++) {
			socketChannel = indexServerPool.get(i);
			if (!liveNodes.contains(socketChannel.getAddress())) {
				LOG.info("remove down index Server : " + socketChannel.getAddress());
				indexServerPool.remove(i);
				socketChannel.close();
				socketChannel = null;
			} else {
				liveNodes.remove(i);
			}
		}
		for (String index : liveNodes) {
			String[] addr = index.split(":");
			AIOSocketChannel asc = new AIOSocketChannel(addr[0], Integer.parseInt(addr[1]));
			indexServerPool.add(asc);
			LOG.info("add new index Server : " + index);
		}
	}

	public void close() {
		zooKeeperClient.close();
		for (AIOSocketChannel entry : indexServerPool) {
			entry.close();
		}
		LOG.info("***	StrideClient is shutdown !");
	}

	public List<NovelHit> search(NovelSearchRequst query) {
		long st = System.currentTimeMillis();
		int listSize = indexServerPool.size();
		if (listSize == 0) {
			return new ArrayList<NovelHit>();
		} else {
			int pos = (int) (Math.random() * 100) % listSize;
			AIOSocketChannel client = indexServerPool.get(pos);
			LOG.info("select indexServer is : " + client.getAddress());
			List<NovelHit> result = client.search(query);
			long et = System.currentTimeMillis();
			LOG.info("-----" + (et - st));
			return result;
		}
	}

	public static void main(String[] args) throws Exception {
		StrideClient sc = StrideClient.getInstance();
		NovelSearchRequst qp = new NovelSearchRequst();
		qp.setNovelName("校园");
		List<NovelHit> list = sc.search(qp);
		for (NovelHit q : list) {
			System.out.println(q.getName());
		}
		sc.close();
	}
}
