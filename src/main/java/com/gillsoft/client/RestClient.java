package com.gillsoft.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.RequestResponseLoggingInterceptor;
import com.gillsoft.util.RestTemplateUtil;
import com.gillsoft.util.StringUtil;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {
	
	public static final String STATIONS_CACHE_KEY = "dopas.stations.";
	public static final String TRIPS_CACHE_KEY = "dopas.trips.";
	
	private static final String GET_SALE_POINTS = "PBGetSalePoints";
	private static final String GET_STATIONS = "PBGetStations";
	private static final String GET_HASH = "PBGetStationsHash";
	private static final String GET_SCHEDULE = "PBSchedule";
	private static final String GET_TRIPS = "PBTripList";
	private static final String GET_SEATS = "PBGetSeatsList";
	private static final String GET_TICKETS = "PBGetTickets";
	private static final String CONFIRM_PAY = "PBPayConfirm";
	private static final String RETURN_INFO = "PBReturnQuery";
	private static final String CONFIRM_RETURN = "PBReturnConfirm";
	
	public static final String DEFAULT_CHARSET = "Cp1251";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm";
	public static final String TIME_FORMAT = "HH:mm";
	public static final String DECIMAL_FORMAT = "%.5f";
	
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance(DATE_FORMAT);
	public static final FastDateFormat fullDateFormat = FastDateFormat.getInstance(FULL_DATE_FORMAT);
	
	public static final int TARIFF_1_CODE = 48;
    public static final int INSURANCE_CODE = 50;
    public static final int STATION_CODE = 57;
    public static final int TARIFF_2_CODE = 67;
    public static final int REMOTE_CODE = 55;
    public static final int ADVANCE_CODE = 68;
    public static final String TARIFF_1_NAME = "Тариф 1";
    public static final String TARIFF_2_NAME = "Тариф 2";
    
    public static final String SIGNATURE_METHOD = "SHA1";
    public static final String SUCCESS_STATUS = "success";
    public static final String FAILURE_STATUS = "failure";
    public static final String CONFIRMED = "confirmed";
    public static final String CANCEL_PERCENT = "1";
    
    private static final Map<Integer, String> ERROR_CODES = new HashMap<>();
    
    static {
		ERROR_CODES.put(5001, "Попытка вернуть билет чужой организации.");
		ERROR_CODES.put(5002, "Указанный билет не найден в базе данных.");
		ERROR_CODES.put(5003, "По данному билету уже оформлен возврат.");
		ERROR_CODES.put(5004, "Операция оплаты по данному билету была отменена.");
		ERROR_CODES.put(5006, "Пригородные билеты после отправления не возвращаются.");
		ERROR_CODES.put(5007, "Пассажир воспользовался поездкой.");
		ERROR_CODES.put(5008, "Неправильная длина номера билета. Билет не найден");
		ERROR_CODES.put(5009, "Не найдена информация о продаже билета.");
		ERROR_CODES.put(5010, "Операция оплаты по данному билету не была произведена.");
    }
    
    @Autowired
    @Qualifier("RedisMemoryCache")
	private CacheHandler cache;
	
	private RestTemplate template;
	
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;
	
	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
	}
	
	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(
				RestTemplateUtil.createPoolingFactory(Config.getUrl(), 300, requestTimeout)));
		template.setMessageConverters(RestTemplateUtil.getMarshallingMessageConverters(Response.class));
		template.setInterceptors(Collections.singletonList(
				new RequestResponseLoggingInterceptor(Charset.forName(DEFAULT_CHARSET)) {

					@Override
					public ClientHttpResponse execute(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
							throws IOException {
						return new ClientHttpResponseWrapper(execution.execute(request, body));
					}

				}));
		return template;
	}
	
	public Salepoints getSalepoints(URI uri) throws Error {
		return sendRequest(uri).getSalepoints();
	}
	
	public Salepoints getCachedSalepoints() throws IOCacheException {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl())
				.queryParam("Action", GET_SALE_POINTS).build().toUri();
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getStationCacheKey(uri));
		return (Salepoints) getCachedStations(uri, new SalepointsUpdateTask(uri));
	}
	
	public Stations getStations(URI uri) throws Error {
		return getStationsInfo(uri);
	}
	
	public Stations getCachedStations(String ip) throws IOCacheException {
		URI uri = getStationsUri(ip, GET_STATIONS);
		return (Stations) getCachedStations(uri, new StationsUpdateTask(uri));
	}
	
	public Object getCachedStations(URI uri, Runnable task) throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getStationCacheKey(uri));
		params.put(RedisMemoryCache.UPDATE_TASK, task);
		return cache.read(params);
	}
	
	public Stations getStationsHash(String ip) throws Error {
		return getStationsInfo(getStationsUri(ip, GET_HASH));
	}
	
	private Stations getStationsInfo(URI uri) throws Error {
		return sendRequest(uri).getStations();
	}
	
	private URI getStationsUri(String ip, String method) {
		return UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", method)
				.queryParam("postid", Config.getOrganisation())
				.build().toUri();
	}
	
	public Schedule getSchedule(String ip, boolean hashOnly) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_SCHEDULE)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("hashonly", hashOnly ? 1 : 0)
				.build().toUri();
		return sendRequest(uri).getSchedule();
	}
	
	public TripPackage getCachedTrips(String ip, String to, Date when) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_TRIPS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", dateFormat.format(when))
				.build().toUri();
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getTripsCacheKey(uri));
		params.put(RedisMemoryCache.UPDATE_TASK, new GetTripsTask(uri));
		try {
			return (TripPackage) cache.read(params);
		} catch (IOCacheException e) {
			
			// ставим пометку, что кэш еще формируется
			TripPackage tripPackage = new TripPackage();
			tripPackage.setContinueSearch(true);
			return tripPackage;
		} catch (Exception e) {
			Error error = new Error();
			error.setName(e.getMessage());
			throw error;
		}
	}
	
	public TripPackage getTrips(URI uri) throws Error {
		return sendRequest(searchTemplate, uri).getTripPackage();
	}
	
	public Seats getSeats(String ip, String to, String tripId, String when) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_SEATS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", when)
				.queryParam("id", tripId)
				.build().toUri();
		return sendRequest(uri).getSeats();
	}
	
	public ResResult getTickets(String ip, String to, String tripId, String when, String transactionId, int seatsCount,
			String places) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_TICKETS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", when)
				.queryParam("id", tripId)
				.queryParam("resid", transactionId)
				.queryParam(places == null || places.isEmpty() ? "seats" : "places",
						places == null || places.isEmpty() ? seatsCount : places)
				.build().toUri();
		return sendRequest(uri).getResResult();
	}
	
	public Accepted confirmTickets(String ip, String transactionId, String status)
			throws Error, NoSuchAlgorithmException {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", CONFIRM_PAY)
				.queryParam("resid", transactionId)
				.queryParam("status", status)
				.queryParam("signature", getSignature(transactionId, status, Config.getKey()))
				.build().toUri();
		return sendRequest(uri).getAccepted();
	}
	
	public TicketType getReturnInfo(String ip, String ticketId) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", RETURN_INFO)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("ticket", ticketId)
				.build().toUri();
		return sendRequest(uri).getInformation();
	}
	
	public Confirmed confirmReturn(String ip, String ticketId, BigDecimal percent) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", CONFIRM_RETURN)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("ticket", ticketId)
				.queryParam("percent", String.format(DECIMAL_FORMAT, percent))
				.build().toUri();
		return sendRequest(uri).getConfirmed();
	}
	
	private Response sendRequest(URI uri) throws Error {
		return sendRequest(template, uri);
	}
	
	private Response sendRequest(RestTemplate template, URI uri) throws Error {
		try {
			Response response = template.getForObject(uri, Response.class);
			if (response.getError() != null) {
				throw response.getError();
			} else {
				return response;
			}
		} catch (RestClientException e) {
			Error error = new Error();
			error.setName(e.getMessage());
			throw error;
		}
	}
	
	private String getHost(String ip) {
		return "http://".concat(ip).concat("/cgi-bin/gtmgate");
	}
	
	private String getSignature(String... args) throws NoSuchAlgorithmException {
        String signature = "";
        for (String string : args) {
            signature = signature + string;
        }
        MessageDigest md = MessageDigest.getInstance(SIGNATURE_METHOD);
        md.update(signature.getBytes());
        return StringUtil.toBase64(md.digest());
    }
	
	public static RestClientException createUnavailableMethod() {
		return new RestClientException("Method is unavailable");
	}

	public CacheHandler getCache() {
		return cache;
	}
	
	public static String getStationCacheKey(URI uri) {
		return STATIONS_CACHE_KEY + uri.toString();
	}
	
	public static String getTripsCacheKey(URI uri) {
		return TRIPS_CACHE_KEY + uri.toString();
	}
	
}
