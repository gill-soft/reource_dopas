package com.gillsoft;

import java.math.BigDecimal;
import java.text.ParseException;
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

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.MemoryCacheHandler;
import com.gillsoft.client.Error;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Seats;
import com.gillsoft.client.TripPackage;
import com.gillsoft.concurrent.PoolType;
import com.gillsoft.concurrent.ThreadPoolStore;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Fare;
import com.gillsoft.model.Lang;
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
import com.gillsoft.model.Trip;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;
import com.gillsoft.util.StringUtil;

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
	public List<Seat> getSeatsResponse(String tripId) {
		String[] params = tripId.split(";");
		try {
			Seats seats = RestClient.getInstance().getSeats(params[0], params[1], params[2], params[3]);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
						validateSearchParams(pair, date);
						TripPackage tripPackage = RestClient.getInstance().getTrips(
								pair[0].split(";")[1], pair[1].split(";")[2], date);
						SearchServiceController.addRequest(tripPackage, pair, date);
						return tripPackage;
					} catch (Error e) {
						TripPackage tripPackage = new TripPackage();
						tripPackage.setError(e);
						SearchServiceController.addRequest(tripPackage, pair, date);
						return tripPackage;
					}
				});
			}
		}
		// запускаем задания и полученные ссылки кладем в кэш
		return putToCache(ThreadPoolStore.executeAll(PoolType.SEARCH, callables));
	}
	
	private static void validateSearchParams(String[] pair, Date date) throws Error {
		if (date == null
				|| date.getTime() < new Date().getTime()) {
			Error error = new Error();
			error.setName("Invalid parameter \"date\"");
			throw error;
		}
		if (pair == null || pair.length < 2
				|| pair[0] == null || pair[1] == null
				|| pair[0].split(";").length < 2
				|| pair[1].split(";").length < 3) {
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
			
			// идем по ссылкам и из выполненных берем результат, а с
			// невыполненных формируем список для следующего запроса результата
			Map<String, Vehicle> vehicles = new HashMap<>();
			Map<String, Locality> localities = new HashMap<>();
			Map<String, Segment> segments = new HashMap<>();
			List<TripContainer> containers = new ArrayList<>();
			for (Future<TripPackage> future : futures) {
				if (future.isDone()) {
					try {
						addResult(vehicles, localities, segments, containers, future.get());
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
				
				// сегменты
				Trip resTrip = new Trip();
				resTrip.setId(addSegment(vehicles, localities, segments, trip));
				trips.add(resTrip);
			}
			// делаем ид, по которому сможем продать
			for (Segment segment : segments.values()) {
				segment.setId(String.join(";",
						tripPackage.getRequest().getLocalityPairs().get(0)[0].split(";")[1],
						tripPackage.getRequest().getLocalityPairs().get(0)[1].split(";")[2],
						RestClient.dateFormat.format(tripPackage.getRequest().getDates().get(0)),
						segment.getId()));
			}
			container.setTrips(trips);
		}
		if (tripPackage.getError() != null) {
			container.setError(new RestError(tripPackage.getError().getMessage()));
		}
		containers.add(container);
	}
	
	private String addSegment(Map<String, Vehicle> vehicles, Map<String, Locality> localities,
			Map<String, Segment> segments, TripPackage.Trips.Trip trip) {
		
		// сегменты
		String segmentKey = StringUtil.md5(String.join(";", trip.getId(), trip.getFirstPointCode(),
				trip.getLastPointCode(), trip.getFromDeparture(), trip.getToArrival(), trip.getPrice().toString()));
		Segment segment = segments.get(segmentKey);
		if (segment == null) {
			segment = new Segment();
			
			// автобусы
			addVehicle(vehicles, segment, trip);
			
			// станции
			segment.setDepartureId(addStation(localities, trip.getFirstPointCode(), trip.getFistPointName()));
			segment.setArrivalId(addStation(localities, trip.getLastPointCode(), trip.getLastPointName()));
			
			setSegmentFields(segment, trip);
			
			segments.put(segmentKey, segment);
		}
		return segmentKey;
	}
	
	private void setSegmentFields(Segment segment, TripPackage.Trips.Trip trip) {
		
		// рейс
		segment.setId(trip.getId());
		segment.setNumber(trip.getNumber());
		segment.setFreeSeatsCount(trip.getSeats());
		try {
			segment.setDepartureDate(RestClient.fullDateFormat.parse(trip.getFromDeparture()));
			
			// есть только время прибытия, по-этому берем дату с отправления
			segment.setArrivalDate(RestClient.fullDateFormat.parse(
					String.join(" ", RestClient.dateFormat.format(segment.getDepartureDate()), trip.getToArrival())));
			
			// если отправление больше прибытия, то добавляем день к прибытию
			if (segment.getArrivalDate().getTime() < segment.getDepartureDate().getTime()) {
				segment.setArrivalDate(DateUtils.addDays(segment.getArrivalDate(), 1));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		addPrice(segment, trip.getPrice());
	}
	
	private void addPrice(Segment segment, BigDecimal price) {
		Price tripPrice = new Price();
		Fare fare = new Fare();
		fare.setValue(price);
		tripPrice.setCurrency(Currency.UAH);
		tripPrice.setAmount(price);
		tripPrice.setFare(fare);
	}
	
	private void addVehicle(Map<String, Vehicle> vehicles, Segment segment, TripPackage.Trips.Trip trip) {
		String vehicleKey = StringUtil.md5(trip.getTuMark());
		Vehicle vehicle = vehicles.get(vehicleKey);
		if (vehicle == null) {
			vehicle = new Vehicle();
			vehicle.setModel(trip.getTuMark());
			vehicles.put(vehicleKey, vehicle);
		}
		segment.setVehicleId(vehicleKey);
	}
	
	private String addStation(Map<String, Locality> localities, String id, String name) {
		String key = StringUtil.md5(String.join(";", id, name));
		Locality locality = localities.get(key);
		if (locality == null) {
			locality = new Locality();
			locality.setId(id);
			locality.setName(Lang.UA, name);
			localities.put(key, locality);
		}
		return key;
	}

}
