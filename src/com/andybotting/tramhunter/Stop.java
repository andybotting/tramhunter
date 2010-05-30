package com.andybotting.tramhunter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.location.Location;


public class Stop { 
	
	private int id;
	private int tramTrackerID;
	private String flagStopNumber;
	private String primaryName;
	private String secondaryName;
	private String cityDirection;
	private Location location;
	private Float latitude;
	private Float longitude;
	private String suburb;
	private boolean starred = false;

	
	
	private static final String NAME_PATTERN =
		"(.+) & (.+)";
	

	public void setId(int _id) {
		id = _id;
	}
	
	
	/**
	 * @return The id
	 */
	public int getId() {
		return id;
	}
	
	
	public void setTramTrackerID(int _tramTrackerID) {
		tramTrackerID = _tramTrackerID;
	}
	
	
	public int getTramTrackerID() {
		return tramTrackerID;
	} 
	

	public void setFlagStopNumber(String _flagStopNumber) {
		flagStopNumber = _flagStopNumber;
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
	   String stopName = primaryName + " & " + secondaryName;
	   return stopName;
	}	

	
	public void setPrimaryName(String _primaryName) {
			primaryName = _primaryName;
	}
	
	
	public String getPrimaryName() {
			return primaryName;
	}
	
	


	public void setSecondaryName(String _secondaryName) {
			secondaryName = _secondaryName;
	}
	
	
	public String getSecondaryName() {
		return secondaryName;
	}
	
   
	
	public void setCityDirection(String _cityDirection) {
		   cityDirection = _cityDirection;
	}
	
	
	public String getCityDirection() {
		  return cityDirection;
	}	
	
	public void setLatitude(Float _latitude) {
		latitude = _latitude;		
	}
	 
	
	public void setLongitude(Float _longitude) {
		longitude = _longitude;
		
	}
	
	public void setLocation(Location _location) {
		 location = _location;
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
	
	
	public void setSuburb(String _suburb) {
		suburb = _suburb;
	}
	
	
	public String getSuburb() {
	   return suburb;
	}   

	
	/**
	 * @return true if starred
	 */
	public boolean isStarred() {
		return starred;
	}

	
	/**
	 * @param starred true to star
	 */
	public void setStarred(boolean _starred) {
		starred = _starred;
	}
	
	/**
	 * @param starred true to star
	 */
	public void setStarred(int _starred) {
		if (_starred == 1) {
			starred = true;
		}
	}
	
	
	public void toggleStarred() {
		if(starred) {
			starred = false;
		}
		else {
			starred = true;
		}
	}
	
	
	/**
	 * @return String representing the stop
	 */
	public String toString() {
		return ("Stop " + tramTrackerID + ": " + getPrimaryName());
	}

}
