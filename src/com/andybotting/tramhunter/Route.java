package com.andybotting.tramhunter;


public class Route { 
	
	private String number;
	private Destination destinationUp;
	private Destination destinationDown;
	
	public void setNumber(String _number) {
		number = _number;
	}
	public String getNumber() {
		return number;
	} 
		
	public void setDestinationUp(Destination _destination) {
		destinationUp = _destination;
	}
	public Destination getDestinationUp() {
		return destinationUp;
	}	

	public void setDestinationDown(Destination _destination) {
		destinationDown = _destination;
	}
	public Destination getDestinationDown() {
		return destinationDown;
	}	
	
	public String getDestinationString() {
		return getDestinationUp().getDestination() 
			+ " to " 
			+ getDestinationDown().getDestination();
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Route ");
		buffer.append(number);
		return buffer.toString();
	}

}
