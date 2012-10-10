package com.lin.stride.utils;

public class FieldInfo {

	private String indexName;
	private String type;
	private Class indexClass;
	private boolean isStore;

	public FieldInfo(String indexName, String type, Class indexClass, boolean isStore) {
		this.indexName = indexName;
		this.type = type;
		this.indexClass = indexClass;
		this.isStore = isStore;
	}
	
	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Class getIndexClass() {
		return indexClass;
	}

	public void setIndexClass(Class indexClass) {
		this.indexClass = indexClass;
	}

	public boolean isStore() {
		return isStore;
	}

	public void setStore(boolean isStore) {
		this.isStore = isStore;
	}
	
}