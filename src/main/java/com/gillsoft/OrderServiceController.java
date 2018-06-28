package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.Accepted;
import com.gillsoft.client.Confirmed;
import com.gillsoft.client.Error;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.ResResult;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.ServiceIdModel;
import com.gillsoft.client.TicketType;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.CalcType;
import com.gillsoft.model.Commission;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Customer;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Seat;
import com.gillsoft.model.Segment;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.ValueType;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;
import com.gillsoft.util.StringUtil;

@RestController
public class OrderServiceController extends AbstractOrderService {

	@Override
	public OrderResponse addServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse bookingResponse(String orderId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse createResponse(OrderRequest request) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setCustomers(request.getCustomers());
		
		// копия для определения пассажиров
		List<ServiceItem> items = new ArrayList<>();
		items.addAll(request.getServices());
		
		Map<String, Organisation> organisations = new HashMap<>();
		Map<String, Locality> localities = new HashMap<>();
		Map<String, Segment> segments = new HashMap<>();
		List<ServiceItem> resultItems = new ArrayList<>();
		
		// список билетов
		OrderIdModel orderId = new OrderIdModel();
		
		for (Entry<String, List<ServiceItem>> order : getTripItems(request).entrySet()) {
			String[] params = order.getKey().split(";");
			String transactionId = StringUtil.generateUUID();
			TripIdModel model = new TripIdModel().create(params[0]);
			try {
				// получаем данные по билету в ресурсе
				ResResult result = RestClient.getInstance().getTickets(
						model.getIp(), model.getToId(), model.getTripId(), model.getDate(),
						transactionId, order.getValue().size(), getSeats(order.getValue()));
				if (result.getState() == 0) {
					Error error = new Error();
					error.setName(result.getReason());
					throw error;
				}
				// добавляем ид заказа
				ServiceIdModel orderIdmodel = new ServiceIdModel(model.getIp(), transactionId, null);
				orderIdmodel.setTicketNumbers(new ArrayList<>(result.getTickets().getTicket().size()));
				
				// формируем билеты
				for (TicketType ticket : result.getTickets().getTicket()) {
					orderIdmodel.getTicketNumbers().add(ticket.getNo());
					
					ServiceItem item = new ServiceItem();
					item.setId(new ServiceIdModel(model.getIp(), transactionId, ticket.getNo()).asString());
					item.setNumber(ticket.getNo());
					
					// пассажир
					item.setCustomer(new Customer(
							getTicketCustomer(items, ticket.getPlace().getNumber())));
					
					// рейс
					item.setSegment(addSegment(model, organisations, localities, segments, ticket));
					
					// место
					item.setSeat(createSeat(ticket));
					
					// стоимость
					item.setPrice(createPrice(ticket));
					
					resultItems.add(item);
				}
				orderId.getServices().add(orderIdmodel);
			} catch (Error e) {
				for (ServiceItem item : order.getValue()) {
					item.setError(new RestError(e.getMessage()));
					resultItems.add(item);
				}
			}
		}
		response.setOrderId(orderId.asString());
		response.setCustomers(request.getCustomers());
		response.setLocalities(localities);
		response.setOrganisations(organisations);
		response.setSegments(segments);
		response.setServices(resultItems);
		return response;
	}
	
	private String getSeats(List<ServiceItem> items) {
		StringBuilder sb = new StringBuilder();
		for (ServiceItem item : items) {
			if (item.getSeat().getId() != null) {
				sb.append(item.getSeat().getId()).append(",");
			}
		}
		return sb.toString().replaceFirst(",$", "");
	}
	
	private Seat createSeat(TicketType ticket) {
		Seat seat = new Seat();
		seat.setId(ticket.getPlace().getNumber());
		seat.setNumber(ticket.getPlace().getNumber());
		return seat;
	}
	
	private Price createPrice(TicketType ticket) {
		Price price = new Price();
		price.setCurrency(Currency.UAH);
		price.setAmount(ticket.getPrice().getCash());
		price.setVat(ticket.getNds().getCash());
		
		// считаем тариф
		price.setTariff(createTariff(ticket.getTariffs().getTariff()));
		
		// добавляем комиссии
		price.setCommissions(createCommissions(ticket.getTariffs().getTariff()));
		
		return price;
	}
	
