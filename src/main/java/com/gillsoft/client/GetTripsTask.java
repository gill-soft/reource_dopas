package com.gillsoft.client;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
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
		params.put(RedisMemoryCache.OBJECT_NAME, uri.toString());
		params.put(RedisMemoryCache.UPDATE_TASK, this);
		params.put(RedisMemoryCache.TIME_TO_LIVE, Config.getCacheTimeToLive());
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheUpdateDelay());
		TripPackage tripPackage = null;
		
		// получаем рейсы для создания кэша
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			tripPackage = client.getTrips(uri);
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

}
