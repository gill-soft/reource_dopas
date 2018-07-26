package com.gillsoft.client;

import java.io.Serializable;
import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.client.TripPackage.Trips;
import com.gillsoft.util.ContextProvider;

public class GetTripsTask implements Runnable, Serializable {
	
	private static final long serialVersionUID = -612450869121241871L;
	
	private URI uri;
	
	public GetTripsTask() {

	}

	public GetTripsTask(URI uri) {
		this.uri = uri;
	}

	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.getTripsCacheKey(uri));
		params.put(RedisMemoryCache.UPDATE_TASK, this);
		TripPackage tripPackage = null;
		
		// получаем рейсы для создания кэша
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			tripPackage = client.getTrips(uri);
			params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheTripUpdateDelay());
			params.put(RedisMemoryCache.TIME_TO_LIVE, getTimeToLive(tripPackage));
		} catch (Error e) {
			
			// ошибку поиска тоже кладем в кэш но с другим временем жизни
			params.put(RedisMemoryCache.TIME_TO_LIVE, Config.getCacheErrorTimeToLive());
			params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheErrorUpdateDelay());
			Error error = new Error();
			error.setName(e.getMessage());
			tripPackage = new TripPackage();
			tripPackage.setError(e);
		}
		try {
			client.getCache().write(tripPackage, params);
		} catch (IOCacheException e) {
		}
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	// время жизни до момента самого позднего отправления
	private long getTimeToLive(TripPackage tripPackage) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		for (Trips.Trip trip : tripPackage.getTrips().getTrip()) {
			try {
				Date date = RestClient.fullDateFormat.parse(trip.getFromDeparture());
				if (date.getTime() > max) {
					max = date.getTime();
				}
			} catch (ParseException e) {
			}
		}
		return max - System.currentTimeMillis();
	}

}
