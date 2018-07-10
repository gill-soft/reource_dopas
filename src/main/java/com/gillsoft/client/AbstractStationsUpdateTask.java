package com.gillsoft.client;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.util.ContextProvider;

public abstract class AbstractStationsUpdateTask implements Runnable, Serializable {

	private static final long serialVersionUID = 5773203262295301942L;

	protected URI uri;
	
	public AbstractStationsUpdateTask() {

	}

	public AbstractStationsUpdateTask(URI uri) {
		this.uri = uri;
	}
	
	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.getStationCacheKey(uri));
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		
		try {
			RestClient client = ContextProvider.getBean(RestClient.class);
			Object cacheObject = createCacheObject(client, params);
			params.put(RedisMemoryCache.UPDATE_TASK, this);
			client.getCache().write(cacheObject, params);
		} catch (Error | IOCacheException e) {
		}
	}
	
	protected abstract Object createCacheObject(RestClient client, Map<String, Object> params)
			throws IOCacheException, Error;

}
