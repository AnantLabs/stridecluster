package com.lin.stride.client;

import java.util.List;

public interface ServerNodeListener {
	public void changeEvent(List<String> liveNodes);
}
