package com.andybotting.tramhunter;


public class Route { 
	
	private String number;
	private String destination;
	private Boolean up;

	
	public void setNumber(String _number) {
		number = _number;
	}
	public String getNumber() {
		return number;
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
		StringBuffer buffer = new StringBuffer();
		buffer.append("Route ");
		buffer.append(number);
		buffer.append(":");
		buffer.append(destination);
		return buffer.toString();
	}

}
