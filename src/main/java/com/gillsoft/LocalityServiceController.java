package com.gillsoft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractLocalityService;
import com.gillsoft.client.Error;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Salepoints;
import com.gillsoft.client.Salepoints.Salepoint;
import com.gillsoft.client.Stations;
import com.gillsoft.client.Stations.Station;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.request.LocalityRequest;
import com.google.common.collect.Lists;

@RestController
public class LocalityServiceController extends AbstractLocalityService {
	
	private static Set<Locality> all;
	private static Map<Locality, List<Locality>> binding;

	@Override
	public List<Locality> getAllResponse(LocalityRequest request) {
		return getAllLocalities(request);
	}

	@Override
	public Map<String, List<String>> getBindingResponse(LocalityRequest request) {
		createLocalities();
		if (binding != null) {
			Map<String, List<String>> result = new HashMap<>();
			for (Entry<Locality, List<Locality>> entry : binding.entrySet()) {
				if (entry.getValue() != null) {
					List<String> ids = new ArrayList<>();
					for (Locality locality : entry.getValue()) {
						ids.add(locality.getId());
					}
					result.put(entry.getKey().getId(), ids);
				}
			}
			return result;
		}
		return null;
	}

	@Override
	public List<Locality> getUsedResponse(LocalityRequest request) {
		return getAllLocalities(request);
	}
	
	private List<Locality> getAllLocalities(LocalityRequest request) {
		createLocalities();
		return Lists.newArrayList(all);
	}
	
	private synchronized void createLocalities() {
		if (all == null) {
			synchronized (LocalityServiceController.class) {
				if (all == null) {
					try {
						Salepoints salepoints = RestClient.getInstance().getSalepoints();
						if (salepoints != null
								&& salepoints.getSalepoint() != null) {
							all = new CopyOnWriteArraySet<>();
							for (Salepoint point : salepoints.getSalepoint()) {
								Locality dispatch = new Locality();
								dispatch.setId(String.join(";", point.getId(), point.getIp()));
								dispatch.setName(Lang.UA, point.getName());
								all.add(dispatch);
								List<Locality> arrivals = getLocalities(point);
								if (arrivals != null) {
									all.addAll(arrivals);
									getBindingMap().put(dispatch, arrivals);
								}
							}
						}
					} catch (Error e) {
						// TODO: handle exception
					}
				}
			}
		}
	}
	
	private Map<Locality, List<Locality>> getBindingMap() {
		if (binding == null) {
			binding = new ConcurrentHashMap<>();
		}
		return binding;
	}
	
	private List<Locality> getLocalities(Salepoint salepoint) {
		try {
			Stations stations = RestClient.getInstance().getStations(salepoint.getIp());
			if (stations != null
					&& stations.getStation() != null) {
				List<Locality> localities = new CopyOnWriteArrayList<>();
				for (Station station : stations.getStation()) {
					Locality arrival = new Locality();
					arrival.setId(String.join(";", salepoint.getId(), salepoint.getIp(), station.getId()));
					arrival.setName(Lang.UA, station.getName());
					localities.add(arrival);
				}
				return localities;
			}
		} catch (Error e) {
			// TODO: handle exception
		}
		return null;
	}

}
