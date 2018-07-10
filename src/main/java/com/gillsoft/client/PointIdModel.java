package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class PointIdModel extends AbstractJsonModel {
	
	private static final long serialVersionUID = 584282348358969654L;

	private String id;
	
	private String ip;
	
	private String fromId;
	
	public PointIdModel() {
		
	}

	public PointIdModel(String id, String ip, String fromId) {
		this.id = id;
		this.ip = ip;
		this.fromId = fromId;
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

	public String getFromId() {
		return fromId;
	}

	public void setFromId(String fromId) {
		this.fromId = fromId;
	}
	
	@Override
	public PointIdModel create(String json) {
		return (PointIdModel) super.create(json);
	}
	
}
