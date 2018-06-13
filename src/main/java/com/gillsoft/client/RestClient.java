package com.gillsoft.client;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.client.Salepoints.Salepoint;

import sun.misc.BASE64Encoder;

public class RestClient {
	
	private static final String GET_SALE_POINTS = "PBGetSalePoints";
	private static final String GET_STATIONS = "PBGetStations";
	private static final String GET_HASH = "PBGetStationsHash";
	private static final String GET_SCHEDULE = "PBSchedule";
	private static final String GET_TRIPS = "PBTripList";
	private static final String GET_SEATS = "PBGetSeatsList";
	private static final String GET_TICKETS = "PBGetTickets";
	private static final String CONFIRM_PAY = "PBPayConfirm";
	private static final String RETURN = "PBReturnQuery";
	private static final String CONFIRM_RETURN = "PBReturnConfirm";
	
	public static final String DEFAULT_CHARSET = "Cp1251";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm";
	public static final String TIME_FORMAT = "HH:mm";
	
	private static FastDateFormat dateFormat = FastDateFormat.getInstance(DATE_FORMAT);
	
	public final static String TARIFF_1_CODE = "48";
    public final static String INSURANCE_CODE = "50";
    public final static String STATION_CODE = "57";
    public final static String TARIFF_2_CODE = "67";
    public final static String REMOTE_CODE = "55";
    public final static String ADVANCE_CODE = "68";
    public final static String SIGNATURE_METHOD = "SHA1";
    public final static String SUCCESS_STATUS = "success";
    public final static String FAILURE_STATUS = "failure";
    public final static String CONFIRMED = "confirmed";
    public final static String CANCEL_PERCENT = "1";
    
    private final static Map<Integer, String> ERROR_CODES = new HashMap<>();
    
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
	
	private static RestClient instance;
	
	private RestTemplate template;
	
	private RestClient() {
		template = createNewPoolingTemplate();
	}
	
	public static RestClient getInstance() {
		if (instance == null) {
			synchronized (RestClient.class) {
				if (instance == null) {
					instance = new RestClient();
				}
			}
		}
		return instance;
	}
	
	public RestTemplate createNewPoolingTemplate() {
		
		// создаем пул соединений
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(Config.getUrl())), 300);
		
		HttpClient httpClient = HttpClients.custom()
		        .setConnectionManager(connectionManager)
		        .build();
		
		// настраиваем таймауты
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setConnectTimeout(1000);
		factory.setConnectionRequestTimeout(Config.getRequestTimeout());
		factory.setHttpClient(httpClient);
		
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(factory));
		template.setMessageConverters(getMessageConverters());
		template.setInterceptors(Collections.singletonList(new RequestResponseLoggingInterceptor()));
		return template;
	}
	
	private List<HttpMessageConverter<?>> getMessageConverters() {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Response.class);
		return Collections.singletonList(new MarshallingHttpMessageConverter(marshaller, marshaller));
	}
	
	public Salepoints getSalepoints() throws Error {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl())
				.queryParam("Action", GET_SALE_POINTS).build().toUri();
		return sendRequest(uri).getSalepoints();
	}
	
	public Stations getStations(String ip) throws Error {
		return getStationsInfo(ip, GET_STATIONS);
	}
	
	public Stations getStationsHash(String ip) throws Error {
		return getStationsInfo(ip, GET_HASH);
	}
	
	public Stations getStationsInfo(String ip, String method) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", method)
				.queryParam("postid", Config.getOrganisation())
				.build().toUri();
		return sendRequest(uri).getStations();
	}
	
	public Schedule getSchedule(String ip, boolean hashOnly) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_SCHEDULE)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("hashonly", hashOnly ? 1 : 0)
				.build().toUri();
		return sendRequest(uri).getSchedule();
	}
	
	public TripPackage getTrips(String ip, String to, Date when) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_TRIPS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", dateFormat.format(when))
				.build().toUri();
		return sendRequest(uri).getTripPackage();
	}
	
	public Seats getSeats(String ip, String to, String id, Date when) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_SEATS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", dateFormat.format(when))
				.queryParam("id", id)
				.build().toUri();
		return sendRequest(uri).getSeats();
	}
	
	public ResResult getTickets(String ip, String to, String id, Date when, String transactionId, int seatsCount,
			String places) throws Error {
		URI uri = UriComponentsBuilder.fromUriString(getHost(ip))
				.queryParam("Action", GET_TICKETS)
				.queryParam("postid", Config.getOrganisation())
				.queryParam("to", to)
				.queryParam("when", dateFormat.format(when))
				.queryParam("id", id)
				.queryParam("resid", id)
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
	
	private Response sendRequest(URI uri) throws Error {
		Response response = template.getForObject(uri, Response.class);
		if (response.getError() != null) {
			throw response.getError();
		} else {
			return response;
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
        return new BASE64Encoder().encode(md.digest());
    }
	
}
