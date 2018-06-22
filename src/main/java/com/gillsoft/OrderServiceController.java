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
import com.gillsoft.client.Error;
import com.gillsoft.client.ResResult;
import com.gillsoft.client.ResResult.Tickets;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.CalcType;
import com.gillsoft.model.Commission;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Customer;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RestError;
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
	public OrderResponse addTicketsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse bookResponse(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse cancelResponse(String id) {
		// TODO Auto-generated method stub
		return null;
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
				// формируем билеты
				for (Tickets.Ticket ticket : result.getTickets().getTicket()) {
					ServiceItem item = new ServiceItem();
					item.setId(String.join(";", params[0], transactionId, ticket.getNo())); //TODO
					item.setNumber(ticket.getNo());
					
					// пассажир
					item.setCustomer(new Customer(
							getTicketCustomer(items, ticket.getPlace().getNumber())));
					
					// рейс
					item.setSegment(addSegment(params[0], organisations, localities, segments, ticket));
					
					// место
					item.setSeat(createSeat(ticket));
					
					// стоимость
					item.setPrice(createPrice(ticket));
					
					resultItems.add(item);
				}
			} catch (Error e) {
				for (ServiceItem item : order.getValue()) {
					item.setError(new RestError(e.getMessage()));
					resultItems.add(item);
				}
			}
		}
		response.setCustomers(request.getCustomers());
		response.setLocalities(localities);
		response.setOrganisations(organisations);
		response.setSegments(segments);
		response.setServices(resultItems);
		
		//TODO response orderid
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
	
	private Seat createSeat(Tickets.Ticket ticket) {
		Seat seat = new Seat();
		seat.setId(ticket.getPlace().getNumber());
		seat.setNumber(ticket.getPlace().getNumber());
		return seat;
	}
	
	private Price createPrice(Tickets.Ticket ticket) {
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
	
	private List<Commission> createCommissions(List<Tickets.Ticket.Tariffs.Tariff> tariffs) {
		List<Commission> commissions = new ArrayList<>();
		for (Tickets.Ticket.Tariffs.Tariff tariff : tariffs) {
			if (tariff.getCode() != RestClient.TARIFF_1_CODE
					&& tariff.getCode() != RestClient.TARIFF_2_CODE) {
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
	
	private Tariff createTariff(List<Tickets.Ticket.Tariffs.Tariff> tariffs) {
		Tariff priceTariff = new Tariff();
		priceTariff.setValue(BigDecimal.ZERO);
		priceTariff.setCode("");
		priceTariff.setName("");
		for (Tickets.Ticket.Tariffs.Tariff tariff : tariffs) {
			if (tariff.getCode() == RestClient.TARIFF_1_CODE
					|| tariff.getCode() == RestClient.TARIFF_2_CODE) {
				priceTariff.setValue(priceTariff.getValue().add(tariff.getCash()));
				priceTariff.setCode(priceTariff.getCode().concat(",").concat(
						String.valueOf(tariff.getCode())));
				priceTariff.setName(priceTariff.getName().concat(",").concat(
						String.valueOf(tariff.getText())));
			}
		}
		priceTariff.setName(priceTariff.getName().replaceFirst("^,", ""));
		priceTariff.setCode(priceTariff.getCode().replaceFirst("^,", ""));
		return priceTariff;
	}
	
	private Segment addSegment(String id, Map<String, Organisation> organisations, Map<String, Locality> localities,
			Map<String, Segment> segments, Tickets.Ticket ticket) {
		Segment segment = segments.get(id);
		if (segment == null) {
			segment = new Segment();
			
			setSegmentFields(segment, ticket);
			
			// станции
			segment.setDeparture(SearchServiceController.addStation(localities, null, ticket.getFrom().getText()));
			segment.setArrival(SearchServiceController.addStation(localities, null, ticket.getTo().getText()));
			
			// перевозчик
			segment.setCarrier(addOrganisation(organisations,
					ticket.getCarrier().getBrand(), ticket.getCarrier().getAddress(), ticket.getCarrier().getPhone()));
			// страховая
			segment.setInsurance(addOrganisation(organisations,
					ticket.getInsurance().getBrand(), ticket.getInsurance().getAddress(), ticket.getInsurance().getPhone()));
			
			segments.put(id, segment);
		}
		Segment result = new Segment();
		result.setId(id);
		return result;
	}
	
	private Organisation addOrganisation(Map<String, Organisation> organisations, String name, String address,
			String phone) {
		String key = StringUtil.md5(String.join(";", name, address, phone));
		Organisation organisation = organisations.get(key);
		if (organisation == null) {
			organisation = new Organisation();
			organisation.setName(name);
			organisation.setAddress(address);
			organisation.setPhones(Arrays.asList(phone));
			organisations.put(key, organisation);
		}
		return new Organisation(key);
	}
	
	private void setSegmentFields(Segment segment, Tickets.Ticket ticket) {
		
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
	public OrderResponse getPdfTicketsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse getResponse(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse getTicketResponse(String ticketId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse payResponse(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse removeTicketsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse returnTicketsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse updatePassengersResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
