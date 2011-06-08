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
