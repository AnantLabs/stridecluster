package com.lin.stride.server;

/**
 * 索引切换回调的接口.在StrideSearchServer的构造函数中实例化一个接口
 * StrideZooKeeperServer的实例中调用这个回调函数的方法.这样不需要在
 * StrideZooKeeperServer中实例化一个StrideSearchServer对象,来操作
 * StrideSearchServer中的方法.
 * @author xiaolin
 *
 */
public interface IndexUpdateListener {
	/**
	 * 切换索引,对下载下来的新的索引重新建立Directory和IndexReader
	 * @return 返回新索引文件的MaxDoc数.这个数值写入Live_nodes节点下,当前目录临时节点的value
	 */
	public boolean switchIndex(long version);

}
