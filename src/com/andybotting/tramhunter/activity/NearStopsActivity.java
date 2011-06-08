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

package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;

 
public class NearStopsActivity extends ListActivity implements LocationListener {
	
    private static final String TAG = "NearStopsActivity";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
	
	private final static int CONTEXT_MENU_VIEW_STOP = 0;
	
	private final static int MENU_MAP = 0;
	
	private ListView mListView;
	private List<Stop> mAllStops;
	private StopsList mNearStopsList;
	private StopsListAdapter mStopsListAdapter;
	private LocationManager mLocationManager;
	private Location mLastKnownLocation;

	//private Context mContext;
	//private PreferenceHelper mPreferenceHelper;
	private TramHunterDB mDB;
		
	// Maximum stops to list
	private final int MAXSTOPS = 20;
	private final String mTitle = "Nearest Stops";//±	
 
	// Only show loading dialog at first load
	private boolean mShowBusy = true;
	private boolean mIsListeningForNetworkLocation;
	private boolean mIsCalculatingStopDistances;
	
	
	/**
	 * 
	 */
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	 

		setContentView(R.layout.near_stops_list);
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(NearStopsActivity.this);
		    }
		});	

		// Map title button
		findViewById(R.id.title_btn_map).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putParcelable("stopslist", mNearStopsList);
				Intent intent = new Intent(NearStopsActivity.this, StopMapActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
		    }
		});
		
		mIsListeningForNetworkLocation = true;
		mIsCalculatingStopDistances = false;
		
		mListView = (ListView)this.findViewById(android.R.id.list);
		mListView.setOnItemClickListener(listView_OnItemClickListener);
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		
		// Set the title
		((TextView) findViewById(R.id.title_text)).setText(mTitle);

		//mContext = this.getBaseContext();
		//mPreferenceHelper = new PreferenceHelper();
		mDB = new TramHunterDB();
		
		// Get our stops from the DB
		mAllStops = mDB.getAllStops();
		
		// Make our Near Stops List for the map
		mNearStopsList = new StopsList();
		
		// Get the location
		mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

		displayNearStops();
	}	
	
	
	/**
	 * 
	 */
	private void displayNearStops() {
		startLocationListening(true);
	  	Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	  	
	  	if (LOGV) Log.i(TAG, "Enabled Location Services:" + 
	  			" Network=" + mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) +
	  			" GPS=" + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
	  	
	  	if (location != null) {		
	  		new StopDistanceCalculator(location).execute();			
    	}
	  	else {
	  		
	  	    if ( (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) && 
	  	    		(!mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) ) {
	  	    	buildAlertNoLocationServices();
	  	    }
	  	    
	  	}
	}
	
	
	/**
	 * On activity resume
	 */
    @Override
    protected void onResume() {
        super.onResume();
        displayNearStops();
    }
	
    
    /**
     * On activity pause
     */
    @Override
    protected void onPause() {
    	super.onPause();
        stopLocationListening();
	}
    
    
    /**
     * On activity destroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    	stopLocationListening();
    	mDB.close();
    }
	
	
	/**
	 * Build an alert dialog
	 */
    private void buildAlertNoLocationServices() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("You do not have GPS or Wireless network location services enabled.\n\nWould you like to enable them now?")
        	.setTitle("No Location Services")
        	.setCancelable(false)
        	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        		public void onClick(final DialogInterface dialog, final int id) {
        			launchGPSOptions(); 
        		}
        	})
        	.setNegativeButton("No", new DialogInterface.OnClickListener() {
        		public void onClick(final DialogInterface dialog, final int id) {
        			dialog.cancel();
        			finish();
        		}
        	});
        
        final AlertDialog alert = builder.create();
        alert.show();
    }
	
	
    /**
     * Open up the location settings
     */
    private void launchGPSOptions() {
        final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
        final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 0);
    }  
    
    
	
    /**
     * 
     * @param stop
     */
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		
		startActivityForResult(intent, 1);
	}
	
	
	/**
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_MAP, 0, "Map");
		MenuItem menuItem0 = menu.findItem(0);
		menuItem0.setIcon(R.drawable.ic_menu_mapmode);
		
		return true;
	}
	
	
	/**
	 * 
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_MAP:
			Bundle bundle = new Bundle();
			bundle.putParcelable("stopslist", mNearStopsList);
			Intent intent = new Intent(NearStopsActivity.this, StopMapActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
		}
		return false;

	}


	/**
	 * 
	 */
	private OnItemClickListener listView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			StopsListAdapter stopsListAdapter = (StopsListAdapter)adapterView.getAdapter();
			viewStop(stopsListAdapter.getStops().get(position));
		}
    };
    
    
    /**
     * 
     */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			menu.add(0, CONTEXT_MENU_VIEW_STOP, 0, "View Stop");
		}
    };
    
    
    /**
     * 
     */
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			StopsListAdapter stopsListAdapter = (StopsListAdapter)getListAdapter();
			Stop thisStop = stopsListAdapter.getStops().get(info.position);
        	
        	switch (item.getItemId()) {
    			case CONTEXT_MENU_VIEW_STOP:
    				viewStop(thisStop);
    				return true;
        	}
    	} catch (ClassCastException e) {}
    	    	
		return super.onContextItemSelected(item);
    }
    
    
    /**
     * 
     * @author andy
     *
     */
	private class StopDistanceCalculator extends AsyncTask<Stop, Void, ArrayList<Stop>> {
		
		private final Location mLocation;
		private boolean mRefreshListOnly;
		
		public StopDistanceCalculator(Location location){
			mLocation = location;
		}
		
		// Can use UI thread here
		protected void onPreExecute() {
			mIsCalculatingStopDistances = true;
//			mRefreshListOnly = !mShowBusy;			
			if (mShowBusy) {
				// Show the dialog window
				mListView.setVisibility(View.GONE);
				findViewById(android.R.id.empty).setVisibility(View.GONE);
				findViewById(R.id.loading).setVisibility(View.VISIBLE);
				mShowBusy = false;
			}
		}

		// Automatically done on worker thread (separate from UI thread)
		protected ArrayList<Stop> doInBackground(final Stop... params) {
			ArrayList<Stop> sortedStops = new ArrayList<Stop>();
			SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();
			
	    	for(Stop stop : mAllStops){
	    		double distance = mLocation.distanceTo(stop.getLocation());
	    		sortedStopList.put(distance, stop);
	    	}  
	
	    	// Build a sorted list, of MAXSTOPS stops 
	    	for(Entry<Double, Stop> item : sortedStopList.entrySet()) {
	    		Stop stop = item.getValue();
	    		
	    		// Don't show terminus stops > 8000
				if (stop.getTramTrackerID() < 8000)
					sortedStops.add(stop);
	    		
				if(sortedStops.size() >= MAXSTOPS)
	    			break;
	    	}

	    	return sortedStops;
		}
		
		// Can use UI thread here
		protected void onPostExecute(final ArrayList<Stop> sortedStops) {
			StopsListAdapter stopsListAdapter;
			
//			if (mRefreshListOnly) {
//				// Just update the list
//				stopsListAdapter = (StopsListAdapter) getListAdapter();
//				stopsListAdapter.updateStopList(sortedStops, mLocation);
//			}
//			else {
				// Refresh the entire list
				stopsListAdapter = new StopsListAdapter(sortedStops, mLocation);
				setListAdapter(stopsListAdapter);	
//			}
			
			// If we've just been showing the loading screen
			if (mListView.getVisibility() == View.GONE) {
				mListView.setVisibility(View.VISIBLE);
				findViewById(R.id.loading).setVisibility(View.GONE);
			}
			
			mIsCalculatingStopDistances = false;
		}
	}

	
	
	/**
	 * 
	 * @author andy
	 *
	 */
	private class StopsListAdapter extends BaseAdapter {
		
		private ArrayList<Stop> mStops;
		private Location mLocation;
		
		public StopsListAdapter(ArrayList<Stop> stops, Location location){
			mStops = stops;
			mLocation = location;
		}
		
		public void updateStopList(ArrayList<Stop> stops, Location location){
			mStops = stops;
			mLocation = location;
			this.notifyDataSetChanged();			
		}
		
		public ArrayList<Stop> getStops() {
			return mStops;
		}

		public int getCount() {
			return mStops.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv;
            if(convertView == null) {
    			LayoutInflater inflater = getLayoutInflater();
    			pv = inflater.inflate(R.layout.near_stops_list_row, parent, false);
            }
            else {
                pv = convertView;
            }
			
			Stop stop = (Stop) mStops.get(position);
			
			String stopName = stop.getPrimaryName();
			String stopDetails = "Stop " + stop.getFlagStopNumber();
			// If the stop has a secondary name, add it
			if (stop.getSecondaryName().length() > 0) {
				stopDetails += ": " + stop.getSecondaryName();
			}
			
			stopDetails += " - " + stop.getCityDirection();
			stopDetails += " (" + stop.getTramTrackerID() + ")";
			
			String stopDistance = stop.formatDistanceTo(mLocation);

			((TextView) pv.findViewById(R.id.stopNameTextView)).setText(stopName);
			((TextView) pv.findViewById(R.id.stopDetailsTextView)).setText(stopDetails);
			((TextView) pv.findViewById(R.id.stopDistanceTextView)).setText(stopDistance);
			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(stop.getRoutesString());
			
			return pv;
		}
			
	}	
	
    
	/**
	 * 
	 */
    public void onLocationChanged(Location location) {

    	if (location != null) {	
        	// If this is a GPS location then ignore and unsubscribe from network location updates.
        	if (location.getProvider().equals("gps") && mIsListeningForNetworkLocation) {
        		stopLocationListening();
        		startLocationListening(false);
        	}
        	
        	if(shouldCalculateNewDistance(location)) {
        		new StopDistanceCalculator(location).execute();
        		mLastKnownLocation = location;
        	}
        	
        	if(location.hasAccuracy()) {
        		((TextView) findViewById(R.id.title_text)).setText(mTitle + " (±" + (int)location.getAccuracy() + "m)");
        	}
        	else {
        		((TextView) findViewById(R.id.title_text)).setText(mTitle);
        	}
        	
    	}
    }
    
    
    /**
     * 
     * @param location
     * @return
     */
    private boolean shouldCalculateNewDistance(Location location) {
		boolean result = false;
		
    	if (mLastKnownLocation != null && mLastKnownLocation.distanceTo(location) > 1) {
    		result = true;	
    	}
    	else if(mLastKnownLocation == null) {
    		result = true;
    	}
    	
    	return result && (!mIsCalculatingStopDistances);
    }

    
    /**
     * 
     */
    private void stopLocationListening() {
    	if (LOGV) Log.i(TAG, "Stopping location listening");
    	if (mLocationManager != null) {
    		mLocationManager.removeUpdates(this);
    	}
	}
    
    
    /**
     * 
     * @param subscribeToNetworkLocation
     */
    private void startLocationListening(boolean subscribeToNetworkLocation) {
    	if (LOGV) Log.i(TAG, "Starting location listening");
    	if (mLocationManager != null) {
    		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        	mIsListeningForNetworkLocation = subscribeToNetworkLocation;
        	
        	if(subscribeToNetworkLocation)       		
        		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 20, this);
    	}
	}

    
    /**
     * 
     */
	@Override
	public void onProviderDisabled(String provider) {}

	
	/**
	 * 
	 */
	@Override
	public void onProviderEnabled(String provider) {}

	
	/**
	 * 
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
}
