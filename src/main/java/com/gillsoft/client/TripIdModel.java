package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = -4570318053620484041L;

	private String ip;
	
	private String fromId;

	private String toId;

	private String date;

	private String tripId;

	public TripIdModel() {

	}

	public TripIdModel(String ip, String fromId, String toId, String date, String tripId) {
		this.ip = ip;
		this.fromId = fromId;
		this.toId = toId;
		this.date = date;
		this.tripId = tripId;
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

	public String getToId() {
		return toId;
	}

	public void setToId(String toId) {
		this.toId = toId;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	@Override
	public TripIdModel create(String json) {
		return (TripIdModel) super.create(json);
	}
}
