package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class TripIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 488895518243117139L;

	private String ip;

	private String toId;

	private String date;

	private String tripId;

	public TripIdModel() {

	}

	public TripIdModel(String ip, String toId, String date, String tripId) {
		this.ip = ip;
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
