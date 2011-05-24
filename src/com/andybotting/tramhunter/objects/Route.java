package com.andybotting.tramhunter.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author andy
 *
 */
public class Route { 
	
	private int id;
	private String number;
	private Destination destinationUp = null;
	private Destination destinationDown = null;
	
	/**
	 * 
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * 
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getNumber() {
		return number;
	}
	
	/**
	 * 
	 * @param number
	 */
	public void setNumber(String number) {
		this.number = number;
	}
	
	/**
	 * 
	 * @return
	 */
	public Destination getDestinationUp() {
		return destinationUp;
	}
	
	/**
	 * 
	 * @param destinationUp
	 */
	public void setDestinationUp(Destination destinationUp) {
		this.destinationUp = destinationUp;
	}

	/**
	 * 
	 * @return
	 */
	public Destination getDestinationDown() {
		return destinationDown;
	}
	
	/**
	 * 
	 * @param destinationDown
	 */
	public void setDestinationDown(Destination destinationDown) {
		this.destinationDown = destinationDown;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Destination> getDestinations() {
		
		List<Destination> destinations = new ArrayList<Destination>();
		
		if (destinationUp != null)
			destinations.add(destinationUp);
		
		if (destinationDown != null)
			destinations.add(destinationDown);
		
		return destinations;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasDestinationUp(){
		if (this.destinationUp != null) 
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasDestinationDown(){
		if (this.destinationDown != null) 
			return true;
		else
			return false;
	}	

	/**
	 * 
	 * @return
	 */
	public String getDestinationString() {
		
		String destinationString = "";
		
		Destination up = getDestinationUp();
		Destination down = getDestinationDown();
		
		if ( (up != null) && (down != null) ) {
			destinationString = getDestinationUp().getDestination() 
				+ " to " 
				+ getDestinationDown().getDestination();
		}
		else {
			destinationString = "To ";

			if (up != null)
				destinationString += getDestinationUp().getDestination();
			
			if (down != null)
				destinationString += getDestinationDown().getDestination();
				
		}
		
		return destinationString;
	}
	
	/**
	 * 
	 */
	public String toString() {
		return "Route " + number;
	}

}
