package com.gillsoft.client;

import java.net.URI;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.Salepoints.Salepoint;

public class SalepointsUpdateTask extends AbstractStationsUpdateTask {

	private static final long serialVersionUID = 7883832040961900893L;
	
	public SalepointsUpdateTask() {
		super();
	}

	public SalepointsUpdateTask(URI uri) {
		super(uri);
	}

	@Override
	protected Object createCacheObject(RestClient client, Map<String, Object> params) throws IOCacheException, Error {
		Salepoints salepoints = client.getSalepoints(uri);
		if (salepoints == null) {
			salepoints = (Salepoints) client.getCache().read(params);
		}
		if (salepoints != null
				&& salepoints.getSalepoint() != null) {
			for (Salepoint point : salepoints.getSalepoint()) {
				try {
					client.getCachedStations(point.getIp());
				} catch (IOCacheException e) {
				}
			}
		}
		return salepoints;
	}

}
