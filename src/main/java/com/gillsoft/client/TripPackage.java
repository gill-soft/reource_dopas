//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.06.13 at 09:39:45 AM EEST 
//


package com.gillsoft.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="trips">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="trip" maxOccurs="unbounded" minOccurs="0">
 *                     &lt;complexType>
 *                       &lt;simpleContent>
 *                         &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *                           &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="fist_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="first_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="last_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="last_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="number" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="price" type="{http://www.w3.org/2001/XMLSchema}decimal" />
 *                           &lt;attribute name="from_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="from_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="to_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="to_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="tu_mark" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                           &lt;attribute name="seats" type="{http://www.w3.org/2001/XMLSchema}int" />
 *                         &lt;/extension>
 *                       &lt;/simpleContent>
 *                     &lt;/complexType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "trips"
})
public class TripPackage {

    @XmlElement(required = true)
    protected TripPackage.Trips trips;
    
    private Error error;

    /**
     * Gets the value of the trips property.
     * 
     * @return
     *     possible object is
     *     {@link TripPackage.Trips }
     *     
     */
    public TripPackage.Trips getTrips() {
        return trips;
    }

    /**
     * Sets the value of the trips property.
     * 
     * @param value
     *     allowed object is
     *     {@link TripPackage.Trips }
     *     
     */
    public void setTrips(TripPackage.Trips value) {
        this.trips = value;
    }


    public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}

	/**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="trip" maxOccurs="unbounded" minOccurs="0">
     *           &lt;complexType>
     *             &lt;simpleContent>
     *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
     *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="fist_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="first_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="last_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="last_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="number" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="price" type="{http://www.w3.org/2001/XMLSchema}decimal" />
     *                 &lt;attribute name="from_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="from_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="to_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="to_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="tu_mark" type="{http://www.w3.org/2001/XMLSchema}string" />
     *                 &lt;attribute name="seats" type="{http://www.w3.org/2001/XMLSchema}int" />
     *               &lt;/extension>
     *             &lt;/simpleContent>
     *           &lt;/complexType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "trip"
    })
    public static class Trips {

        protected List<TripPackage.Trips.Trip> trip;

        /**
         * Gets the value of the trip property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the trip property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTrip().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link TripPackage.Trips.Trip }
         * 
         * 
         */
        public List<TripPackage.Trips.Trip> getTrip() {
            if (trip == null) {
                trip = new ArrayList<TripPackage.Trips.Trip>();
            }
            return this.trip;
        }


        /**
         * <p>Java class for anonymous complex type.
         * 
         * <p>The following schema fragment specifies the expected content contained within this class.
         * 
         * <pre>
         * &lt;complexType>
         *   &lt;simpleContent>
         *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
         *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="fist_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="first_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="last_point_name" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="last_point_code" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="number" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="price" type="{http://www.w3.org/2001/XMLSchema}decimal" />
         *       &lt;attribute name="from_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="from_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="to_arrival" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="to_departure" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="tu_mark" type="{http://www.w3.org/2001/XMLSchema}string" />
         *       &lt;attribute name="seats" type="{http://www.w3.org/2001/XMLSchema}int" />
         *     &lt;/extension>
         *   &lt;/simpleContent>
         * &lt;/complexType>
         * </pre>
         * 
         * 
         */
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {
            "value"
        })
        public static class Trip {

            @XmlValue
            protected String value;
            @XmlAttribute(name = "id")
            protected String id;
            @XmlAttribute(name = "fist_point_name")
            protected String fistPointName;
            @XmlAttribute(name = "first_point_code")
            protected String firstPointCode;
            @XmlAttribute(name = "last_point_name")
            protected String lastPointName;
            @XmlAttribute(name = "last_point_code")
            protected String lastPointCode;
            @XmlAttribute(name = "number")
            protected String number;
            @XmlAttribute(name = "price")
            protected BigDecimal price;
            @XmlAttribute(name = "from_arrival")
            protected String fromArrival;
            @XmlAttribute(name = "from_departure")
            protected String fromDeparture;
            @XmlAttribute(name = "to_arrival")
            protected String toArrival;
            @XmlAttribute(name = "to_departure")
            protected String toDeparture;
            @XmlAttribute(name = "tu_mark")
            protected String tuMark;
            @XmlAttribute(name = "seats")
            protected Integer seats;

            /**
             * Gets the value of the value property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getValue() {
                return value;
            }

            /**
             * Sets the value of the value property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setValue(String value) {
                this.value = value;
            }

            /**
             * Gets the value of the id property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getId() {
                return id;
            }

            /**
             * Sets the value of the id property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setId(String value) {
                this.id = value;
            }

            /**
             * Gets the value of the fistPointName property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFistPointName() {
                return fistPointName;
            }

            /**
             * Sets the value of the fistPointName property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFistPointName(String value) {
                this.fistPointName = value;
            }

            /**
             * Gets the value of the firstPointCode property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFirstPointCode() {
                return firstPointCode;
            }

            /**
             * Sets the value of the firstPointCode property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFirstPointCode(String value) {
                this.firstPointCode = value;
            }

            /**
             * Gets the value of the lastPointName property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLastPointName() {
                return lastPointName;
            }

            /**
             * Sets the value of the lastPointName property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLastPointName(String value) {
                this.lastPointName = value;
            }

            /**
             * Gets the value of the lastPointCode property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getLastPointCode() {
                return lastPointCode;
            }

            /**
             * Sets the value of the lastPointCode property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setLastPointCode(String value) {
                this.lastPointCode = value;
            }

            /**
             * Gets the value of the number property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getNumber() {
                return number;
            }

            /**
             * Sets the value of the number property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setNumber(String value) {
                this.number = value;
            }

            /**
             * Gets the value of the price property.
             * 
             * @return
             *     possible object is
             *     {@link BigDecimal }
             *     
             */
            public BigDecimal getPrice() {
                return price;
            }

            /**
             * Sets the value of the price property.
             * 
             * @param value
             *     allowed object is
             *     {@link BigDecimal }
             *     
             */
            public void setPrice(BigDecimal value) {
                this.price = value;
            }

            /**
             * Gets the value of the fromArrival property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFromArrival() {
                return fromArrival;
            }

            /**
             * Sets the value of the fromArrival property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFromArrival(String value) {
                this.fromArrival = value;
            }

            /**
             * Gets the value of the fromDeparture property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getFromDeparture() {
                return fromDeparture;
            }

            /**
             * Sets the value of the fromDeparture property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setFromDeparture(String value) {
                this.fromDeparture = value;
            }

            /**
             * Gets the value of the toArrival property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getToArrival() {
                return toArrival;
            }

            /**
             * Sets the value of the toArrival property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setToArrival(String value) {
                this.toArrival = value;
            }

            /**
             * Gets the value of the toDeparture property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getToDeparture() {
                return toDeparture;
            }

            /**
             * Sets the value of the toDeparture property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setToDeparture(String value) {
                this.toDeparture = value;
            }

            /**
             * Gets the value of the tuMark property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getTuMark() {
                return tuMark;
            }

            /**
             * Sets the value of the tuMark property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setTuMark(String value) {
                this.tuMark = value;
            }

            /**
             * Gets the value of the seats property.
             * 
             * @return
             *     possible object is
             *     {@link Integer }
             *     
             */
            public Integer getSeats() {
                return seats;
            }

            /**
             * Sets the value of the seats property.
             * 
             * @param value
             *     allowed object is
             *     {@link Integer }
             *     
             */
            public void setSeats(Integer value) {
                this.seats = value;
            }

        }

    }

}
