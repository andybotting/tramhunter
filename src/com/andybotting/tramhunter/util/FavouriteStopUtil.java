package com.andybotting.tramhunter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.location.Location;
import android.location.LocationManager;

import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class FavouriteStopUtil {
	private static final int UNLIMITED_METRES_AWAY = 0;
	
	private final TramHunterDB db;
	private final LocationManager locationManager;

	public FavouriteStopUtil(TramHunterDB db, LocationManager locationManager) {
		this.db = db;
		this.locationManager = locationManager;
	}

	public Stop getClosestFavouriteStop() {
		final List<Stop> closestFavourites = getClosestFavouriteStops(1, UNLIMITED_METRES_AWAY);
		return (closestFavourites.isEmpty()) ? null : closestFavourites.get(0);
	}

	public List<Stop> getClosestFavouriteStops(final int maxNumberOfStops, final int maxMetresAway) {
		final List<Stop> stops = db.getFavouriteStops();
		List<Stop> closestFavourites = new ArrayList<Stop>();
		if (!stops.isEmpty()) {
			// Get the location
			Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			
			if (location != null) {
				// Order favourites by location
				SortedMap<Double, Stop> sortedStopMap = new TreeMap<Double, Stop>();
		
				for(Stop stop : stops){
					double distance = location.distanceTo(stop.getLocation());
					if (maxMetresAway == UNLIMITED_METRES_AWAY || distance <= maxMetresAway)
					{
						sortedStopMap.put(distance, stop);
					}
				}
				
				final List<Stop> sortedStopList = new ArrayList<Stop>(sortedStopMap.values());
				int numToReturn = (maxNumberOfStops <= sortedStopList.size()) ? maxNumberOfStops : sortedStopList.size();
				closestFavourites = sortedStopList.subList(0, numToReturn);
				
				for (Stop stop : closestFavourites) {
					stop.setRoutes(db.getRoutesForStop(stop.getTramTrackerID()));
				}
			}
		}
		return closestFavourites;
	}
	
}
