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

package com.andybotting.tramhunter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.location.Location;
import android.location.LocationManager;

import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.FavouriteList;

public class FavouriteStopUtil {
	private static final int UNLIMITED_METRES_AWAY = 0;
	
	private final LocationManager locationManager;
	
	public FavouriteStopUtil(LocationManager locationManager) {
		this.locationManager = locationManager;
	}

	
	public Favourite getClosestFavouriteStop() {
		final List<Favourite> closestFavourites = getClosestFavourites(1, UNLIMITED_METRES_AWAY);
		return (closestFavourites.isEmpty()) ? null : closestFavourites.get(0);
	}

	
	public List<Favourite> getClosestFavourites(final int maxNumberOfStops, final int maxMetresAway) {
		
		FavouriteList favouriteList = new FavouriteList();
		List<Favourite> closestFavourites = new ArrayList<Favourite>();
		
		if (favouriteList.hasFavourites()) {
			
			// Get our list of favourites
			List<Favourite> favourites = favouriteList.getFavouriteItems();
			
			// Get our last location
			Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			
			SortedMap<Double, Favourite> sortedFavouriteMap = new TreeMap<Double, Favourite>();
			
			if (location != null) {
			
				for (Favourite favourite : favourites) {
					double distance = location.distanceTo(favourite.getStop().getLocation());
					if (maxMetresAway == UNLIMITED_METRES_AWAY || distance <= maxMetresAway)
						sortedFavouriteMap.put(distance, favourite);
					
				}
				
				final List<Favourite> sortedFavouriteList = new ArrayList<Favourite>(sortedFavouriteMap.values());
				int numToReturn = (maxNumberOfStops <= sortedFavouriteList.size()) ? maxNumberOfStops : sortedFavouriteList.size();
				closestFavourites = sortedFavouriteList.subList(0, numToReturn);
				
				
			}
			
		}

		return closestFavourites;
	}
	
}
