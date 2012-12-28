package com.lin.stride.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import com.lin.stride.zk.ClusterState;

/**
 * zk工具类.
 * @Author : xiaolin
 * @Date : 2012-12-20 下午2:18:36
 */
final public class ZKUtils {

	private ZKUtils() {
	}

	private static final Logger LOG = Logger.getLogger(ZKUtils.class);

	public static void initialPersistentPath(ZooKeeper zk) throws IllegalArgumentException, KeeperException, InterruptedException {
		initialPersistentPath(zk, ConfigReader.INSTANCE().getZkLiveNodePath(), ZKIndexVersionTools.versionToBytes(0, 0));
		initialPersistentPath(zk, ConfigReader.INSTANCE().getZkUpdateLockPath(), null);
		initialPersistentPath(zk, ConfigReader.INSTANCE().getZkUpdateStatusPath(), ClusterState.NORMAL.getBytes());
		initialPersistentPath(zk, ConfigReader.INSTANCE().getZkLeaderElectionPath(), null);
	}

	/**
	 * 初始化zk一些固定路径.
	 * Date : 2012-12-20 下午2:21:27
	 * @param zk	zookeeper实例
	 * @param path	要创建的路径
	 * @param value	初始值
	 * @throws IllegalArgumentException	路径非法异常
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public static void initialPersistentPath(ZooKeeper zk, String path, byte[] value) throws IllegalArgumentException, KeeperException,
			InterruptedException {
		byte[] nodeValue = null;
		if (validatePath(path)) {
			String[] pathTree = StringUtils.split(path, "/");
			StringBuilder sb = new StringBuilder();
			for (int cursor = 0; cursor < pathTree.length; cursor++) {
				sb.append("/").append(pathTree[cursor]);
				if (zk.exists(sb.toString(), false) == null) {
					if (cursor == pathTree.length - 1) {
						nodeValue = value;
					}
					zk.create(sb.toString(), nodeValue, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					LOG.info("ZooKeeper create persistent node :" + sb.toString() + "\t" + value);
				}
			}

		} else { //如果路径非法,那么抛出异常.
			throw new IllegalArgumentException("invalid path String " + path);
		}
	}

	/**
	 * 验证路径的正则表达式.
	 * Date : 2012-12-20 下午2:23:28
	 * @param path	需要验证的路径
	 * @return	返回是否合法
	 */
	private static boolean validatePath(String path) {
		//Pattern p = Pattern.compile("(/((\\d|\\w|_)+(\\d|\\w|[\\.@-_:])*))+");
		Pattern p = Pattern.compile("(/(\\w+(\\w|[\\.@:\\-])*))+(/?)$"); // /开头 \w一个或多个,后面的可以是[xx]中的本内容, 最后必须以/结尾(非必要条件)
		Matcher m = p.matcher(path);
		return m.matches();

	}

	public static byte[] getData(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		return zk.getData(path, false, null);
	}

	public static void main(String[] args) throws Exception {
		ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 3000, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				// zk只有失效或者链接不可用时才报警
				if (event.getState() == KeeperState.Expired || event.getState() == KeeperState.Disconnected) {
					LOG.warn("session Expired");
				}
			}
		});
		
		//ZKUtils.initialPersistentPath(zk);
		byte[] v = ZKUtils.getData(zk, ConfigReader.INSTANCE().getZkLiveNodePath());
		System.out.println(ZKIndexVersionTools.bytesToCtime(v));
		System.out.println(ZKIndexVersionTools.bytesToIndexNum(v));
		
		
		v = ZKUtils.getData(zk, ConfigReader.INSTANCE().getZkUpdateStatusPath());
		System.out.println(new String(v));
		zk.close();
	}

}