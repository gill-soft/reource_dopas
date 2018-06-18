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
import org.springframework.web.bind.annotation.RestController;

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
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;

@RestController
public class SearchServiceController extends AbstractTripSearchService {
	
	@Autowired
	private CacheHandler cache;

	@Override
	public List<ReturnCondition> getConditionsResponse(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Document> getDocumentsResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Fare> getFaresResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Trip getInfoResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Required getRequiredFieldsResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Route getRouteResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Seat> updateSeatsResponse(String arg0, List<Seat> arg1) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Seat> getSeatsResponse(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		
		// формируем задания поиска
		List<Callable<TripPackage>> callables = new ArrayList<>();
		for (final Date date : request.getDates()) {
			for (final String[] pair : request.getLocalityPairs()) {
				callables.add(() -> {
					try {
						TripPackage tripPackage = RestClient.getInstance().getTrips(
								pair[0].split(";")[1], pair[1].split(";")[2], date);
						addRequest(tripPackage, pair, date);
						return tripPackage;
					} catch (Error e) {
						TripPackage tripPackage = new TripPackage();
						tripPackage.setError(e);
						addRequest(tripPackage, pair, date);
						return tripPackage;
					}
				});
			}
		}
		// запускаем задания и полученные ссылки кладем в кэш
		return putToCache(ThreadPoolStore.executeAll(PoolType.SEARCH, callables));
	}
	
	private static void addRequest(TripPackage tripPackage, String[] pair, Date date) {
		TripSearchRequest request = new TripSearchRequest();
		List<String[]> pairs = new ArrayList<>(1);
		pairs.add(pair);
		request.setLocalityPairs(pairs);
		List<Date> dates = new ArrayList<>(1);
		dates.add(date);
		request.setDates(dates);
		tripPackage.setRequest(request);
	}
	
	private TripSearchResponse putToCache(List<Future<TripPackage>> futures) {
		String searchId = UUID.randomUUID().toString();
		Map<String, Object> params = new HashMap<>();
		params.put(MemoryCacheHandler.OBJECT_NAME, searchId);
		params.put(MemoryCacheHandler.TIME_TO_LIVE, 60000l);
		try {
			cache.write(futures, params);
			return new TripSearchResponse(null, searchId);
		} catch (IOCacheException e) {
			return new TripSearchResponse(null, e);
		}
	}
	
	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		Map<String, Object> params = new HashMap<>();
		params.put(MemoryCacheHandler.OBJECT_NAME, searchId);
		try {
			// вытаскиваем с кэша ссылки, по которым нужно получить результат поиска
			@SuppressWarnings("unchecked")
			List<Future<TripPackage>> futures = (List<Future<TripPackage>>) cache.read(params);
			if (futures == null) {
				throw new IOCacheException("Too late for getting result");
			}
			
			// список ссылок, по которым нет еще результата
			List<Future<TripPackage>> otherFutures = new CopyOnWriteArrayList<>();
			
			// идем по ссылка и из выполненных берем результат, а с
			// невыполненных формируем список для следующего запроса результат
			List<TripContainer> containers = new ArrayList<>();
			for (Future<TripPackage> future : futures) {
				if (future.isDone()) {
					try {
						addResult(containers, future.get());
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
			response.setTripContainers(containers);
			return response;
		} catch (IOCacheException e) {
			return new TripSearchResponse(null, e);
		}
	}
	
	private void addResult(List<TripContainer> containers, TripPackage tripPackage) {
		TripContainer container = new TripContainer();
		container.setRequest(tripPackage.getRequest());
		if (tripPackage != null
				&& tripPackage.getTrips() != null) {
			List<Trip> trips = new ArrayList<>();
			for (com.gillsoft.client.TripPackage.Trips.Trip trip : tripPackage.getTrips().getTrip()) {
				Trip resTrip = new Trip();
				resTrip.setId(trip.getId());
				resTrip.setNumber(trip.getNumber());
				// TODO other properties
				trips.add(resTrip);
			}
			container.setTrips(trips);
		}
		if (tripPackage.getError() != null) {
			container.setError(new java.lang.Error(String.format("code: %s1 message: %s2",
					tripPackage.getError().getCode(), tripPackage.getError().getMessage())));
		}
		containers.add(container);
	}

}