	private List<Commission> createCommissions(List<TicketType.Tariffs.Tariff> tariffs) {
		List<Commission> commissions = new ArrayList<>();
		for (TicketType.Tariffs.Tariff tariff : tariffs) {
			if (!checkTariff(tariff)) {
				Commission commission = new Commission();
				commission.setCode(String.valueOf(tariff.getCode()));
				commission.setName(tariff.getText());
				commission.setValue(tariff.getCash());
				commission.setType(ValueType.FIXED);
				commission.setValueCalcType(CalcType.OUT);
				commissions.add(commission);
			}
		}
		return commissions;
	}
	
	private Tariff createTariff(List<TicketType.Tariffs.Tariff> tariffs) {
		Tariff priceTariff = new Tariff();
		priceTariff.setValue(BigDecimal.ZERO);
		priceTariff.setCode("");
		priceTariff.setName("");
		for (TicketType.Tariffs.Tariff tariff : tariffs) {
			if (checkTariff(tariff)) {
				priceTariff.setValue(priceTariff.getValue().add(tariff.getCash()));
				priceTariff.setCode(priceTariff.getCode().concat(",").concat(
						String.valueOf(tariff.getCode())));
				priceTariff.setName(priceTariff.getName().concat(",").concat(
						String.valueOf(tariff.getText())));
			}
		}
		priceTariff.setName(priceTariff.getName().replaceFirst("^,", ""));
		if (priceTariff.getCode() != null) {
			priceTariff.setCode(priceTariff.getCode().replaceFirst("^,", ""));
		}
		return priceTariff;
	}
	
	private boolean checkTariff(TicketType.Tariffs.Tariff tariff) {
		return (tariff.getCode() == null
				&& (Objects.equals(tariff.getText(), RestClient.TARIFF_1_NAME)
						|| Objects.equals(tariff.getText(), RestClient.TARIFF_2_NAME)))
				|| (tariff.getCode() != null
						&& (tariff.getCode() == RestClient.TARIFF_1_CODE
								|| tariff.getCode() == RestClient.TARIFF_2_CODE));
	}
	
	private Segment addSegment(TripIdModel model, Map<String, Organisation> organisations,
			Map<String, Locality> localities, Map<String, Segment> segments, TicketType ticket) {
		String segmentId = model.asString();
		Segment segment = segments.get(segmentId);
		if (segment == null) {
			segment = new Segment();
			
			setSegmentFields(segment, ticket);
			
			// станции
			segment.setDeparture(SearchServiceController.addStation(localities, model.getFromId()));
			segment.setArrival(SearchServiceController.addStation(localities,
					String.join(";", model.getFromId(), model.getToId())));
			
			// перевозчик
			segment.setCarrier(addOrganisation(organisations,
					ticket.getCarrier().getBrand(), ticket.getCarrier().getAddress(), ticket.getCarrier().getPhone()));
			// страховая
			segment.setInsurance(addOrganisation(organisations,
					ticket.getInsurance().getBrand(), ticket.getInsurance().getAddress(), ticket.getInsurance().getPhone()));
			
			segments.put(segmentId, segment);
		}
		Segment result = new Segment();
		result.setId(segmentId);
		return result;
	}
	
	private Organisation addOrganisation(Map<String, Organisation> organisations, String name, String address,
			String phone) {
		String key = StringUtil.md5(String.join(";", name, address, phone));
		Organisation organisation = organisations.get(key);
		if (organisation == null) {
			organisation = new Organisation();
			organisation.setName(Lang.UA, name);
			organisation.setAddress(Lang.UA, address);
			organisation.setPhones(Arrays.asList(phone));
			organisations.put(key, organisation);
		}
		return new Organisation(key);
	}
	
	private void setSegmentFields(Segment segment, TicketType ticket) {
		
		// рейс
		segment.setNumber(ticket.getRace().getCode());
		
		// даты
		SearchServiceController.addDates(segment,
				String.join(" ", ticket.getDeparture().getDate(), ticket.getDeparture().getTime()),
				ticket.getArrival().getTime());
	}
	
