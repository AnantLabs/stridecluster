package com.lin.stride.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigReader {

	private final Logger LOG = Logger.getLogger(ConfigReader.class);
	private final Properties prop = new Properties();
	private static ConfigReader instance = new ConfigReader();

	private int serverPort;
	private int shutdownPort;
	private String indexStorageDir;
	private String zkServers;
	private String zkLiveNodePath;
	private String zkUpdateLockPath;
	private String zkUpdateStatusPath;
	private String zkLeaderElectionPath;
	private String hadoopNameNodeAddress;
	private String hadoopIndexPath;

	private ConfigReader() {
		InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("stride.properties");
		try {
			prop.load(in);
			in.close();
			serverPort = Integer.parseInt(prop.getProperty("serverport"));
			shutdownPort =Integer.parseInt(prop.getProperty("shutdownport"));
			indexStorageDir=prop.getProperty("indexStorageDir");
			zkServers=prop.getProperty("zk_servers");
			zkLiveNodePath=prop.getProperty("zk_live_nodes");
			zkUpdateLockPath=prop.getProperty("zk_update_lock");
			zkUpdateStatusPath=prop.getProperty("zk_update_status");
			zkLeaderElectionPath=prop.getProperty("zk_leader_election");
			hadoopNameNodeAddress=prop.getProperty("hadoop_namenode_address");
			hadoopIndexPath=prop.getProperty("hadoop_index_path");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static ConfigReader INSTANCE() {
		return instance;
	}

	public int getServerPort() {
		return serverPort;
	}

	public int getShutdownPort() {
		return shutdownPort;
	}

	public String getIndexStorageDir() {
		return indexStorageDir;
	}

	public String getZkServers() {
		return zkServers;
	}

	public String getZkLiveNodePath() {
		return zkLiveNodePath;
	}

	public String getZkUpdateLockPath() {
		return zkUpdateLockPath;
	}

	public String getZkUpdateStatusPath() {
		return zkUpdateStatusPath;
	}

	public String getZkLeaderElectionPath() {
		return zkLeaderElectionPath;
	}

	public String getHadoopNameNodeAddress() {
		return hadoopNameNodeAddress;
	}

	public String getHadoopIndexPath() {
		return hadoopIndexPath;
	}

}
