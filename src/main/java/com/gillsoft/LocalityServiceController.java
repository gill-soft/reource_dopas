package com.gillsoft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractLocalityService;
import com.gillsoft.client.Error;
import com.gillsoft.client.PointIdModel;
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
	private static Map<String, List<String>> binding = new ConcurrentHashMap<>();
	
	private static Map<String, Locality> internalAll = new ConcurrentHashMap<>();

	@Override
	public List<Locality> getAllResponse(LocalityRequest request) {
		return getAllLocalities(request);
	}

	@Override
	public Map<String, List<String>> getBindingResponse(LocalityRequest request) {
		createLocalities();
		if (binding != null) {
			return binding;
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
								dispatch.setId(new PointIdModel(point.getId(), point.getIp(), null).asString());
								dispatch.setName(Lang.UA, point.getName());
								all.add(dispatch);
								getInternalAll().put(point.getId(), dispatch);
								List<Locality> arrivals = getLocalities(point);
								if (arrivals != null) {
									List<String> arrivalIds = new CopyOnWriteArrayList<>();
									for (Locality arrival : arrivals) {
										arrivalIds.add(arrival.getId());
										all.add(arrival);
									}
									getBindingMap().put(dispatch.getId(), arrivalIds);
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
	
	private Map<String, List<String>> getBindingMap() {
		return binding;
	}
	
	private static Map<String, Locality> getInternalAll() {
		return internalAll;
	}
	
	private List<Locality> getLocalities(Salepoint salepoint) {
		try {
			Stations stations = RestClient.getInstance().getStations(salepoint.getIp());
			if (stations != null
					&& stations.getStation() != null) {
				List<Locality> localities = new ArrayList<>();
				for (Station station : stations.getStation()) {
					Locality arrival = new Locality();
					arrival.setId(new PointIdModel(salepoint.getId(), salepoint.getIp(), station.getId()).asString());
					arrival.setName(Lang.UA, station.getName());
					localities.add(arrival);
					internalAll.put(String.join(";", salepoint.getId(), station.getId()), arrival);
				}
				return localities;
			}
		} catch (Error e) {
			// TODO: handle exception
		}
		return null;
	}
	
	public static Locality getLocality(String id) {
		return getInternalAll().get(id);
	}

}
