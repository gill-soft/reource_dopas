package com.gillsoft.client;

import java.net.URI;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;

public class StationsUpdateTask extends AbstractStationsUpdateTask {

	private static final long serialVersionUID = -447739350784487326L;
	
	public StationsUpdateTask() {
		super();
	}

	public StationsUpdateTask(URI uri) {
		super(uri);
	}

	@Override
	protected Object createCacheObject(RestClient client, Map<String, Object> params) throws IOCacheException, Error {
		Stations stations = client.getStations(uri);
		if (stations == null) {
			stations = (Stations) client.getCache().read(params);
		}
		return stations;
	}

}
