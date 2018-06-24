package com.gillsoft.client;

import java.util.ArrayList;
import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {
	
	private List<ServiceIdModel> services = new ArrayList<>();

	public List<ServiceIdModel> getServices() {
		return services;
	}

	public void setServices(List<ServiceIdModel> services) {
		this.services = services;
	}
	
	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}
	
}
