package com.andybotting.tramhunter;


public class Destination { 
	private long id;
	private String routeNumber;
	private String destination;
	private Boolean up;

	public long getId() {
		return id;
	}

	public void setId(long _id) {
		this.id = _id;
	}
	
	public void setRouteNumber(String _routeNumber) {
		routeNumber = _routeNumber;
	}
	public String getRouteNumber() {
		return routeNumber;
	} 
	
	
	public void setDestination(String _destination) {
		destination = _destination;
	}
	public String getDestination() {
		return destination;
	}	

	
	// Set 'up' via int from database
	public void setUp(int _up) {
		up = false;
		if (_up == 1) {
			up = true;
		}		
	}
	
	// Set 'up' from boolean
	public void setUp(boolean _up) {
		up = _up;
	}
	
	public boolean getUp() {
		return up;
	}	

	
	public String toString() {
		return "Destination: " + destination;
	}

}
