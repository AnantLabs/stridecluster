package com.lin.stride.zk;


public class StrideZKClient{// implements StrideZK {

/*	private ZkClient zkClient = new ZkClient(ConfigReader.getEntry("zk_servers"), 5000, 5000);
	private String liveNodesPath = ConfigReader.getEntry("zk_live_nodes");
	private String updateLockPath = ConfigReader.getEntry("zk_update_lock");
	private String updateStatusPath = ConfigReader.getEntry("zk_update_status");
	private Logger LOG = Logger.getLogger(StrideZKClient.class);
	private BlockingQueue<List<String>> liveNodesQueue = new ArrayBlockingQueue<List<String>>(10);
	private BlockingQueue<List<String>> updateLockQueue = new ArrayBlockingQueue<List<String>>(10);
	private InetAddress inetAddress;

	public StrideZKClient() {
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerLiveNode() {
		createParent(liveNodesPath);
		StringBuilder sb = new StringBuilder();
		sb.append(liveNodesPath).append("/").append(inetAddress.getHostName()).append(":").append(ConfigReader.getEntry("serverport"));
		zkClient.createEphemeral(sb.toString());
		LOG.info("ZK createEphemeral : " + sb.toString());
	}

	@Override
	public void registerLiveNode(String hostName) {
		createParent(liveNodesPath);
		StringBuilder sb = new StringBuilder();
		sb.append(liveNodesPath).append("/").append(hostName).append(":").append(ConfigReader.getEntry("serverport"));
		zkClient.createEphemeral(sb.toString());
		LOG.info("ZK createEphemeral : " + sb.toString());
	}

	private void createParent(String parentName) {
		String[] dirLevel = parentName.split("/");
		parentName = "";
		for (int i = 1; i < dirLevel.length; i++) {
			parentName += "/" + dirLevel[i];
			if (!zkClient.exists(parentName)) {
				zkClient.createPersistent(parentName);
				LOG.info("ZK createPersistent : " + parentName);
			}
		}
	}

	@Override
	public List<String> liveNodesWatcher() {
		zkClient.subscribeChildChanges(liveNodesPath, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				liveNodesQueue.put(currentChilds);
				LOG.info("index server change!!");
			}
		});
	}

	@Override
	public void indexServerStateWatcher() {
		zkClient.subscribeDataChanges(updateStatusPath, new IZkDataListener() {

			@Override
			public void handleDataDeleted(String path) throws Exception {

			}

			@Override
			public void handleDataChange(String path, Object value) throws Exception {
				if (value.toString().equals("update")) {
					Thread.sleep(5000);
					zkClient.createEphemeralSequential(updateLockPath, new byte[0]);
				}
			}
		});
	}

	@Override
	public void updateIndexLockWatcher() {
		zkClient.subscribeChildChanges(updateLockPath, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				updateLockQueue.put(currentChilds);
				LOG.info("lock change!!");
			}
		});
	}

	@Override
	public List<String> getChilds() {
		return zkClient.getChildren(liveNodesPath);
	}

	@Override
	public List<String> watingGetLiveNodes() throws InterruptedException {
		return liveNodesQueue.take();
	}

	@Override
	public List<String> watingGetUpdateLock() throws InterruptedException {
		return updateLockQueue.take();
	}

	@Override
	public void availableService() {
		registerLiveNode();
	}

	@Override
	public void unavailableService() {
		StringBuilder sb = new StringBuilder();
		sb.append(liveNodesPath).append("/").append(inetAddress.getHostName()).append(":").append(ConfigReader.getEntry("serverport"));
		zkClient.delete(sb.toString());
	}

	@Override
	public void close() {
		zkClient.close();
	}

	public void delete(String path) {
		zkClient.delete(path);
	}

	public static void main(String[] args) {
		StrideZKClient szk = new StrideZKClient();
		szk.delete("/lin/stride/shop_live_nodes");
	}*/

}
