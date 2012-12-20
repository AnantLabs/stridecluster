package com.lin.stride.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.lin.stride.search.request.NovelHit;
import com.lin.stride.search.request.NovelSearchRequst;
import com.lin.stride.zk.SZKClientImpl;
import com.lin.stride.zk.StrideZookeeperClient;

public class StrideClient {

	// index连接池,key为index注册的名称，value为这个index地址的AIO连接
	private ConcurrentHashMap<String, AIOSocketChannel> indexServerPool = new ConcurrentHashMap<String, AIOSocketChannel>();
	private static StrideClient instance;
	private StrideZookeeperClient zooKeeperClient;
	private List<String> indexList;
	private boolean islisten = true;
	private Logger LOG = Logger.getLogger(StrideClient.class);
	private ZKLiveNodesListener zksl = new ZKLiveNodesListener();

	private StrideClient() {
		zooKeeperClient = new SZKClientImpl();
		indexList = zooKeeperClient.liveNodesWatcher();
		for (String index : indexList) {
			String[] addr = index.split(":");
			AIOSocketChannel asc = new AIOSocketChannel(addr[0], Integer.parseInt(addr[1]));
			indexServerPool.put(index, asc);
			LOG.info("***	init index Server : " + index);
		}
		zksl.start();
	}

	public synchronized static StrideClient getInstance() {
		if (instance == null) {
			instance = new StrideClient();
		}
		return instance;
	}

	private void updateIndexList(List<String> childsList) {
		while (islisten) {
			for (String index : indexList) {
				if (!childsList.contains(index)) {
					indexServerPool.get(index).close();
					indexServerPool.remove(index);
					LOG.info("remove down index Server : " + index);
				}
			}
			for (String index : childsList) {
				String[] addr = index.split(":");
				if (!indexServerPool.containsKey(index)) {
					AIOSocketChannel asc = new AIOSocketChannel(addr[0], Integer.parseInt(addr[1]));
					indexServerPool.put(index, asc);
					LOG.info("add new index Server : " + index);
				}
			}
			indexList = childsList;
		}
	}

	public void close() {
		islisten = false;
		zooKeeperClient.close();
		
		for (Entry<String, AIOSocketChannel> entry : indexServerPool.entrySet()) {
			entry.getValue().close();
		}
		indexServerPool = null;
		
		LOG.info("***	StrideClient is shutdown !");
	}

	public List<NovelHit> search(NovelSearchRequst query) {
		long st = System.currentTimeMillis();
		int listSize = indexList.size();
		if (listSize == 0) {
			return new ArrayList<NovelHit>();
		} else {
			int pos = (int) (Math.random() * 100) % listSize;
			LOG.info("select indexServer is : " + indexList.get(pos));
			AIOSocketChannel client = indexServerPool.get(indexList.get(pos));
			List<NovelHit> result = client.search(query);
			long et = System.currentTimeMillis();
			LOG.info("-----"+(et-st));
			return result;
		}
	}

	public static void main(String[] args) {
		StrideClient sc = StrideClient.getInstance();
		NovelSearchRequst qp = new NovelSearchRequst();
		qp.setNovelName("校园");
		List<NovelHit> list = sc.search(qp);
		for (NovelHit q : list) {
			System.out.println(q.getName());
		}
		sc.close();
	}
	
	class ZKLiveNodesListener extends Thread{
		private Logger LOG = Logger.getLogger(ZKLiveNodesListener.class);
		@Override
		public void run() {
			while(islisten){
				try{
					List<String> childsList = zooKeeperClient.blockingGetLiveNodes();
					if(islisten==true && childsList.size()>0){
						updateIndexList(childsList);
					}
				}catch(InterruptedException e){
					e.printStackTrace();
					break;
				} 
			}
			LOG.info("***	ZKStatusListener Thread is terminated !");
		}
	}

}
