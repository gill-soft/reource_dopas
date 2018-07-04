package com.gillsoft;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gillsoft.abstract_rest_service.AbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.MemoryCacheHandler;
import com.gillsoft.client.Error;
import com.gillsoft.client.PointIdModel;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Seats;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.client.TripPackage;
import com.gillsoft.concurrent.PoolType;
import com.gillsoft.concurrent.ThreadPoolStore;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Price;
import com.gillsoft.model.Required;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatStatus;
import com.gillsoft.model.SeatType;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Segment;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.Trip;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;
import com.gillsoft.util.StringUtil;

@RestController
public class SearchServiceController extends AbstractTripSearchService {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	@Qualifier("MemoryCacheHandler")
	private CacheHandler cache;

	@Override
	public List<ReturnCondition> getConditionsResponse(String arg0, String arg1) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Document> getDocumentsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Tariff> getTariffsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public Required getRequiredFieldsResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public Route getRouteResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String arg0) {
		throw RestClient.createUnavailableMethod();
	}
	
	@Override
	public List<Seat> updateSeatsResponse(String arg0, List<Seat> arg1) {
		throw RestClient.createUnavailableMethod();
	}
	
	@Override
	public List<Seat> getSeatsResponse(String tripId) {
		try {
			TripIdModel model = new TripIdModel().create(tripId);
			Seats seats = client.getSeats(
					model.getIp(), model.getToId(), model.getTripId(), model.getDate());
			if (seats != null
					&& !seats.getSeat().isEmpty()) {
				List<Seat> resSeats = new ArrayList<>();
				for (Seats.Seat seat : seats.getSeat()) {
					Seat newSeat = new Seat();
					newSeat.setId(seat.getNum());
					newSeat.setNumber(seat.getNum());
					newSeat.setStatus(SeatStatus.FREE);
					newSeat.setType(SeatType.SEAT);
					resSeats.add(newSeat);
				}
				return resSeats;
			}
		} catch (Error e) {
			throw new RestClientException(e.getMessage());
		}
		return null;
	}

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		
		// формируем задания поиска
		List<Callable<TripPackage>> callables = new ArrayList<>();
		for (final Date date : request.getDates()) {
			for (final String[] pair : request.getLocalityPairs()) {
				addCallables(callables, date, pair);
			}
		}
		// запускаем задания и полученные ссылки кладем в кэш
		return putToCache(ThreadPoolStore.executeAll(PoolType.SEARCH, callables));
	}
	
	private void addCallables(List<Callable<TripPackage>> callables, Date date, String[] pair) {
		callables.add(() -> {
			try {
				validateSearchParams(pair, date);
				TripPackage tripPackage = client.getTrips(
						new PointIdModel().create(pair[0]).getIp(),
						new PointIdModel().create(pair[1]).getId(),
						date);
				if (tripPackage == null) {
					Error error = new Error();
					error.setName("Empty result");
					throw error;
				}
				SearchServiceController.addRequest(tripPackage, pair, date);
				return tripPackage;
			} catch (Error e) {
				TripPackage tripPackage = new TripPackage();
				tripPackage.setError(e);
				SearchServiceController.addRequest(tripPackage, pair, date);
				return tripPackage;
			} catch (Exception e) {
				return null;
			}
		});
	}
	
	private static void validateSearchParams(String[] pair, Date date) throws Error {
		if (date == null
				|| date.getTime() < DateUtils.truncate(new Date(), Calendar.DATE).getTime()) {
			Error error = new Error();
			error.setName("Invalid parameter \"date\"");
			throw error;
		}
		if (pair == null || pair.length < 2) {
			Error error = new Error();
			error.setName("Invalid parameter \"pair\"");
			throw error;
		}
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
		String searchId = StringUtil.generateUUID();
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
			// список заданий на дополучение результата, которого еще не было в кэше
			List<Callable<TripPackage>> callables = new ArrayList<>();
			
			// список ссылок, по которым нет еще результата
			List<Future<TripPackage>> otherFutures = new CopyOnWriteArrayList<>();
			
			// идем по ссылкам и из выполненных берем результат, а с
			// невыполненных формируем список для следующего запроса результата
			Map<String, Vehicle> vehicles = new HashMap<>();
			Map<String, Locality> localities = new HashMap<>();
			Map<String, Segment> segments = new HashMap<>();
			List<TripContainer> containers = new ArrayList<>();
			for (Future<TripPackage> future : futures) {
				if (future.isDone()) {
					try {
						TripPackage tripPackage = future.get();
						if (!tripPackage.isContinueSearch()) {
							addResult(vehicles, localities, segments, containers, tripPackage);
						} else if (tripPackage.getRequest() != null) {
							addCallables(callables, tripPackage.getRequest().getDates().get(0),
									tripPackage.getRequest().getLocalityPairs().get(0));
						}
					} catch (InterruptedException | ExecutionException e) {
					}
				} else {
					otherFutures.add(future);
				}
			}
			// запускаем дополучение результата
			if (!callables.isEmpty()) {
				otherFutures.addAll(ThreadPoolStore.executeAll(PoolType.SEARCH, callables));
			}
			// оставшиеся ссылки кладем в кэш и получаем новый ид или заканчиваем поиск
			TripSearchResponse response = null;
			if (!otherFutures.isEmpty()) {
				response = putToCache(otherFutures);
			} else {
				response = new TripSearchResponse();
			}
			response.setVehicles(vehicles);
			response.setLocalities(localities);
			response.setSegments(segments);
			response.setTripContainers(containers);
			return response;
		} catch (IOCacheException e) {
			return new TripSearchResponse(null, e);
		}
	}
	
	private void addResult(Map<String, Vehicle> vehicles, Map<String, Locality> localities,
			Map<String, Segment> segments, List<TripContainer> containers, TripPackage tripPackage) {
		TripContainer container = new TripContainer();
		container.setRequest(tripPackage.getRequest());
		if (tripPackage != null
				&& tripPackage.getTrips() != null) {
			
			List<Trip> trips = new ArrayList<>();
			for (TripPackage.Trips.Trip trip : tripPackage.getTrips().getTrip()) {
				
				// делаем ид, по которому сможем продать
				PointIdModel from = new PointIdModel().create(tripPackage.getRequest().getLocalityPairs().get(0)[0]);
				PointIdModel to = new PointIdModel().create(tripPackage.getRequest().getLocalityPairs().get(0)[1]);
				String segmentKey = new TripIdModel(
						from.getIp(),
						from.getId(),
						to.getId(),
						RestClient.dateFormat.format(tripPackage.getRequest().getDates().get(0)),
						trip.getId()).asString();
				
				// сегменты
				Trip resTrip = new Trip();
				resTrip.setId(segmentKey);
				addSegment(segmentKey, from.getId(), to.getId(), vehicles, localities, segments, trip);
				trips.add(resTrip);
			}
			container.setTrips(trips);
		}
		if (tripPackage.getError() != null) {
			container.setError(new RestError(tripPackage.getError().getMessage()));
		}
		containers.add(container);
	}
	
	private void addSegment(String segmentKey, String fromId, String toId, Map<String, Vehicle> vehicles,
			Map<String, Locality> localities, Map<String, Segment> segments, TripPackage.Trips.Trip trip) {
		
		// сегменты
		Segment segment = segments.get(segmentKey);
		if (segment == null) {
			segment = new Segment();
			
			// автобусы
			addVehicle(vehicles, segment, trip.getTuMark());
			
			// станции
			segment.setDeparture(addStation(localities, fromId));
			segment.setArrival(addStation(localities, String.join(";", fromId, toId)));
			
			setSegmentFields(segment, trip);
			
			segments.put(segmentKey, segment);
		}
	}
	
	private void setSegmentFields(Segment segment, TripPackage.Trips.Trip trip) {
		
		// рейс
		segment.setNumber(trip.getNumber());
		segment.setFreeSeatsCount(trip.getSeats());
		
		addDates(segment, trip.getFromDeparture(), trip.getToArrival());

		addPrice(segment, trip.getPrice());
	}
	
	public static void addDates(Segment segment, String departureDate, String arrivalDate) {
		try {
			segment.setDepartureDate(RestClient.fullDateFormat.parse(departureDate));
			
			// есть только время прибытия, по-этому берем дату с отправления
			segment.setArrivalDate(RestClient.fullDateFormat.parse(
					String.join(" ", RestClient.dateFormat.format(segment.getDepartureDate()), arrivalDate)));
			
			// если отправление больше прибытия, то добавляем день к прибытию
			if (segment.getArrivalDate().getTime() < segment.getDepartureDate().getTime()) {
				segment.setArrivalDate(DateUtils.addDays(segment.getArrivalDate(), 1));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private void addPrice(Segment segment, BigDecimal price) {
		Price tripPrice = new Price();
		Tariff tariff = new Tariff();
		tariff.setValue(price);
		tripPrice.setCurrency(Currency.UAH);
		tripPrice.setAmount(price);
		tripPrice.setTariff(tariff);
	}
	
	public static void addVehicle(Map<String, Vehicle> vehicles, Segment segment, String model) {
		String vehicleKey = StringUtil.md5(model);
		Vehicle vehicle = vehicles.get(vehicleKey);
		if (vehicle == null) {
			vehicle = new Vehicle();
			vehicle.setModel(model);
			vehicles.put(vehicleKey, vehicle);
		}
		segment.setVehicle(new Vehicle(vehicleKey));
	}
	
	public static Locality addStation(Map<String, Locality> localities, String id) {
		Locality fromDict = LocalityServiceController.getLocality(id);
		if (fromDict == null) {
			return null;
		}
		String fromDictId = fromDict.getId();
		try {
			fromDict = fromDict.clone();
			fromDict.setId(null);
		} catch (CloneNotSupportedException e) {
		}
		Locality locality = localities.get(fromDictId);
		if (locality == null) {
			localities.put(fromDictId, fromDict);
		}
		return new Locality(fromDictId);
	}

}