	private String getTicketCustomer(List<ServiceItem> items, String seatId) {
		
		// если места можно выбрать
		for (ServiceItem item : items) {
			if (item.getSeat() != null
					&& Objects.equals(item.getSeat().getId(), seatId)) {
				items.remove(item);
				return item.getCustomer().getId();
			}
		}
		// если места не нашли, то берем с пустым
		for (ServiceItem item : items) {
			if (item.getSeat() == null
					|| item.getSeat().getId() == null) {
				items.remove(item);
				return item.getCustomer().getId();
			}
		}
		// если и пустых нет, то первое по списку
		return items.remove(0).getCustomer().getId();
	}
	
	/*
	 * В заказе ресурса на некоторых станциях можно оформить максимум 6 пассажиров в одном заказе.
	 */
	private Map<String, List<ServiceItem>> getTripItems(OrderRequest request) {
		Map<String, List<ServiceItem>> trips = new HashMap<>();
		for (ServiceItem item : request.getServices()) {
			String tripId = item.getSegment().getId();
			List<ServiceItem> items = trips.get(tripId);
			if (items == null) {
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			if (items.size() == 6) {
				trips.put(String.join(";", tripId, StringUtil.generateUUID()), trips.get(tripId));
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			items.add(item);
		}
		return trips;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse getResponse(String orderId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse getServiceResponse(String serviceId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		List<ServiceItem> resultItems = new ArrayList<>();
		
		// преобразовываем ид заказа в объкт
		OrderIdModel orderIdModel = new OrderIdModel().create(orderId);
		
		// выкупаем заказы и формируем ответ
		for (ServiceIdModel service : orderIdModel.getServices()) {
			try {
				Accepted accepted = RestClient.getInstance().confirmTickets(
						service.getIp(), service.getTransactionId(), RestClient.SUCCESS_STATUS);
				if (!Objects.equals(accepted.getPaystatus(), RestClient.CONFIRMED)) {
					Error error = new Error();
					error.setName("Pay confirm status: ".concat(accepted.getPaystatus()));
					throw error;
				} else {
					addServiceItems(resultItems, service, true, null);
				}
			} catch (Exception e) {
				addServiceItems(resultItems, service, false, new RestError(e.getMessage()));
			}
		}
		response.setOrderId(orderId);
		response.setServices(resultItems);
		return response;
	}
	
	private void addServiceItems(List<ServiceItem> resultItems, ServiceIdModel service, boolean confirmed, RestError error) {
		for (String ticketNumber : service.getTicketNumbers()) {
			ServiceItem serviceItem = new ServiceItem();
			serviceItem.setId(new ServiceIdModel(service.getIp(), service.getTransactionId(), ticketNumber).asString());
			serviceItem.setConfirmed(confirmed);
			serviceItem.setError(error);
			resultItems.add(serviceItem);
		}
	}

	@Override
	public OrderResponse removeServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			ServiceIdModel model = new ServiceIdModel().create(serviceItem.getId());
			try {
				Confirmed confirmed = RestClient.getInstance().confirmReturn(
						model.getIp(), model.getTicketNumber(), BigDecimal.ONE);
				serviceItem.setConfirmed(confirmed != null);
			} catch (Error e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse updateCustomersResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			ServiceIdModel model = new ServiceIdModel().create(serviceItem.getId());
			try {
				TicketType info = RestClient.getInstance().getReturnInfo(model.getIp(), model.getTicketNumber());
				Price price = new Price();
				price.setCurrency(Currency.UAH);
				price.setAmount(info.getMoney().getCash());
				
				// считаем тариф
				Tariff tariff = createTariff(info.getTariffs().getTariff());
				price.setTariff(tariff);
				
				ReturnCondition condition = new ReturnCondition();
				condition.setDescription(info.getRefund().getInfo());
				
				tariff.setReturnConditions(new ArrayList<>(1));
				tariff.getReturnConditions().add(condition);
				
				// добавляем комиссии
				price.setCommissions(createCommissions(info.getTariffs().getTariff()));
				
				serviceItem.setPrice(price);
			} catch (Error e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

}
