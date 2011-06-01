package com.andybotting.tramhunter.objects;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.andybotting.tramhunter.dao.TramHunterDB;

import android.util.Log;

public class Favourite {
	
    private static final String TAG = "Favourite";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
	
	private Stop stop;
	private Route route = null;
	private String name = null;
	
	/**
	 * Constructor for a favourite specifying Stop, Route and Name
	 * @param stop
	 * @param route
	 * @param name
	 */
	public Favourite(Stop stop, Route route, String name) {
		this.route = route;
		this.stop = stop;
		this.name = name;
		
		if (LOGV) Log.i(TAG, "New Favourite with Name: " + this);
	}
	
	/**
	 * Constructor for a favourite specifying Stop and Route
	 * @param stop
	 * @param route
	 */
	public Favourite(Stop stop, Route route) {
		this.route = route;
		this.stop = stop;
		
		if (LOGV) Log.i(TAG, "New Favourite: " + this);
	}
	
	/**
	 * Get the stop for this favourite stop/route
	 * @return Stop
	 */
	public Stop getStop() {
		return stop;
	}

	/**
	 * Setthe stop for this favourite stop/route
	 * @param stop
	 */
	public void setStop(Stop stop) {
		this.stop = stop;
	}
	
	/**
	 * Get the route for this favourite stop/route
	 * @return
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * Set the route for this favourite stop/route
	 * @param route
	 */
	public void setRoute(Route route) {
		this.route = route;
	}
	
	/**
	 * Set the nickname of a the stop
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the nickname of the stop
	 * @return String
	 */
	public String getName() {
		
		if (name == null) {
			return stop.getStopName();
		}
		else {
			return name;
		}
	} 
	
	
	/**
	 * Return true if the favourite has a custom name
	 * @return boolean
	 */
	public boolean hasName() {
		if (name != null)
			return true;
		return false;
	}
	
	
	/**
	 * Clear a custom stop name
	 */
	public void clearName() {
		name = null;
	}
	
	/**
	 * If a specific route has not been set, then we just show all the routes for
	 * that 'favourited' stop. Otherwise, show them all as normal.
	 * @return String
	 */
	public String getRouteName() {
		if (route == null) {
			// Fetch the routes from the DB
			TramHunterDB db = new TramHunterDB();
			List<Route> routes = db.getRoutesForStop(stop.getTramTrackerID());
			db.close();
			stop.setRoutes(routes);
			return stop.getRoutesListString();
		}
		else {
			return "Route " + route.getNumber();
		}
	}
	
	
	/**
	 * Comparison of favourites
	 */
	@Override
	public boolean equals(Object obj) {
		Favourite favourite = (Favourite)obj;

		boolean equalStop = false;
		boolean equalRoute = false;
		
		// Compare the Stop
		if (favourite.getStop().getTramTrackerID() == this.stop.getTramTrackerID())
			equalStop = true;

		// Compare the Route
		int thisRouteId;
		int otherRouteId;
		
		if (this.route == null)
			thisRouteId = -1;
		else
			thisRouteId = this.route.getId();
		
		if (favourite.getRoute() == null)
			otherRouteId = -1;
		else
			otherRouteId = favourite.getRoute().getId();

		if (thisRouteId == otherRouteId)
			equalRoute = true;
		
		// If both Stop and Route are equal
		if (equalStop && equalRoute)
			return true;
		
		return false;
	}
	
	
	/**
	 * Serialize our favourite into a JSON Object
	 * @return
	 */
	public JSONObject getFavouriteJSON() {
		
		JSONObject object = new JSONObject();
		try {
			object.put("stop", stop.getTramTrackerID());
			
			if (route != null)
				object.put("route", route.getId());
			
			if (name != null)
				object.put("name", name);
		} 
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return object;
	}
	
	
	/**
	 * JSON string
	 */
	public String toString() {
		if (LOGV) Log.i(TAG, "New Favourite: " + getFavouriteJSON().toString());
		return getFavouriteJSON().toString();
	}
	
}
