package com.gillsoft.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "data")
public class Response {

	private Accepted accepted;
	private Error error;
	private Information information;
	private ResResult resResult;
	private Salepoints salepoints;
	private Schedule schedule;
	private Seats seats;
	private Stations stations;
	private TripPackage tripPackage;

	public Accepted getAccepted() {
		return accepted;
	}

	public Error getError() {
		return error;
	}

	public Information getInformation() {
		return information;
	}

	public ResResult getResResult() {
		return resResult;
	}

	public Salepoints getSalepoints() {
		return salepoints;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public Seats getSeats() {
		return seats;
	}

	public Stations getStations() {
		return stations;
	}

	public TripPackage getTripPackage() {
		return tripPackage;
	}

}
