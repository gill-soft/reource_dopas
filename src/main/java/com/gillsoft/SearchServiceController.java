package com.gillsoft;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.client.Error;
import com.gillsoft.client.PointIdModel;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.Seats;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.client.TripPackage;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RequiredField;
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
public class SearchServiceController extends SimpleAbstractTripSearchService<TripPackage> {
	
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
	public List<RequiredField> getRequiredFieldsResponse(String arg0) {
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
		return simpleInitSearchResponse(cache, request);
	}
	
	@Override
	public void addInitSearchCallables(List<Callable<TripPackage>> callables, String[] pair, Date date) {
		callables.add(() -> {
			try {
				validateSearchParams(pair, date);
				TripPackage tripPackage = client.getCachedTrips(
						new PointIdModel().create(pair[0]).getIp(),
						new PointIdModel().create(pair[1]).getId(),
						date);
				if (tripPackage == null) {
					Error error = new Error();
					error.setName("Empty result");
					throw error;
				}
				tripPackage.setRequest(TripSearchRequest.createRequest(pair, date));
				return tripPackage;
			} catch (Error e) {
				TripPackage tripPackage = new TripPackage();
				tripPackage.setError(e);
				tripPackage.setRequest(TripSearchRequest.createRequest(pair, date));
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
	
	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		return simpleGetSearchResponse(cache, searchId);
	}
	
	@Override
	public void addNextGetSearchCallablesAndResult(List<Callable<TripPackage>> callables, Map<String, Vehicle> vehicles,
			Map<String, Locality> localities, Map<String, Organisation> organisations, Map<String, Segment> segments,
			List<TripContainer> containers, TripPackage tripPackage) {
		if (!tripPackage.isContinueSearch()) {
			addResult(vehicles, localities, segments, containers, tripPackage);
		} else if (tripPackage.getRequest() != null) {
			addInitSearchCallables(callables, tripPackage.getRequest().getLocalityPairs().get(0),
					tripPackage.getRequest().getDates().get(0));
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
		segment.setPrice(tripPrice);
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
