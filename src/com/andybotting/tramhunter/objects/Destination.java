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
