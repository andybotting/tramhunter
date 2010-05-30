package com.andybotting.tramhunter;

import java.util.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.http.impl.cookie.DateParseException;

import android.util.Log;


public class NextTram implements Comparable { 
	
	private String internalRouteNo;
	private String routeNo;
	private String headboardRouteNo;
	private String vehicleNo;
	private String destination;
	private Boolean hasDisruption;
	private Boolean isTTAvailable;
	private Boolean isLowFloorTram;
	private Boolean airConditioned;
	private Boolean displayAC;
	private Boolean hasSpecialEvent;
	private String specialEventMessage;
	private Date predictedArrivalDateTime;
	private Date requestDateTime;


	
	// Internal Route Number
	public void setInternalRouteNo(String _internalRouteNo) {
		internalRouteNo = _internalRouteNo;
	}
	
	public String getInternalRouteNo() {
		return internalRouteNo;
	} 
	
	
	// Route Number
	public void setRouteNo(String _routeNo) {
		routeNo = _routeNo;
	}
	public String getRouteNo() {
		return routeNo; 
	} 

	
	// headboardRouteNo
	public void setHeadboardRouteNo(String _headboardRouteNo) {
		headboardRouteNo = _headboardRouteNo;
	}
	public String getHeadboardRouteNo() {
		return headboardRouteNo;
	}	 
	
	
	// vehicleNo
	public void setVehicleNo(String _vehicleNo) {
		vehicleNo = _vehicleNo;
	}
	public String getVehicleNo() {
		return vehicleNo;
	} 
	
	
	// destination
	public void setDestination(String _destination) {
		destination = _destination;
	}
	public String getDestination() {
		return destination;
	}	

	
	// hasDisruption
	public void setHasDisruption(String _hasDisruption) {
		hasDisruption = Boolean.parseBoolean(_hasDisruption);
	}
	
	public boolean getHasDisruption() {
	   return hasDisruption;
	}	

	// isTTAvailable
	public void setIsTTAvailable(String _isTTAvailable) {
		isTTAvailable = Boolean.parseBoolean(_isTTAvailable);
	}
	
	public boolean getIsTTAvailable() {
	   return isTTAvailable;
	}   
	
	// isLowFloorTram
	public void setIsLowFloorTram(String _isLowFloorTram) {
		isLowFloorTram = Boolean.parseBoolean(_isLowFloorTram);
	}
	
	public boolean getIsLowFloorTram() {
	   return isLowFloorTram;
	}	  
	
	// airConditioned
	public void setAirConditioned(String _airConditioned) {
		airConditioned = Boolean.parseBoolean(_airConditioned);
	}
	
	public boolean getAirConditioned() {
	   return airConditioned;
	}	 
	
	
	// displayAC
	public void setDisplayAC(String _displayAC) {
		displayAC = Boolean.parseBoolean(_displayAC);
	}
	
	public boolean getDisplayAC() {
	   return displayAC;
	}	   
	
	
	// hasSpecialEvent
	public void setHasSpecialEvent(String _hasSpecialEvent) {
		hasSpecialEvent = Boolean.parseBoolean(_hasSpecialEvent);
	}
	
	public boolean getHasSpecialEvent() {
	   return hasSpecialEvent;
	}		
	
	
	// specialEventMessage
	public void setSpecialEventMessage(String _specialEventMessage) {
		specialEventMessage = _specialEventMessage;
	}
	public String getSpecialEventMessage() {
		  return specialEventMessage;
	}	   
	
	
	// predictedArrivalDateTime
	public void setPredictedArrivalDateTime(String _predictedArrivalDateTime) {
		predictedArrivalDateTime = parseDate(_predictedArrivalDateTime);
	}
	 public Date getPredictedArrivalDateTime() {
		 return predictedArrivalDateTime;
	}	
	
	// requestDateTime
	public void setRequestDateTime(String _requestDateTime) {
		requestDateTime = parseDate(_requestDateTime);
	}
	public Date getRequestDateTime() {
		return requestDateTime;
	}  	 
	 
	
	// Parse the dates
	public Date parseDate(String dateString) {
		DateFormat df = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ssZ");
		Date date = new Date();
		
		//<PredictedArrivalDateTime>2010-05-30T19:00:48+10:00</PredictedArrivalDateTime>
		//<RequestDateTime>2010-05-30T18:59:54.2212858+10:00</RequestDateTime>
		
		try {
			date = df.parse(dateString);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
		}	
		
		return date;
	}
	
	
	// Get minutes away
	public int minutesAway() {
		Date predictedDate = getPredictedArrivalDateTime();
		Date requestDate = getRequestDateTime();
		
		long diff = predictedDate.getTime() - requestDate.getTime(); 
		int minutes = (int)diff/60000;

		return minutes;
	}
	
	
	
	public String humanMinutesAway() {
		
		int minutes = this.minutesAway();
		
		if (minutes < 0) {
			return "Err";
		}
		else if (minutes < 1) {
			return "Now";
		}
		else {
			return Integer.toString(minutes);
		}
	}
	
	
	
	public int compareTo(Object obj) {

		NextTram otherTram = (NextTram)obj;
		
		int thisTramMinutes = this.minutesAway();
		int otherTramMinutes = otherTram.minutesAway();
		
		// Test the difference between this, and the given NextTram obj
		if(thisTramMinutes < otherTramMinutes) {
			return -1;
		} 
		else if(thisTramMinutes > otherTramMinutes) {
			return 1;
		}
		return 0; 
	}
	
	
	

	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
			buffer.append(routeNo + " " + destination + ": " + minutesAway());
//		buffer.append("routeNo: ");
//		buffer.append(routeNo);
//		buffer.append("\n");
//		buffer.append("destination: ");
//		buffer.append(destination);
//		buffer.append("\n");
//		buffer.append("hasDisruption: ");
//		buffer.append(hasDisruption);
//		buffer.append("\n");
//		buffer.append("minutesAway: ");
//		buffer.append(minutesAway());
//		buffer.append("\n");
//		buffer.append("hasSpecialEvent: ");
//		buffer.append(hasSpecialEvent);
//		buffer.append("\n");
//		buffer.append("specialEventMessage: ");
//		buffer.append(specialEventMessage);
//		buffer.append("\n");
//		buffer.append("predictedArrivalDateTime: ");
//		buffer.append(predictedArrivalDateTime.toString());
//		buffer.append("\n");
//		buffer.append("requestDateTime: ");
//		buffer.append(requestDateTime.toString());
//		buffer.append("\n");
		return buffer.toString();
	}



}
