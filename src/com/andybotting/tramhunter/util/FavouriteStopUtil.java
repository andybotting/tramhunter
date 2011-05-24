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
