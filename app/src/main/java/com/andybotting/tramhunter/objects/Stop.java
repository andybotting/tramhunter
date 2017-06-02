/*  
 * Copyright 2013 Andy Botting <andy@andybotting.com>
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

import android.location.Location;

import com.google.android.maps.GeoPoint;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Stop { 
	
	private long id = -1;
	private int tramTrackerID = -1;
	private String flagStopNumber;
	private String primaryName;
	private String secondaryName;
	private String routesString;
	private String cityDirection;
	private Location location;
	private double latitude;
	private double longitude;
	private List<Route> routes;
	
	private static final String NAME_PATTERN = "(.+) & (.+)";
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Stop) {
			Stop oStop = (Stop)o;
			return this.getTramTrackerID()==oStop.getTramTrackerID();
		}
		else {
			return false;
		}
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	public void setTramTrackerID(int tramTrackerID) {
		this.tramTrackerID = tramTrackerID;
	}
	
	
	public int getTramTrackerID() {
		return tramTrackerID;
	} 
	

	public void setFlagStopNumber(String flagStopNumber) {
		this.flagStopNumber = flagStopNumber;
	}
	
	
	public String getFlagStopNumber() {
		return flagStopNumber;
	}	

	
	public void setStopName(String _stopName) {
		
		Pattern pattern = Pattern.compile(NAME_PATTERN);
		Matcher matcher = pattern.matcher(_stopName);
		
		if (matcher.find()) {
			primaryName = matcher.group(1);
			secondaryName = matcher.group(2);
		}
	}

	
	public String getStopName() {
	   String stopName = primaryName;
	   
	   if (secondaryName != null) {
		   stopName += " & " + secondaryName;
	   }
	   
	   return stopName;
	}	

	
	public void setPrimaryName(String primaryName) {
		this.primaryName = primaryName;
	}
		
	public String getPrimaryName() {
		return primaryName;
	}
	
	public void setSecondaryName(String secondaryName) {
		if (secondaryName != null)
			if (secondaryName.length() < 1)
				secondaryName = null;
		this.secondaryName = secondaryName;
	}
		
	public String getSecondaryName() {
		return secondaryName;
	}

	public void setRoutesString(String routesString) {
		this.routesString = routesString;
	}

	public String getRoutesString() {
		return routesString;
	}

	public void setCityDirection(String cityDirection) {
		this.cityDirection = cityDirection;
	}

	public String getCityDirection() {
		return cityDirection;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;

	}
	
	public void setLocation(Location location) {
		 this.location = location;
	}
	
	public Location getLocation() {
		if (location != null) {
			return location;
		}
		else {
			location = new Location("dummy");
			location.setLongitude(longitude);
			location.setLatitude(latitude);
			return location;

		}
	}	
	
	
	/**
	 * @return a float representing the distance between the stop and
	 * a given location
	 */
	public double distanceTo(Location location) {
		double distance = this.getLocation().distanceTo(location);
		return distance;
	}
	
    public String formatDistanceTo(Location location){
    	
    	double distance = this.distanceTo(location);

    	String result = "0m";
    	
    	if(distance > 10000) {
    		// More than 10kms
    		distance = distance / 1000;
    		result = (int)distance + "km";
    	}
    	else if(distance > 999) {
    		distance = distance / 1000;
    		result = roundToDecimals(distance, 1) + "km";
    	}
    	else {
    		result = (int)distance + "m";
    	}
    	
    	return result;
    }
	
	private static double roundToDecimals(double value, int decimalPlaces) {
    	int intValue = (int)((value * Math.pow(10, decimalPlaces)));
    	return (((double)intValue) / Math.pow(10, decimalPlaces));
    }
	
	/**
	 * @return the GeoPoint of the stop
	 */
	public GeoPoint getGeoPoint() {
        int lat1E6 = (int) (latitude * 1E6);
        int lng1E6 = (int) (longitude * 1E6);
        GeoPoint point = new GeoPoint(lat1E6, lng1E6);
        return point;
	} 
	
	
	public void setRoutes(List<Route> routes) {
		routes = this.routes;
	}
	
	
	public List<Route> getRoutes() {
	   return routes;
	}   	
	
	/**
	 * Return a friendly formatted routes list for stop lists
	 */
	public String getRoutesListString() {
		String routesString = "";

		if (routes != null && routes.size() > 0) {

			if (routes.size() < 2)
				routesString = "Route ";
			else
				routesString = "Routes ";

			for (int i = 0; i < routes.size(); i++) {
				Route r = routes.get(i);
				routesString += r.getNumber();

				if (i < routes.size() - 2)
					routesString += ", ";
				else if (i == routes.size() - 2)
					routesString += " and ";
			}
		}

		return routesString;
	}

	/**
	 * Generate a useful description string to show in lists to supplement the
	 * stops primary name
	 */
	public String getStopDetailsLine() {

		// Stop 4: Swanston St - towards City (3004)
		// Stop 8 - Towards Fed. Square (3008)

		String line = "Stop " + flagStopNumber;

		// If the stop has a secondary name, add it
		if (secondaryName != null)
			line += ": " + secondaryName;

		line += " - " + cityDirection + " (" + tramTrackerID + ")";

		return line;
	}

	/**
	 * @return String representing the stop
	 */
	public String toString() {
		return ("Stop " + tramTrackerID + " (" + getPrimaryName() + ")");
	}

	
}
