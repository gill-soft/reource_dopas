package com.gillsoft;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;

import com.gillsoft.abstract_rest_service.AbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.MemoryCacheHandler;
import com.gillsoft.client.Error;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TripPackage;
import com.gillsoft.concurrent.PoolType;
import com.gillsoft.concurrent.ThreadPoolStore;
import com.gillsoft.model.Document;
import com.gillsoft.model.Fare;
import com.gillsoft.model.Required;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Trip;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;

public class SearchServiceController extends AbstractTripSearchService {
	
	@Autowired
	private CacheHandler cache;

	@Override
	public List<ReturnCondition> getConditions(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getDocuments(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Fare> getFares(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Trip getInfo(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Required getRequiredFields(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Route getRoute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SeatsScheme getSeatsScheme(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Seat updateSeat(String arg0, List<Seat> arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Seat> getSeats(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TripSearchResponse initSearch(TripSearchRequest request) {
		
		// формируем задания поиска
		List<Callable<TripPackage>> callables = new ArrayList<>();
		for (Date date : request.getDates()) {
			for (String[] pair : request.getLocalityPairs()) {
				callables.add(() -> {
					try {
						return RestClient.getInstance().getTrips(
								pair[0].split(";")[0], pair[1], date);
					} catch (Error e) {
						TripPackage tripPackage = new TripPackage();
						tripPackage.setError(e);
						return tripPackage;
					}
				});
			}
		}
		// запускаем задания и полученные ссылки кладем в кэш
		return putToCache(ThreadPoolStore.executeAll(PoolType.SEARCH, callables));
	}
	
	private TripSearchResponse putToCache(List<Future<TripPackage>> futures) {
		String searchId = UUID.randomUUID().toString();
		Map<String, Object> params = new HashMap<>();
		params.put(MemoryCacheHandler.OBJECT_NAME, searchId);
		params.put(MemoryCacheHandler.TIME_TO_LIVE, 60000);
		try {
			cache.write(futures, params);
			return new TripSearchResponse(null, searchId);
		} catch (IOCacheException e) {
			return new TripSearchResponse(null, e);
		}
	}
	
	@Override
	public TripSearchResponse getSearchResult(String searchId) {
		Map<String, Object> params = new HashMap<>();
		params.put(MemoryCacheHandler.OBJECT_NAME, searchId);
		try {
			// вытаскиваем с кэша ссылки, по которым нужно получить результат поиска
			@SuppressWarnings("unchecked")
			List<Future<TripPackage>> futures = (List<Future<TripPackage>>) cache.read(params);
			
			// список ссылок, по которым нет еще результата
			List<Future<TripPackage>> otherFutures = new CopyOnWriteArrayList<>();
			
			// идем по ссылка и из выполненных берем результат, а с
			// невыполненных формируем список для следующего запроса результат
			List<Trip> trips = new ArrayList<>();
			for (Future<TripPackage> future : futures) {
				if (future.isDone()) {
					try {
						addResult(trips, future.get());
					} catch (InterruptedException | ExecutionException e) {
					}
				} else {
					otherFutures.add(future);
				}
			}
			// оставшиеся ссылки кладем в кэш и получаем новый ид или заканчиваем поиск
			TripSearchResponse response = null;
			if (!otherFutures.isEmpty()) {
				response = putToCache(otherFutures);
			} else {
				response = new TripSearchResponse();
			}
			response.setTrips(trips);
			return response;
		} catch (IOCacheException e) {
			return new TripSearchResponse(null, e);
		}
	}
	
	private void addResult(List<Trip> trips, TripPackage tripPackage) {
		if (tripPackage != null
				&& tripPackage.getTrips() != null) {
			for (com.gillsoft.client.TripPackage.Trips.Trip trip : tripPackage.getTrips().getTrip()) {
				Trip resTrip = new Trip();
				resTrip.setId(trip.getId());
				resTrip.setNumber(trip.getNumber());
				// TODO other properties
				trips.add(resTrip);
			}
		}
	}

}
