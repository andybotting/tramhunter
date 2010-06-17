package com.andybotting.tramhunter;


public class Route { 
	
	private int id;
	private String number;
	private Destination destinationUp;
	private Destination destinationDown;
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Destination getDestinationUp() {
		return destinationUp;
	}

	public void setDestinationUp(Destination destinationUp) {
		this.destinationUp = destinationUp;
	}

	public Destination getDestinationDown() {
		return destinationDown;
	}

	public void setDestinationDown(Destination destinationDown) {
		this.destinationDown = destinationDown;
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
