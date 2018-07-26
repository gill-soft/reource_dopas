package com.gillsoft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractLocalityService;
import com.gillsoft.cache.IOCacheException;
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
	private static Map<String, List<String>> binding;
	
	private static Map<String, Locality> internalAll;
	
	@Autowired
	private RestClient client;

	@Override
	public List<Locality> getAllResponse(LocalityRequest request) {
		return getAllLocalities(request);
	}

	@Override
	public Map<String, List<String>> getBindingResponse(LocalityRequest request) {
		createLocalities();
		return binding;
	}

	@Override
	public List<Locality> getUsedResponse(LocalityRequest request) {
		return getAllLocalities(request);
	}
	
	private List<Locality> getAllLocalities(LocalityRequest request) {
		createLocalities();
		if (all != null) {
			return Lists.newArrayList(all);
		}
		return null;
	}
	
	@Scheduled(initialDelay = 60000, fixedDelay = 900000)
	private void createLocalities() {
		if (LocalityServiceController.all == null) {
			synchronized (LocalityServiceController.class) {
				if (LocalityServiceController.all == null) {
					boolean cacheError = true;
					do {
						try {
							Salepoints salepoints = client.getCachedSalepoints();
							if (salepoints != null
									&& salepoints.getSalepoint() != null) {
								Set<Locality> all = new CopyOnWriteArraySet<>();
								Map<String, Locality> internalAll = new ConcurrentHashMap<>();
								Map<String, List<String>> binding = new ConcurrentHashMap<>();
								for (Salepoint point : salepoints.getSalepoint()) {
									Locality dispatch = new Locality();
									dispatch.setId(new PointIdModel(point.getId(), point.getIp(), null).asString());
									dispatch.setName(Lang.UA, point.getName());
									all.add(dispatch);
									internalAll.put(point.getId(), dispatch);
									List<Locality> arrivals = getLocalities(internalAll, point);
									if (arrivals != null) {
										List<String> arrivalIds = new CopyOnWriteArrayList<>();
										for (Locality arrival : arrivals) {
											arrivalIds.add(arrival.getId());
											all.add(arrival);
										}
										binding.put(dispatch.getId(), arrivalIds);
									}
								}
								LocalityServiceController.all = all;
								LocalityServiceController.internalAll = internalAll;
								LocalityServiceController.binding = binding;
							}
							cacheError = false;
						} catch (IOCacheException e) {
							try {
								TimeUnit.MILLISECONDS.sleep(100);
							} catch (InterruptedException ie) {
							}
						}
					} while (cacheError);
				}
			}
		}
	}
	
	private List<Locality> getLocalities(Map<String, Locality> internalAll, Salepoint salepoint) throws IOCacheException {
		Stations stations = client.getCachedStations(salepoint.getIp());
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
		return null;
	}
	
	/**
	 * Возвращает населенный пункт по его ид
	 * 
	 * @param id
	 *            Для пунктов отправления ид пункта отправления, для пунктов
	 *            прибытия ид отправления + ";" + ид прибытия
	 * @return Населенный пункт
	 */
	public static Locality getLocality(String id) {
		if (internalAll == null) {
			return null;
		}
		return internalAll.get(id);
	}

}
