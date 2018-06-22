package com.gillsoft.client;

public class OrderIdModel extends AbstractModel {
	
	private String ip;
	
	private String transactionId;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
}
