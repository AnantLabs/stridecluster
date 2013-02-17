package com.lin.stride.client;

import java.util.List;

/**
 * 客户端响应服务器变化的回调接口
 * @Author : xiaolin
 * @Date : 2013-1-4 下午5:07:25
 */
public interface ServerNodeListener {
	
	/**
	 * 服务节点变化的响应方法.
	 * Date : 2013-1-4 下午5:08:01
	 * @param liveNodes
	 */
	public void changeEvent(List<String> liveNodes);
}
