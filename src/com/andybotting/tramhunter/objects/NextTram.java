package com.andybotting.tramhunter.objects;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NextTram implements Comparable<NextTram> { 
	
	private Stop originStop;
	private int internalRouteNo;
	private String routeNo;
	private String headboardRouteNo;
	private int vehicleNo;
	private String tramClass;
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
	private List<Stop> favouritesOnRoute;
	
	public Stop getOriginStop() {
		return originStop;
	}

	public void setOriginStop(Stop originStop) {
		this.originStop = originStop;
	}

	// Internal Route Number
	public void setInternalRouteNo(int _internalRouteNo) {
		internalRouteNo = _internalRouteNo;
	}
	
	public int getInternalRouteNo() {
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
	public void setVehicleNo(int _vehicleNo) {
		vehicleNo = _vehicleNo;
	}
	public int getVehicleNo() {
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
	public void setHasDisruption(Boolean _hasDisruption) {
		hasDisruption = _hasDisruption;
	}
	
	public boolean getHasDisruption() {
	   return hasDisruption;
	}	

	// isTTAvailable
	public void setIsTTAvailable(Boolean _isTTAvailable) {
		isTTAvailable = _isTTAvailable;
	}
	
	public boolean getIsTTAvailable() {
	   return isTTAvailable;
	}   
	
	// isLowFloorTram
	public void setIsLowFloorTram(Boolean _isLowFloorTram) {
		isLowFloorTram = _isLowFloorTram;
	}
	
	public boolean getIsLowFloorTram() {
	   return isLowFloorTram;
	}	  
	
	// airConditioned
	public void setAirConditioned(Boolean _airConditioned) {
		airConditioned = _airConditioned;
	}
	
	public boolean getAirConditioned() {
	   return airConditioned;
	}	 
	
	
	// displayAC
	public void setDisplayAC(Boolean _displayAC) {
		displayAC = _displayAC;
	}
	
	public boolean getDisplayAC() {
	   return displayAC;
	}	   
	
	
	// hasSpecialEvent
	public void setHasSpecialEvent(Boolean _hasSpecialEvent) {
		hasSpecialEvent = _hasSpecialEvent;
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
	public void setPredictedArrivalDateTime(Date _predictedArrivalDateTime) {
		predictedArrivalDateTime = _predictedArrivalDateTime;
	}
	 public Date getPredictedArrivalDateTime() {
		 return predictedArrivalDateTime;
	}	
	
	// requestDateTime
	public void setRequestDateTime(Date _requestDateTime) {
		requestDateTime = _requestDateTime;
	}
	
	public Date getRequestDateTime() {
		return requestDateTime;
	}  	
	
	
	// tramClass
	public void setTramClass(String _tramClass) {
		tramClass = _tramClass;
	}
	public String getTramClass() {
		return tramClass;
	}	
	
	
	public List<Stop> getFavouritesOnRoute() {
		return favouritesOnRoute;
	}

	public void setFavouritesOnRoute(List<Stop> favouritesOnRoute) {
		this.favouritesOnRoute = favouritesOnRoute;
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
	
	public int compareTo(NextTram otherTram) {

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
		return buffer.toString();
	}



}
