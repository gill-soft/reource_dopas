package com.gillsoft.client;

import java.util.ArrayList;
import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {
	
	private static final long serialVersionUID = 5661521517528841959L;
	
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
