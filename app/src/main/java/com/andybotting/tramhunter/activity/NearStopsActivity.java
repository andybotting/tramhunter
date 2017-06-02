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

package com.andybotting.tramhunter.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.TramHunterConstants;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.content.Context.LOCATION_SERVICE;

public class NearStopsActivity extends AppCompatActivity implements LocationListener {

	private static final String TAG = "NearStopsActivity";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private final static int CONTEXT_MENU_VIEW_STOP = 0;

	private final String ACTIVITY_TITLE = "Nearest Stops"; // ±

	private ListView mListView;
	private List<Stop> mAllStops;
	private StopsList mNearStopsList;
	private LocationManager mLocationManager;
	private Criteria mCriteria;
	private Location mLastKnownLocation = null;
	protected LocationListener mLocationListener;

	private TramHunterDB mDB;

	private boolean mShowBusy = true;

	private String mErrorMessage = null;

	private AsyncTask<Stop, Void, ArrayList<Stop>> mFindLastLocationTask;
	private AsyncTask<Stop, Void, ArrayList<Stop>> mUpdateStopsTask;

	/**
	 * On Create
	 */
	@SuppressLint("InlinedApi")
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.near_stops_list);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(ACTIVITY_TITLE);

		mListView = (ListView) this.findViewById(android.R.id.list);
		mListView.setOnItemClickListener(listView_OnItemClickListener);
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);

		mDB = new TramHunterDB();

		// Get our stops from the DB
		mAllStops = mDB.getAllStops();

		// Make our Near Stops List for the map
		mNearStopsList = new StopsList();

		// Get the location
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mCriteria = new Criteria();

		if (TramHunterConstants.SUPPORTS_GINGERBREAD)
			mCriteria.setAccuracy(Criteria.ACCURACY_LOW);
		else
			mCriteria.setAccuracy(Criteria.ACCURACY_COARSE);

		if (!hasLocationEnabled())
			buildAlertNoLocationServices();

		// onResume will be called and getLocationAndUpdateStops()
	}

	protected ListView getListView() {
		if (mListView == null) {
			mListView = (ListView) findViewById(android.R.id.list);
		}
		return mListView;
	}

	protected void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	protected ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}

	/**
	 * Check Android settings that we have enabled location providers
	 */
	protected boolean hasLocationEnabled() {
		String bestAvailableProvider = mLocationManager.getBestProvider(mCriteria, true);
		if (bestAvailableProvider == null)
			return false;
		return true;
	}

	/**
	 * Fetch the best last known location
	 */
	protected Location getInitialLocation() {

		Location bestResult = mLastKnownLocation;

		for (String provider : mLocationManager.getAllProviders()) {
			Location location = mLocationManager.getLastKnownLocation(provider);
			if (location != null) {

				if (isBetterLocation(location, bestResult))
					bestResult = location;
			}
		}

		if (bestResult != null)
			if (LOGV) Log.d(TAG, "Found initial location from: " + bestResult.getProvider() + " " + StringUtil.humanFriendlyDate(bestResult.getTime()) + " ago");

		return bestResult;
	}

	/**
	 * On activity resume
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Get the last known location (and optionally request location updates)
		// and update the stops list.
		if (hasLocationEnabled()) {
			startLocationListening();
			getLocationAndUpdateStops();
		}
	}

	/**
	 * On activity pause
	 */
	@Override
	protected void onPause() {
		// Stop listening for location updates when the Activity is inactive.
		stopLocationListening();

		// Kill tasks on pause
		if (mFindLastLocationTask != null)
			mFindLastLocationTask.cancel(true);

		if (mUpdateStopsTask != null)
			mUpdateStopsTask.cancel(true);

		super.onPause();
	}

	/**
	 * On activity destroy
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
	}

	/**
	 * Build an alert dialog
	 */
	private void buildAlertNoLocationServices() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("You do not have GPS or Wireless network location services enabled.\n\nWould you like to enable them now?").setTitle("No Location Services")
				.setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.dismiss();
						launchLocationSettings();
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
						finish();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Open up the location settings
	 */
	private void launchLocationSettings() {
		final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(intent, -1);
	}

	/**
	 * View a stop
	 */
	private void viewStop(Stop stop) {
		int tramTrackerId = stop.getTramTrackerID();

		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);

		startActivityForResult(intent, 1);
	}

	/**
	 * Options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.near_stops, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Options item select
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId())
			{
			case R.id.menu_map:
				showMap();
				break;
			case android.R.id.home:
				finish();
			}

		return false;
	}

	/**
	 * Show the map view, passing this stop
	 */
	private void showMap() {

		// Detect Google Maps
		try {
			Class.forName("com.google.android.maps.MapActivity");
			// Map view
			Bundle bundle = new Bundle();
			bundle.putParcelable("stopslist", mNearStopsList);
			Intent intent = new Intent(NearStopsActivity.this, StopMapActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, -1);
		} catch (Exception e) {
			Toast.makeText(this, "Google Maps not available", Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * On click of menu item
	 */
	private OnItemClickListener listView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			StopsListAdapter stopsListAdapter = (StopsListAdapter) adapterView.getAdapter();
			viewStop(stopsListAdapter.getStops().get(position));
		}
	};

	/**
	 * Create context menu
	 */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			menu.add(0, CONTEXT_MENU_VIEW_STOP, 0, "View Stop");
		}
	};

	/**
	 * Find the last known location and update the stops list
	 */
	protected void getLocationAndUpdateStops() {

		if (LOGV) Log.d(TAG, "Getting location and updating stops");

		mFindLastLocationTask = new AsyncTask<Stop, Void, ArrayList<Stop>>() {

			// Can use UI thread here
			protected void onPreExecute() {
				if (mShowBusy) {
					// Show the dialog window
					mListView.setVisibility(View.GONE);
					findViewById(android.R.id.empty).setVisibility(View.GONE);
					findViewById(R.id.loading).setVisibility(View.VISIBLE);
					mShowBusy = false;
				}
			}

			@Override
			protected ArrayList<Stop> doInBackground(final Stop... params) {

				// Loop for a while until we get a location fix.
				// If we can't, then timeout and throw an error.
				int i = 0;
				while (mLastKnownLocation == null) {
					try {
						if (LOGV)
							Log.d(TAG, "Location not found yet. Sleeping 1s");
						Thread.sleep(1000);
						i++;

						if (i == TramHunterConstants.MAX_WAIT_LOCATION) {
							mErrorMessage = "Unable to find your location";
							return null;
						}

					} catch (InterruptedException e) {
					}
				}

				ArrayList<Stop> sortedStops = new ArrayList<Stop>();
				SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();

				for (Stop stop : mAllStops) {
					double distance = mLastKnownLocation.distanceTo(stop.getLocation());
					sortedStopList.put(distance, stop);
				}

				// Build a sorted list, of MAXSTOPS stops
				for (Entry<Double, Stop> item : sortedStopList.entrySet()) {
					Stop stop = item.getValue();

					// Don't show terminus stops > 8000
					if (stop.getTramTrackerID() < 8000) {
						sortedStops.add(stop);
						mNearStopsList.add(stop);
					}

					if (sortedStops.size() >= TramHunterConstants.MAX_NEARBY_STOPS)
						break;
				}

				return sortedStops;
			}

			// Can use UI thread here
			protected void onPostExecute(final ArrayList<Stop> sortedStops) {

				// Toast: Error fetching departure information
				if (mErrorMessage != null) {
					Toast.makeText(NearStopsActivity.this, mErrorMessage, Toast.LENGTH_LONG).show();
					// Reset error message
					mErrorMessage = null;
					finish();
				}
				else {
					StopsListAdapter stopsListAdapter;
					// Refresh the entire list
					stopsListAdapter = new StopsListAdapter(sortedStops, mLastKnownLocation);
					setListAdapter(stopsListAdapter);
				}

				// If we've just been showing the loading screen
				if (mListView.getVisibility() == View.GONE) {
					mListView.setVisibility(View.VISIBLE);
					findViewById(R.id.loading).setVisibility(View.GONE);
				}
			}
		};

		mFindLastLocationTask.execute();
	}

	/**
	 * Given a location, update the stops list
	 */
	protected void updateStops(Location l) {

		final Location location = l;
		if (LOGV) Log.d(TAG, "Updating stops list with location from: " + location.getProvider());

		// This isn't directly affecting the UI, so put it on a worker thread.
		mUpdateStopsTask = new AsyncTask<Stop, Void, ArrayList<Stop>>() {

			// Can use UI thread here
			protected void onPreExecute() {
				if (mShowBusy) {
					// Show the dialog window
					mListView.setVisibility(View.GONE);
					findViewById(android.R.id.empty).setVisibility(View.GONE);
					findViewById(R.id.loading).setVisibility(View.VISIBLE);
					mShowBusy = false;
				}
			}

			@Override
			protected ArrayList<Stop> doInBackground(final Stop... params) {

				ArrayList<Stop> sortedStops = new ArrayList<Stop>();
				SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();

				for (Stop stop : mAllStops) {
					double distance = location.distanceTo(stop.getLocation());
					sortedStopList.put(distance, stop);
				}

				// Build a sorted list
				for (Entry<Double, Stop> item : sortedStopList.entrySet()) {
					Stop stop = item.getValue();

					// Don't show terminus stops > 8000
					if (stop.getTramTrackerID() < 8000) {
						sortedStops.add(stop);
						mNearStopsList.add(stop);
					}

					if (sortedStops.size() >= TramHunterConstants.MAX_NEARBY_STOPS)
						break;
				}

				return sortedStops;
			}

			// Can use UI thread here
			protected void onPostExecute(final ArrayList<Stop> sortedStops) {
				StopsListAdapter stopsListAdapter;
				// Refresh the entire list
				stopsListAdapter = new StopsListAdapter(sortedStops, location);
				setListAdapter(stopsListAdapter);

				// If we've just been showing the loading screen
				if (mListView.getVisibility() == View.GONE) {
					mListView.setVisibility(View.VISIBLE);
					findViewById(R.id.loading).setVisibility(View.GONE);
				}
			}
		};

		mUpdateStopsTask.execute();
	}	
	
	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TramHunterConstants.LOCATION_UPDATE_DELTA;
		boolean isSignificantlyOlder = timeDelta < -TramHunterConstants.LOCATION_UPDATE_DELTA;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether two providers are the same
	 */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	/**
	 * On location changed
	 */
	public void onLocationChanged(Location location) {

		if (isBetterLocation(location, mLastKnownLocation)) {
			if (LOGV)
				Log.i(TAG, "Better location updated by provider: " + location.getProvider());

			mLastKnownLocation = location;

			updateStops(location);

			if (location.hasAccuracy()) {
				getSupportActionBar().setTitle(ACTIVITY_TITLE + " (±" + (int) location.getAccuracy() + "m)");
			} else {
				getSupportActionBar().setTitle(ACTIVITY_TITLE);
			}
		}
	}

	/**
	 * Stop listening for location events
	 */
	private void stopLocationListening() {
		if (LOGV)
			Log.i(TAG, "Stopping location listening");
		if (mLocationManager != null)
			mLocationManager.removeUpdates(this);

	}

	/**
	 * Start listening for location events
	 */
	private void startLocationListening() {
		if (mLocationManager != null) {

			for (String provider : mLocationManager.getProviders(mCriteria, true)) {
				if (LOGV)
					Log.i(TAG, "Listening for location from: " + provider);
				mLocationManager.requestLocationUpdates(provider, TramHunterConstants.MIN_TIME, TramHunterConstants.MIN_DISTANCE, this);
			}

			// Find last known location as a starter
			mLastKnownLocation = getInitialLocation();
		}
	}

	/**
	 * onProviderEnabled should restart the location listening
	 */
	@Override
	public void onProviderEnabled(String provider) {
		startLocationListening();
	}

	/**
	 * onProviderDisabled should restart the location listening
	 */
	@Override
	public void onProviderDisabled(String provider) {
		startLocationListening();
	}

	/**
	 * onStatusChanged unused
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/**
	 * Adapter for Stops list
	 */
	private class StopsListAdapter extends BaseAdapter {

		private ArrayList<Stop> mStops;
		private Location mLocation;

		public StopsListAdapter(ArrayList<Stop> stops, Location location) {
			mStops = stops;
			mLocation = location;
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
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.near_stops_list_row, parent, false);
			} else {
				pv = convertView;
			}

			Stop stop = (Stop) mStops.get(position);

			String stopName = stop.getPrimaryName();
			String stopDetails = stop.getStopDetailsLine();
			String stopDistance = stop.formatDistanceTo(mLocation);

			((TextView) pv.findViewById(R.id.stop_name)).setText(stopName);
			((TextView) pv.findViewById(R.id.stop_details)).setText(stopDetails);
			((TextView) pv.findViewById(R.id.stop_distance)).setText(stopDistance);
			((TextView) pv.findViewById(R.id.stop_routes)).setText(stop.getRoutesString());

			return pv;
		}
	}

}
