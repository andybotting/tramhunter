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



public class NextTramCollection {
	
	private Stop stop;
	private List<NextTram> trams = new ArrayList<NextTram>();

	
	public void setStop(Stop _stop) {
		stop = _stop;
	}
	
	
	public Stop getStop(){
		return stop;
	}
	
	
	 
	
	/**
	 * Returns a specific property at a certain index.
	 * 
	 * @param index
	 *			the index of the desired property
	 * @return the desired property
	 */
	public NextTram getTram(int index) {
		return trams.get(index);
	}
	
	/**
	 * Returns the number of properties
	 * 
	 * @return the number of properties
	 */
	public int getTramCount() {
		return trams.size();
	}
	
	/**
	 * Adds a property (parameter) to the object. This is essentially a sub
	 * element.
	 * 
	 * @param propertyInfo
	 *			designated retainer of desired property
	 * @param value
	 *			the value of the property
	 */
	public void addTram(NextTram tram) {
		trams.add(tram);

	}
	
	
	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		buf.append("Stop: ");
		buf.append(stop.toString());
		buf.append("\n");
		
		for (int i = 0; i < getTramCount(); i++) {
			buf.append( getTram(i).toString() );
		}
		return buf.toString();
	}
	
	
}
