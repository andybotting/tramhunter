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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.FavouriteList;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.service.TramNotification;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceException;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.service.TramTrackerServiceJSON;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class StopDetailsActivity extends SherlockListActivity {

	private static final String TAG = "StopDetailsActivity";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private TramHunterDB mDB;

	private List<NextTram> mNextTrams = new ArrayList<NextTram>();
	private Stop mStop;
	private Route mRoute = null;
	private int mTramTrackerId;
	private volatile Thread mRefreshThread;

	private ListAdapter mListAdapter;
	private ListView mListView;
	private Spinner mRoutesSpinner;
	private ArrayAdapter<CharSequence> mAdapterForSpinner;

	private PreferenceHelper mPreferenceHelper;
	private FavouriteList mFavouriteList;
	private TramTrackerService ttService;

	private String mErrorMessage = null;
	private int mErrorRetry = 0;
	private final int MAX_ERRORS = 2;
	private boolean mFirstDepartureReqest = true;

	private static final int REFRESH_SECONDS = 60;

	// Menu items
	private MenuItem mRefreshItem;
	private MenuItem mFavouriteItem;
	private View mRefreshIndeterminateProgressView;

	// Refresh
	private boolean mShowDialog = true;
	private boolean mIsRefreshing = false;

	// Handle the timer
	Handler UpdateHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (!mFirstDepartureReqest)
				mShowDialog = false;
			getNextTramTimes();
		}
	};

	/**
	 * On create of this class
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.stop_details);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Set up our list
		mListAdapter = new NextTramsListAdapter();
		mListView = getListView();
		findViewById(R.id.departures_list).setVisibility(View.GONE);

		mListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				NextTram nextTram = (NextTram) mListAdapter.getItem(position);
				int vehicleNumber = nextTram.getVehicleNo();
				
				Bundle bundle = new Bundle();
				bundle.putInt("vehicleNumber", vehicleNumber);
				Intent intent = new Intent(StopDetailsActivity.this, TramRunActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);				
			}
			
		});
		
		// long click on list item implementation
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, final long id) {

				final Dialog dialog = new Dialog(StopDetailsActivity.this);
				dialog.setTitle(R.string.app_name);
				dialog.setContentView(R.layout.notification_dialog);

				final Button notificationBtn = (Button) dialog.findViewById(R.id.setNotifBtn);

				// set notification dialog click implementation
				notificationBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(final View v) {
						final EditText notificationText = (EditText) dialog.findViewById(R.id.txtNotification);
						final CharSequence inputText = notificationText.getText();

						if (inputIsNotValid(inputText)) {
							Toast.makeText(StopDetailsActivity.this, R.string.dialog_error_msg, Toast.LENGTH_LONG).show();
							return;
						}

						final int minutesToAdd = Integer.parseInt(inputText.toString());
						final Calendar currentDateTime = calculateAlarmDateTime(position, minutesToAdd);

						final Intent intent = new Intent(StopDetailsActivity.this, TramNotification.class);

						final PendingIntent pendingIntent = PendingIntent.getBroadcast(StopDetailsActivity.this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

						final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
						alarmManager.set(AlarmManager.RTC_WAKEUP, currentDateTime.getTimeInMillis(), pendingIntent);

						dialog.dismiss();

						final CharSequence message = getString(R.string.dialog_notification_trigger_msg, minutesToAdd);
						Toast.makeText(StopDetailsActivity.this, message, Toast.LENGTH_LONG).show();
					}

					private boolean inputIsNotValid(final CharSequence inputMinutes) {
						if (null == inputMinutes) {
							return true;
						}

						final String inputMinStr = inputMinutes.toString();
						return !inputMinStr.matches("\\d{1,2}");
					}

					private Calendar calculateAlarmDateTime(final int chosenTramListIndex, final int minutesToAdd) {
						final Calendar currentDateTime = new GregorianCalendar();

						final NextTram tram = mNextTrams.get(chosenTramListIndex);
						currentDateTime.setTime(tram.getPredictedArrivalDateTime());
						currentDateTime.add(Calendar.MINUTE, minutesToAdd * -1);

						Log.d(TAG, "Scheduling to: " + currentDateTime);
						return currentDateTime;
					}

				});

				dialog.show();
				return true;
			}

		});

		// Preferences
		mPreferenceHelper = new PreferenceHelper();

		// Get bundle data
		int routeId = -1;
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
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
		final String title = mStop.getStopName();
		actionBar.setTitle(title);

		// Display stop data
		displayStop(mStop);

		// Get our TramTracker service, either SOAP (def) or JSON
		if (mPreferenceHelper.isJSONAPIEnabled())
			ttService = new TramTrackerServiceJSON();
		else
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

		if (mRefreshThread.isInterrupted())
			mRefreshThread.start();

		// Force update of tram times to prevent stale times if the app
		// is opened again
		mShowDialog = true;
		getNextTramTimes();

	}

	/**
	 * Options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.stop_details, menu);

		// Get our refresh item for animating later
		mRefreshItem = menu.findItem(R.id.menu_refresh);
		mFavouriteItem = menu.findItem(R.id.menu_favourite);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Set favourite/unfavourite text for menu item
		boolean isFavourite = mFavouriteList.isFavourite(new Favourite(mStop, mRoute));

		// Set the favourite/unfavourite item icon
		mFavouriteItem.setIcon(isFavourite ? R.drawable.ic_action_star : R.drawable.ic_action_unstar);

		// Set the favourite/unfavourite item label
		mFavouriteItem.setTitle(isFavourite ? R.string.menu_item_unfavourite : R.string.menu_item_favourite);

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Menu actions
	 * 
	 * @param menuItem
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
			{

			case R.id.menu_favourite:
				toggleFavourite();
				return true;

			case R.id.menu_map:
				showMap();
				return true;

			case R.id.menu_refresh:
				mShowDialog = true; // Show the 'loading' view if specifically
									// clicked
				getNextTramTimes();
				return true;

			case android.R.id.home:
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
			}
	}

	/**
	 * Toggle favourite stop status
	 */
	private void toggleFavourite() {

		// Build a new favourite object
		Favourite favourite = new Favourite(mStop, mRoute);
		mFavouriteList.toggleFavourite(favourite);

		boolean isFavourite = mFavouriteList.isFavourite(favourite);

		// Set the favourite/unfavourite item icon
		mFavouriteItem.setIcon(isFavourite ? R.drawable.ic_action_star : R.drawable.ic_action_unstar);

		// Set the favourite/unfavourite item label
		mFavouriteItem.setTitle(isFavourite ? R.string.menu_item_favourite : R.string.menu_item_favourite);

		// Toast message
		int toastMessage = isFavourite ? R.string.toast_favourite_added : R.string.toast_favourite_removed;
		Toast.makeText(StopDetailsActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
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
			StopsList mStopList = new StopsList();
			mStopList.add(mStop);
			bundle.putParcelable("stopslist", mStopList);
			final Intent intent = new Intent(StopDetailsActivity.this, StopMapActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
		} catch (Exception e) {
			Toast.makeText(this, "Google Maps are not available", Toast.LENGTH_LONG).show();
		};
	}

	/**
	 * Update refresh status icon/views
	 */
	private void showLoadingView(boolean isRefreshing) {

		// The 'No Results' view or the stop times list
		if (mListAdapter.getCount() < 2) {
			// Only 1 entry means an error, so make it < 2
			mListView.getEmptyView().setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
			findViewById(R.id.departures_list).setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
		} else {
			mListView.setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
			findViewById(R.id.departures_list).setVisibility(isRefreshing ? View.GONE : View.VISIBLE);
		}

		// Loading screen
		findViewById(R.id.departures_loading).setVisibility(isRefreshing ? View.VISIBLE : View.GONE);

		// Refresh spinner in Action Bar
		if (mRefreshItem != null) {
			if (isRefreshing) {
				if (mRefreshIndeterminateProgressView == null) {
					LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					mRefreshIndeterminateProgressView = inflater.inflate(R.layout.actionbar_progress, mListView, false);
				}
				mRefreshItem.setActionView(mRefreshIndeterminateProgressView);
			} else {
				mRefreshItem.setActionView(null);
			}
		}
	}

	/**
	 * Display the details for a given stop
	 * 
	 * @param stop
	 */
	private void displayStop(Stop stop) {

		// Set labels from Stop hash map
		String stopName = stop.getPrimaryName();
		String stopDetails = stop.getStopDetailsLine();
		String stopRoutes = stop.getRoutesString();

		((TextView) findViewById(R.id.stopNameTextView)).setText(stopName);
		((TextView) findViewById(R.id.stopDetailsTextView)).setText(stopDetails);
		((TextView) findViewById(R.id.stopRoutesTextView)).setText(stopRoutes);

		final List<Route> mRoutes = mDB.getRoutesForStop(mTramTrackerId);
		stop.setRoutes(mRoutes);

		// If we have more than one route for this stop, then show the spinner
		if (mRoutes.size() > 1) {
			mRoutesSpinner = (Spinner) findViewById(R.id.routeSelectSpinner);
			mRoutesSpinner.setVisibility(View.VISIBLE);
			mAdapterForSpinner = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
			mAdapterForSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mRoutesSpinner.setAdapter(mAdapterForSpinner);

			// Add 'All Routes' option
			mAdapterForSpinner.add("All Routes");

			Route r;
			for (int i = 0; i < mRoutes.size(); i++) {
				r = mRoutes.get(i);
				mAdapterForSpinner.add("Route " + r.getNumber());

				// If we have a route already (e.g. passed from Favourites
				// activity, then
				// select it right now
				if (mRoute != null) {
					if (mRoute.getId() == r.getId()) {
						// i+1 because we have 'All Route' in position 0
						mRoutesSpinner.setSelection(i + 1);
					}
				}

			}

			mRoutesSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (id == 0) {
						mRoute = null;
					} else {
						// -1 for the offset of having 'All Routes' first item
						mRoute = mRoutes.get(position - 1);
						if (LOGV) Log.v(TAG, "Route selected: " + mRoute);
					}

					// Set favourite/unfavourite text for menu item
					boolean isFavourite = mFavouriteList.isFavourite(new Favourite(mStop, mRoute));

					// Set the favourite/unfavourite item icon
					mFavouriteItem.setIcon(isFavourite ? R.drawable.ic_action_star : R.drawable.ic_action_unstar);

					// Set the favourite/unfavourite item label
					mFavouriteItem.setTitle(isFavourite ? R.string.menu_item_unfavourite : R.string.menu_item_favourite);

					mShowDialog = true;
					getNextTramTimes();
				}

				public void onNothingSelected(AdapterView<?> parent) {
					mRoute = null;
				}
			});
		}
	}

	/**
	 * Countdown thread for 60-sec refresh
	 */
	private class CountDown implements Runnable {
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				Message m = new Message();
				UpdateHandler.sendMessage(m);
				try {
					// 60 Seconds
					Thread.sleep(REFRESH_SECONDS * 1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * Helper function for the background task of GetNextTramTimes
	 */
	private void getNextTramTimes() {
		// Really dumb check that we're not starting two update threads
		// at once.
		if (!mIsRefreshing)
			new GetNextTramTimes().execute();
	}

	/**
	 * Background task for fetching tram times
	 */
	private class GetNextTramTimes extends AsyncTask<NextTram, Void, List<NextTram>> {

		@Override
		protected void onPreExecute() {
			mIsRefreshing = true;
			if (mShowDialog)
				showLoadingView(true);
		}

		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {
			if (LOGV)
				Log.v(TAG, "Fetching getNextPredictedRoutesCollection...");
			try {
				mDB = new TramHunterDB();

				// Get out next trams
				mNextTrams = ttService.getNextPredictedRoutesCollection(mStop, mRoute);

				for (int i = 0; i < mNextTrams.size(); i++) {
					NextTram thisTram = mNextTrams.get(i);

					// Fetch our tram class from the DB
					int vehicleNumber = thisTram.getVehicleNo();
					if (vehicleNumber > 0) {
						String tramClass = mDB.getTramClass(vehicleNumber);
						thisTram.setTramClass(tramClass);
					}

					// Remove any entries that show 'anyType{}' - they are
					// errors
					if (thisTram.getDestination().matches("anyType")) {
						mNextTrams.remove(i);
					}
				}

				mDB.close();
			} catch (TramTrackerServiceException e) {
				// Retry a couple of times before error
				if (mErrorRetry < MAX_ERRORS) {
					mErrorRetry++;
					if (LOGV)
						Log.e(TAG, "Error " + mErrorRetry + " of " + MAX_ERRORS + ": " + e);
					this.doInBackground(params);
				} else {
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
				if (mFirstDepartureReqest) {
					final CharSequence message = getString(R.string.dialog_error_fetching, mErrorMessage);
					Toast.makeText(StopDetailsActivity.this, message, Toast.LENGTH_LONG).show();
				}
				mErrorMessage = null; // Reset
				mErrorRetry = 0;

				// Stop our refresh thread because we had errors
				mRefreshThread.interrupt();
			} else {
				boolean noResults = true;

				if (nextTrams.size() > 0) {
					// Start the refresh thread if stopped
					if (mRefreshThread.isInterrupted()) {
						mRefreshThread.start();
					}

					// Sort trams by minutesAway
					Collections.sort(nextTrams);

					// Show trams list, only if more than one tram.
					if (nextTrams.size() > 1) {
						noResults = false;
						setListAdapter(mListAdapter);
					}

					String specialEventMessage = nextTrams.get(0).getSpecialEventMessage();
					if (specialEventMessage.length() > 10) {
						findViewById(R.id.special_event).setVisibility(View.VISIBLE);
						((TextView) findViewById(R.id.special_event_message)).setText(specialEventMessage);
					} else {
						findViewById(R.id.special_event).setVisibility(View.GONE);
					}
				}

				if (noResults) {
					mListView.getEmptyView().setVisibility(noResults ? View.VISIBLE : View.GONE);
					mListView.setVisibility(noResults ? View.VISIBLE : View.GONE);
					findViewById(R.id.departures_list).setVisibility(noResults ? View.VISIBLE : View.GONE);
				}
			}

			// Hide the loading spinners
			mIsRefreshing = false;
			showLoadingView(false);
		}
	}


	/**
	 * Create a NextTramsListAdapter for showing our next trams
	 */
	private class NextTramsListAdapter extends BaseAdapter {

		public int getCount() {
			return mNextTrams.size();
		}

		public Object getItem(int position) {
			return mNextTrams.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.stop_details_row, parent, false);
			} else {
				pv = convertView;
			}

			NextTram thisTram = (NextTram) mNextTrams.get(position);
			((TextView) pv.findViewById(R.id.routeNumber)).setText(thisTram.getRouteNo());
			((TextView) pv.findViewById(R.id.routeDestination)).setText(thisTram.getDestination());
			((TextView) pv.findViewById(R.id.nextTime)).setText(thisTram.humanMinutesAway());

			ImageView tramClassView = (ImageView) pv.findViewById(R.id.tramClass);

			if (mPreferenceHelper.isTramImageEnabled()) {
				String tramClass = thisTram.getTramClass();
				String tramClassImage = UIUtils.getTramImage(tramClass);

				if (tramClassImage != null) {
					int resID = getResources().getIdentifier(tramClassImage, "drawable", getPackageName());
					tramClassView.setPadding(3, 5, 3, 3);
					tramClassView.setBackgroundResource(resID);
					tramClassView.setVisibility(View.VISIBLE);
				} else {
					tramClassView.setVisibility(View.GONE);
				}

			}

			return pv;
		}
	}

}