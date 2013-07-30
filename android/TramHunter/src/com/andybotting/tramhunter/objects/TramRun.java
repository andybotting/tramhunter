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

import java.util.ArrayList;
import java.util.List;

public class TramRun {
	
	int vehicleNumber;
    boolean isAtLayover;
    boolean isAvailable;
    Route route;
    Destination destination;
    boolean hasSpecialEvent;
    boolean hasDisruption;
	private List<TramRunTime> times = new ArrayList<TramRunTime>();
	
	/**
	 * 
	 */
	public void setRoute(Route route) {
		this.route = route;
	}
	
	/**
	 * 
	 */	
	public Route getRoute() {
		return route; 
	}
	
	/**
	 * 
	 */
	public void setVehicleNo(int vehicleNumber) {
		this.vehicleNumber = vehicleNumber;
	}
	
	/**
	 * 
	 */	
	public int getVehicleNumber() {
		return vehicleNumber;
	} 
	
	/**
	 * 
	 */
	public void setIsAtLayover(Boolean isAtLayover) {
		this.isAtLayover = isAtLayover;
	}
	
	/**
	 * 
	 */	
	public boolean getIsAtLayover() {
	   return isAtLayover;
	}
	
	/**
	 * 
	 */
	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	
	/**
	 * 
	 */	
	public boolean getIsAvailable() {
	   return isAvailable;
	}
	
	/**
	 * 
	 */
	public void setDestination(Destination destination) {
		this.destination = destination;
	}
	
	/**
	 * 
	 */	
	public Destination getDestination() {
		return destination;
	}	

	/**
	 * 
	 */
	public void setHasSpecialEvent(Boolean hasSpecialEvent) {
		this.hasSpecialEvent = hasSpecialEvent;
	}
	
	/**
	 * 
	 */	
	public boolean getHasSpecialEvent() {
	   return hasSpecialEvent;
	}		

	/**
	 * 
	 */
	public void setHasDisruption(Boolean hasDisruption) {
		this.hasDisruption = hasDisruption;
	}
	
	/**
	 * 
	 */
	public boolean getHasDisruption() {
	   return hasDisruption;
	}		
	
	/**
	 * 
	 */
	public TramRunTime getTramRunTime(int index) {
		return times.get(index);
	}
	
	/**
	 * 
	 */
	public int getTramRunTimeCount() {
		return times.size();
	}
	
	/**
	 * 
	 */
	public void addTramRunTime(TramRunTime time) {
		times.add(time);

	}
	
	/**
	 * 
	 */	
	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		
		for (int i = 0; i < getTramRunTimeCount(); i++) {
			buf.append( getTramRunTime(i).toString() );
		}
		return buf.toString();
	}
	
	
}
