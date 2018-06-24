package com.gillsoft.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gillsoft.model.AbstractJsonModel;

@JsonInclude(Include.NON_NULL)
public class ServiceIdModel extends AbstractJsonModel {
	
	private String ip;
	
	private String transactionId;
	
	private String ticketNumber;

	public ServiceIdModel() {
		
	}

	public ServiceIdModel(String ip, String transactionId, String ticketNumber) {
		this.ip = ip;
		this.transactionId = transactionId;
		this.ticketNumber = ticketNumber;
	}

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

	public String getTicketNumber() {
		return ticketNumber;
	}

	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}
	
	@Override
	public ServiceIdModel create(String json) {
		return (ServiceIdModel) super.create(json);
	}
	
}
