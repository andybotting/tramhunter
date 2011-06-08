/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This file is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 * General Public License for more details.  
 *  
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:
 * 
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.objects;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
		Date now = getRequestDateTime();
		Date arr = getPredictedArrivalDateTime();
		
		if (minutes < 0) {
			// Anything less than 0 is an error
			return "Err";
		}
		else if (minutes < 1) {
			// Less than 1 minute
			return "Now";
		}
		else if (minutes > 600) {
			// Tue 6:54 AM
			SimpleDateFormat dateFormat = new SimpleDateFormat("EE h:mm a");
			dateFormat.setTimeZone(TimeZone.getTimeZone("Australia/Melbourne"));
			return dateFormat.format(arr);
		}
		else if (minutes > 90) {
			// 6:54 AM
			SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
			dateFormat.setTimeZone(TimeZone.getTimeZone("Australia/Melbourne"));
			return dateFormat.format(arr);
		}
		else {
			// 2 (just minutes)
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
