package com.lin.stride.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
/**
 * zk工具类.
 * @Author : xiaolin
 * @Date : 2012-12-20 下午2:18:36
 */
final public class ZKUtils {

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
	public static void initialPersistentPath(ZooKeeper zk, String path, byte[] value) throws IllegalArgumentException, KeeperException, InterruptedException {
		if (validatePath(path)) {
			String[] pathTree = StringUtils.split(path, "/");
			StringBuilder sb = new StringBuilder();
			for (String p : pathTree) {
				sb.append("/").append(p);
				if (zk.exists(sb.toString(), false) == null) {
					zk.create(sb.toString(), value, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
				System.out.println(sb.toString());
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
		Pattern p = Pattern.compile("(/(\\w+(\\w|[\\.@:\\-])*))+(/?)$");	// /开头 \w一个或多个,后面的可以是[xx]中的本内容, 最后必须以/结尾(非必要条件)
		Matcher m = p.matcher(path);
		return m.matches();

	}

	public static void main(String[] args) throws Exception {
		String s = "/xiaolin/_192.178_9000/namenode:9090-1/";
	}

	/*public static void main(String[] args) throws Exception {
		ZooKeeper zk = new ZooKeeper("127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183", 4000, new ZKWatcher());
		InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("stride.properties");
		Properties prop = new Properties();
		try {
			prop.load(in);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String live_n = prop.getProperty("zk_live_nodes");
		String update_lock = prop.getProperty("zk_update_lock");
		String update_status = prop.getProperty("zk_update_status");
		String leader_election = prop.getProperty("zk_leader_election");

		zk.create("/lin", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		zk.create("/lin/stride", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

		if (zk.exists(live_n, false) == null) {
			zk.create(live_n, IntTools.intToByteArray(0), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + live_n);
		}

		if (zk.exists(update_lock, false) == null) {
			zk.create(update_lock, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + update_lock);
		}

		if (zk.exists(update_status, false) == null) {
			zk.create(update_status, "normal".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + update_status);
		}

		if (zk.exists(leader_election, false) == null) {
			zk.create(leader_election, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("create: " + leader_election);
		}
		zk.close();
	}*/

}

class ZKWatcher implements Watcher {
	private final Logger LOG = Logger.getLogger(ZKWatcher.class);

	@Override
	public void process(WatchedEvent event) {
		LOG.info("Watcher : " + event.getType());
	}
}