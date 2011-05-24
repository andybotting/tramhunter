package com.andybotting.tramhunter.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.util.PreferenceHelper;

import android.util.Log;

public class FavouriteList {
	
    private static final String TAG = "FavouriteList";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private final List<Favourite> mFavourites;
	private PreferenceHelper mPreferenceHelper;
	
	/**
	 * Construct a FavouriteList object by its JSON string
	 * @param jsonFavouriteString
	 */
	public FavouriteList() {
		mPreferenceHelper = new PreferenceHelper();
		mFavourites = getFavouriteItems();
	}
	
	
	/**
	 * Get the favourite items
	 * @return 
	 */
	public ArrayList<Favourite> getFavouriteItems() {
		
		ArrayList<Favourite> favourites = new ArrayList<Favourite>();
		JSONArray favouriteJSONArray = null;
		TramHunterDB db = new TramHunterDB();
		
		// Fetch JSON favourite stops string from preferences
		String favouriteString = mPreferenceHelper.getStarredStopsString();
		if (LOGV) Log.i(TAG, "Parsing favourite string: " + favouriteString);

		// Check to see if we even have any favourites
		if (favouriteString.length() > 1) {
			
			// Convert any old favourites - if not a JSON Array
			if (!favouriteString.contains("{"))
				favouriteString = convertOldFavourites(favouriteString);

			try {
				favouriteJSONArray = new JSONArray(favouriteString);
				
		        for (int i = 0; i < favouriteJSONArray.length(); i++) {
				
					JSONObject favouriteJSONObject = favouriteJSONArray.getJSONObject(i);
					
		            int tramTrackerID = favouriteJSONObject.optInt("stop");
		            int routeID = favouriteJSONObject.optInt("route", -1);
		            String name = favouriteJSONObject.optString("name", null);
		    		
		            Stop stop = db.getStop(tramTrackerID);
		            Route route = null;
		            
		            if (routeID != -1)
		            	route = db.getRoute(routeID);

		            favourites.add(new Favourite(stop, route, name));
		        }
		            
			}
		        catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
			}
		        
		}
		db.close();
		return favourites;
	}
	
	
	/**
	 * Convert an old favourites string to the new JSON format
	 * @param favouriteString
	 * @return JSONArray
	 */
	private String convertOldFavourites(String favouriteString) {

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject;
		int tramTrackerId;
		
		StringTokenizer tokenizer = new StringTokenizer(favouriteString, ",");
        while (tokenizer.hasMoreTokens()) {
            tramTrackerId = Integer.parseInt(tokenizer.nextToken());
            
    		try {
    			jsonObject = new JSONObject();
    			jsonObject.put("stop", tramTrackerId);
    			jsonArray.put(jsonObject);
    		} catch (JSONException e) {
    			e.printStackTrace();
    			return "";
    		}
        }
		return jsonArray.toString();
	}
		
		
	/**
	 * Return a boolean of whether there is any favourites or not
	 * @return
	 */
	public Boolean hasFavourites() {
		if (mFavourites.isEmpty())
			return false;
		
		return true;
	}

	/**
	 * Return the number of favourites in the list
	 * @return Integer
	 */
	public int getCount() {
		return mFavourites.size();
	}

	
	/**
	 * Return the list of favourites
	 * @return
	 */
	public List<Favourite> getFavourites() {
		return mFavourites;
	}
	
	
	/**
	 * Return the favourite at a given location
	 * @return
	 */
	public Favourite getFavourite(int location) {
		return mFavourites.get(location);
	}	
	
	
	/**
	 * Add a favourite to the list at a specific location
	 */
	public void addFavourite(Favourite favourite, int location) {
		mFavourites.add(location, favourite);
		writeFavourites();
	}

	
	/**
	 * Append a favourite to the list
	 */
	public void addFavourite(Favourite favourite) {
		mFavourites.add(favourite);
		writeFavourites();
	}
	
	
	/** 
	 * Remove a favourite from the list by its location
	 * @param location
	 */
	public void removeFavourite(int location) {
		mFavourites.remove(location);
		writeFavourites();
	}
	
	
	/**
	 * Remove a favourite from the list by its object
	 * @param favourite
	 */
	public void removeFavourite(Favourite favourite) {
		mFavourites.remove(favourite);
		writeFavourites();
	}
	
	
	/**
	 * Check to see if an existing favourite exists from a given stop and route
	 * @param stop
	 * @param route
	 * @return Boolean
	 */
	public Boolean isFavourite(Stop stop, Route route) {
		for (Favourite favourite : this.mFavourites) {
			if ( (favourite.getStop().getTramTrackerID() == stop.getTramTrackerID()) && 
					(favourite.getRoute().getId() == route.getId()) ) {
				return true;
			}
		}
		
		return false;
	}

	
	/**
	 * Check to see if an existing favourite exists from an existing Favourite object
	 * @param favourite
	 * @return
	 */
	public Boolean isFavourite(Favourite favourite) {

		for (Favourite fav : this.mFavourites) {
			if (favourite.equals(fav))
				return true;
		}
		return false;
	}

	
	/**
	 * Favourite or Unfavourite a stop based on its current state
	 * @param stop
	 * @param route
	 */
	public void toggleFavourite(Favourite favourite) {
		if (isFavourite(favourite))
			removeFavourite(favourite);
		else
			addFavourite(favourite);
		
		writeFavourites();
	}
	
	
	/**
	 * Add or remove a favourite
	 * @param favourite
	 * @param checked
	 */
	public void setFavourite(Favourite favourite, boolean check) {
		if (!isFavourite(favourite) && check) {
			addFavourite(favourite);
		}
		else {
			removeFavourite(favourite);
		}
	}
	
	
	/**
	 * Generate a JSON string serializing our favourites
	 */
	public JSONArray getFavouritesJSON() {

		JSONArray jsonArray = new JSONArray();
		
		for (Favourite favourite : mFavourites) {
			jsonArray.put(favourite.getFavouriteJSON());
		}
		
		return jsonArray;
	}
	
	/**
	 * Write our favourite list to preferences
	 */
	public void writeFavourites() {
		String jsonString = getFavouritesJSON().toString();
		if (LOGV) Log.i(TAG, "Writing out favourite string: " + jsonString);
		mPreferenceHelper.setStarredStopsString(jsonString);
	}



	
	
	
}