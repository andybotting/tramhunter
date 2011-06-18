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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.FavouriteList;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceException;
import com.andybotting.tramhunter.service.TramTrackerServiceJSON;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class StopDetailsActivity extends ListActivity {
	
    private static final String TAG = "StopDetailsActivity";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
		
	private final static int MENU_ITEM_REFRESH = 0;
	private final static int MENU_ITEM_FAVOURITE = 1;
	private final static int MENU_ITEM_MAP = 2;
	
	private TramHunterDB mDB;
	private CompoundButton mStarButton;

	private List<NextTram> mNextTrams = new ArrayList<NextTram>();
	private Stop mStop;
	private Route mRoute = null;
	private int mTramTrackerId;
    private volatile Thread mRefreshThread;
    
    boolean mLoadingError = false;
    boolean mShowDialog = true;
    
	private ListAdapter mListAdapter;
	private ListView mListView;
	private Spinner mRoutesSpinner;
	private ArrayAdapter<CharSequence> mAdapterForSpinner;
    
    private PreferenceHelper mPreferenceHelper;
    private FavouriteList mFavouriteList;
    private TramTrackerService ttService;
    
    private String mErrorMessage = null;
	private int mErrorRetry = 0;
	private final int MAX_ERRORS = 3;
	private boolean mFirstDepartureReqest = true;
	
	private static final int REFRESH_SECONDS = 60;
     
    // Handle the timer
    Handler UpdateHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		new GetNextTramTimes().execute();
    	}
	};

	
	/**
	 * On create of this class
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		setContentView(R.layout.stop_details);	
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(StopDetailsActivity.this);
		    }
		});	

		// Refresh title button
		findViewById(R.id.title_btn_refresh).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	mShowDialog = true;
		    	new GetNextTramTimes().execute();
		    }
		});	
		
		// Map title button
		findViewById(R.id.title_btn_map).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
				Bundle bundle = new Bundle();
				StopsList mStopList = new StopsList();
				mStopList.add(mStop);
				bundle.putParcelable("stopslist", mStopList);
				Intent intent = new Intent(StopDetailsActivity.this, StopMapActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
		    }
		});			
		
		
        // Set up our list
        mListAdapter = new NextTramsListAdapter();
		mListView = getListView();
		mListView.setVisibility(View.GONE);
		
		// Preferences
		mPreferenceHelper = new PreferenceHelper();
		
		// Get bundle data
		int routeId = -1;
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			mTramTrackerId = extras.getInt("tramTrackerId");
			routeId = extras.getInt("routeId", -1);
		}  
		
		// Get our favourite stops list
		mFavouriteList = new FavouriteList();
		
		// Create out DB instance
		mDB = new TramHunterDB();
		mStop = mDB.getStop(mTramTrackerId);
		if (routeId > -1)
			mRoute = mDB.getRoute(routeId);

		// Set the title
		String title = mStop.getStopName();
		((TextView) findViewById(R.id.title_text)).setText(title);

		// Display stop data
		displayStop(mStop);
		
		// Star button
		mStarButton = (CompoundButton)findViewById(R.id.stopStar);
		mStarButton.setChecked(mFavouriteList.isFavourite(new Favourite(mStop, mRoute)));
		
		mStarButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFavouriteList.setFavourite(new Favourite(mStop, mRoute), mStarButton.isChecked());
			}
		});

		// Get our TramTracker service, either SOAP (def) or JSON
//		if (mPreferenceHelper.isJSONAPIEnabled())
//			ttService = new TramTrackerServiceJSON();
//		else
		ttService = new TramTrackerServiceSOAP();
		
		// Our thread for updating the stops every 60 secs
        mRefreshThread = new Thread(new CountDown());
        mRefreshThread.setDaemon(true);
        mRefreshThread.start();	

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
	}

	@Override
	public void onStop() {
		super.onStop();
		mRefreshThread.interrupt();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(mRefreshThread.isInterrupted()) {
			mRefreshThread.start();
		}
	}

	
    /**
     * Update refresh status icon/views
     */
	private void showLoadingView(boolean isRefreshing) {
		
		// Only 1 entry means an error, so make it < 2
		if (mListAdapter.getCount() < 2)
			mListView.getEmptyView().setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
		else
			mListView.setVisibility(isRefreshing ? View.GONE : View.VISIBLE);

		findViewById(R.id.departures_loading).setVisibility(isRefreshing ? View.VISIBLE : View.GONE);
	}

	
    /**
     * Update refresh status icon/views
     */
	private void showRefreshSpinner(boolean isRefreshing) {
		findViewById(R.id.title_btn_refresh).setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
		findViewById(R.id.title_refresh_progress).setVisibility(isRefreshing ? View.VISIBLE : View.GONE);
	}
	
	
	/**
	 * Create the options for a given menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ITEM_REFRESH, 0, "Refresh");
		MenuItem menuItem1 = menu.findItem(MENU_ITEM_REFRESH);
		menuItem1.setIcon(R.drawable.ic_menu_refresh);

		menu.add(0, MENU_ITEM_FAVOURITE, 0, ""); // Title set in onMenuOpened()
		MenuItem menuItem2 = menu.findItem(MENU_ITEM_FAVOURITE);
		menuItem2.setIcon(R.drawable.ic_menu_star);
		
		menu.add(0, MENU_ITEM_MAP, 0, "Map");
		MenuItem menuItem3 = menu.findItem(MENU_ITEM_MAP);
		menuItem3.setIcon(R.drawable.ic_menu_mapmode);
		
		return true;
	}

	
	/**
	 * Overide the onMenuOpened method to allow us to dynamically set the menu item
	 * title on open.
	 */
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		boolean isFavourite = mFavouriteList.isFavourite(new Favourite(mStop, mRoute));
		menu.getItem(MENU_ITEM_FAVOURITE).setTitle(isFavourite ? "Unfavourite" : "Favourite");
		return super.onMenuOpened(featureId, menu);
	}


	/**
	 * Menu actions
	 * @param menuItem
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem menuItem) {
		
		switch (menuItem.getItemId()) { 
		
			case MENU_ITEM_REFRESH:
				mShowDialog = true;
				new GetNextTramTimes().execute();
				return true;
				
			case MENU_ITEM_FAVOURITE:
				mStarButton = (CompoundButton)findViewById(R.id.stopStar);
				Favourite favourite = new Favourite(mStop, mRoute);
				boolean isFavourite = mFavouriteList.isFavourite(favourite);
				mFavouriteList.toggleFavourite(favourite);
				mStarButton.setChecked(!isFavourite);
				return true;
				
			case MENU_ITEM_MAP:
				// Map view
				Bundle bundle = new Bundle();
				StopsList mStopList = new StopsList();
				mStopList.add(mStop);
				bundle.putParcelable("stopslist", mStopList);
				Intent intent = new Intent(StopDetailsActivity.this, StopMapActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
				return true;
		}
		
		return false;
	}

	
	/**
	 * Display the details for a given stop
	 * @param stop
	 */
	private void displayStop(Stop stop) {
		
		// Set labels from Stop hash map
		String firstLineText = stop.getPrimaryName();	
		
		String secondLineText = "Stop " + stop.getFlagStopNumber();
		// If the stop has a secondary name, add it
		if (stop.getSecondaryName().length() > 0) {
			secondLineText += ": " + stop.getSecondaryName();
		}
		secondLineText += " - " + stop.getCityDirection();
		secondLineText += " (" + stop.getTramTrackerID() + ")";
		
		//initialiseDatabase();
		final List<Route> mRoutes = mDB.getRoutesForStop(mTramTrackerId);
		stop.setRoutes(mRoutes);
		
		String thirdLineText = stop.getRoutesString();
			
		((TextView)findViewById(R.id.stopNameTextView)).setText(firstLineText);
		((TextView)findViewById(R.id.stopDetailsTextView)).setText(secondLineText);
		((TextView)findViewById(R.id.stopRoutesTextView)).setText(thirdLineText);

		// If we have more than one route for this stop, then show the spinner
		if (mRoutes.size() > 1) {
			mRoutesSpinner = (Spinner)findViewById(R.id.routeSelectSpinner);
			mRoutesSpinner.setVisibility(View.VISIBLE);
			mAdapterForSpinner = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
			mAdapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mRoutesSpinner.setAdapter(mAdapterForSpinner);
		
			// Add 'All'
			mAdapterForSpinner.add("All Routes");
			
			Route r;
			for (int i = 0; i < mRoutes.size(); i++) {
				r = mRoutes.get(i);
				mAdapterForSpinner.add("Route " + r.getNumber());
				
				// If we have a route already (e.g. passed from Favourites activity, then
				// select it right now
				if (mRoute != null) {
					if (mRoute.getId() == r.getId()) {
						// i+1 because we have 'All Route' in position 0
						mRoutesSpinner.setSelection(i+1);
					}
				}
				
			}
			
			mRoutesSpinner.setOnItemSelectedListener(
				new OnItemSelectedListener() {
					public void onItemSelected(
						AdapterView<?> parent, View view, int position, long id) {
							if (id == 0) {
								mRoute = null;
							}
							else {
								// -1 for the offset of having 'All Routes' first item
								mRoute = mRoutes.get(position-1);
								if (LOGV) Log.v(TAG, "Route selected: " + mRoute);
								
							}
							
							// Refresh the results
							mStarButton.setChecked(mFavouriteList.isFavourite(new Favourite(mStop, mRoute)));
					    	mShowDialog = true;
					    	new GetNextTramTimes().execute();
	                    }
	
						public void onNothingSelected(AdapterView<?> parent) {
	                    	mRoute = null;
						}
				}
			);
		}
		
		
	}
	

	/**
	 * Create a countdown thread
	 * @author andy
	 *
	 */
    private class CountDown implements Runnable {
        public void run() {
        	while(!Thread.currentThread().isInterrupted()){
        		Message m = new Message();
        		UpdateHandler.sendMessage(m);
        		try {
        			// 60 Seconds
        			Thread.sleep(REFRESH_SECONDS * 1000);
        		} 
        		catch (InterruptedException e) {
        			Thread.currentThread().interrupt();
        		}
        	}
        }
	}
	
    
    /**
     * Background task for fetching tram times
     */
	private class GetNextTramTimes extends AsyncTask<NextTram, Void, List<NextTram>> {

		@Override
		protected void onPreExecute() {
			// Show the spinner in the title bar
			if (mShowDialog)
				showLoadingView(true);
			
			showRefreshSpinner(true);
		}

		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {
			try {
				mNextTrams = ttService.getNextPredictedRoutesCollection(mStop, mRoute);

				for (int i = 0; i < mNextTrams.size(); i++) {
					// Remove any entries that show 'anyType{}' - they are errors
					if (mNextTrams.get(i).getDestination().matches("anyType"))
						mNextTrams.remove(i);
				}
			} 
			catch (TramTrackerServiceException e) {
				// Retry a couple of times before error
				if (mErrorRetry < MAX_ERRORS) {
					mErrorRetry++;
					if (LOGV) Log.e(TAG, "Error " + mErrorRetry + " of " + MAX_ERRORS + ": " + e);
					this.doInBackground(params);
				}
				else {
					// Save the error message for the toast
					mErrorMessage = e.getMessage();
				}
			}			
			return mNextTrams;
		}

		@Override
		protected void onPostExecute(List<NextTram> nextTrams) {
        	if (mErrorRetry == MAX_ERRORS) {

        		// Toast: Error fetching departure information
        		if (mFirstDepartureReqest) 
        			UIUtils.popToast(getApplicationContext(), getResources().getText(R.string.dialog_error_fetching) +": \n(" + mErrorMessage + ")");
        		
        		mErrorMessage = null;
        		mErrorRetry = 0;
        	}
        	else {
        		boolean noResults = true;
       		
				if (nextTrams.size() > 0) {
					mLoadingError = false;
					
					// Sort trams by minutesAway
					Collections.sort(nextTrams);
					
					// Show trams list, only if more than one tram.
					if (nextTrams.size() > 1) {
						noResults = false;
						setListAdapter(mListAdapter);
					}

	        		// If it's the first load of data
	        		if (mFirstDepartureReqest) {
						// > 10 because ksoap2 fills in anytype{} instead of null
	        			String specialEventMessage = nextTrams.get(0).getSpecialEventMessage();
						if (specialEventMessage.length() > 10)
							showSpecialEvent(specialEventMessage);
	        			
	   					if (mPreferenceHelper.isSendStatsEnabled()) {
	   						new Thread() {
	   							public void run() {
	   								uploadStats();
	   							}
	   						}.start();
	   					}
	   					
	   					// Reset the first departure request
	   					mFirstDepartureReqest = false;
	        		}
        		}
        		
        		if (noResults) {
        			mListView.getEmptyView().setVisibility(noResults ? View.VISIBLE : View.GONE);
       				mListView.setVisibility(noResults ? View.VISIBLE : View.GONE);
        		}

        	}
        	        	
        	// Hide the loading spinners
        	showLoadingView(false);
        	showRefreshSpinner(false);
    		mShowDialog = false;
		}
	}
	
	
	/**
	 * Show a dialog message for a given 'Special Event'
	 * @param message
	 */
	private void showSpecialEvent(String message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Special Event");
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton("OK", null);
        dialogBuilder.setIcon(R.drawable.ic_dialog_alert);
        dialogBuilder.show();
	}
	

	/**
	 * Create a NextTramsListAdapter for showing our next trams
	 * @author andy
	 *
	 */
	private class NextTramsListAdapter extends BaseAdapter {
	
		public int getCount() {
			return mNextTrams.size();
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
    			pv = inflater.inflate(R.layout.stop_details_row, parent, false);
            }
            else {
                pv = convertView;
            }
            
			NextTram thisTram = (NextTram) mNextTrams.get(position);
			((TextView) pv.findViewById(R.id.routeNumber)).setText(thisTram.getRouteNo());
			((TextView) pv.findViewById(R.id.routeDestination)).setText(thisTram.getDestination());
			((TextView) pv.findViewById(R.id.nextTime)).setText(thisTram.humanMinutesAway());
			
			if (mPreferenceHelper.isTramImageEnabled()) {
				mDB = new TramHunterDB();
				
				int tramNumber = thisTram.getVehicleNo();
				if (LOGV) Log.v(TAG, thisTram.toString() + " has tram number: " + tramNumber);
				
				if (tramNumber > 0) {
					String tramClass = mDB.getTramClass(tramNumber);
					String tramClassImage = UIUtils.getTramImage(tramClass);

					if (tramClassImage != null) {	
						if (LOGV) Log.v(TAG, "Tram Class: " + tramClass + " Tram Image: " + tramClassImage);
						int resID = getResources().getIdentifier(tramClassImage, "drawable", getPackageName());
						((ImageView) pv.findViewById(R.id.tramClass)).setPadding(3, 5, 3, 3);
						((ImageView) pv.findViewById(R.id.tramClass)).setBackgroundResource(resID);
					}
				}
				mDB.close();
			}
			return pv;
        }
			
	}		


	/**
	 * Upload statistics to our web server
	 */
    private void uploadStats() {
    	if (LOGV) Log.i(TAG, "Sending stop request statistics");
    	
		// gather all of the device info
    	try {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String device_uuid = tm.getDeviceId();
			String device_id = "00000000000000000000000000000000";
			if (device_uuid != null) {
				device_id = GenericUtil.MD5(device_uuid);
			}
			
			LocationManager mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
			Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	
			// post the data
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://tramhunter.andybotting.com/stats/stop/send");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");
	
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("device_id", device_id));
			pairs.add(new BasicNameValuePair("guid", ttService.getGUID()));	
			pairs.add(new BasicNameValuePair("ttid", String.valueOf(mStop.getTramTrackerID())));
			
			if (location != null) {
				pairs.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
				pairs.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
				pairs.add(new BasicNameValuePair("accuracy", String.valueOf(location.getAccuracy())));
			}

			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
	
			try {
				HttpResponse response = client.execute(post);
				response.getStatusLine().getStatusCode();
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    }

}