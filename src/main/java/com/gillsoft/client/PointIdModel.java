package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class PointIdModel extends AbstractJsonModel {
	
	private static final long serialVersionUID = 1903408619589522994L;

	private String id;
	
	private String ip;
	
	public PointIdModel() {
		
	}

	public PointIdModel(String id, String ip) {
		this.id = id;
		this.ip = ip;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	@Override
	public PointIdModel create(String json) {
		return (PointIdModel) super.create(json);
	}
	
}
