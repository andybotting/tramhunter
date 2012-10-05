/*  
 * Copyright 2012 Andy Botting <andy@andybotting.com>
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

import java.util.Date;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.TramRun;
import com.andybotting.tramhunter.objects.TramRunTime;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceException;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class TramRunActivity extends SherlockListActivity {

	private static final String TAG = "TramRunActivity";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);

	private int mVehicleNumber;
	private TramRun mTramRun;
	private volatile Thread mRefreshThread;

	private ListAdapter mListAdapter;
	private ListView mListView;
	
	private PreferenceHelper mPreferenceHelper;
	
	private TramTrackerService ttService;

	private String mErrorMessage = null;
	private int mErrorRetry = 0;
	private final int MAX_ERRORS = 2;
	private boolean mFirstDepartureReqest = true;

	private static final int REFRESH_SECONDS = 60;

	// Menu items
	private MenuItem mRefreshItem;
	private View mRefreshIndeterminateProgressView; // save inflated layout for reference

	// Refresh
	private boolean mShowDialog = true;
	private boolean mIsRefreshing = false;

	// Handle the timer
	Handler UpdateHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (!mFirstDepartureReqest)
				mShowDialog = false;
			getTramRun();
		}
	};

	/**
	 * On create of this class
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.tram_run);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Preferences
		mPreferenceHelper = new PreferenceHelper();
		
		// Set up our list
		mListAdapter = new NextTramsListAdapter();
		mListView = getListView();
		mListView.setVisibility(View.GONE);

		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mVehicleNumber = extras.getInt("vehicleNumber");
			
			// Throw an error if we don't actually have a vehicle ID
			if (mVehicleNumber <= 0) {
				Toast.makeText(this, "Stop details are not available for this service.", Toast.LENGTH_LONG).show();
				finish();
			}
		}

		// Set the title
		//final String title = mStop.getStopName();
		String title = "Stops for Tram #" + mVehicleNumber;
		actionBar.setTitle(title);

		// Display stop data
		displayTramRun();

		ttService = new TramTrackerServiceSOAP();

		// Our thread for updating the stops every 60 secs
		mRefreshThread = new Thread(new CountDown());
		mRefreshThread.setDaemon(true);
		mRefreshThread.start();

	}

	@Override
	public void onStop() {
		super.onStop();
		mRefreshThread.interrupt();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mRefreshThread.isInterrupted()) {
			mRefreshThread.start();
		}
	}

	/**
	 * Options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.tram_run, menu);

		// Get our refresh item for animating later
		mRefreshItem = menu.findItem(R.id.menu_refresh);

		return super.onCreateOptionsMenu(menu);
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

			case R.id.menu_refresh:
				mShowDialog = true; // Show the 'loading' view if specifically
									// clicked
				getTramRun();
				return true;

			case android.R.id.home:
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
			}
	}

	/**
	 * Update refresh status icon/views
	 */
	private void showLoadingView(boolean isRefreshing) {

		// Loading screen
		findViewById(R.id.departures_loading).setVisibility(isRefreshing ? View.VISIBLE : View.GONE);
		
		if (isRefreshing) {
			mListView.setVisibility(View.GONE);
			mListView.getEmptyView().setVisibility(View.GONE);
			findViewById(R.id.departures_loading).setVisibility(View.VISIBLE);
		}
		else {
			findViewById(R.id.departures_loading).setVisibility(View.GONE);
			// The 'No Results' view or the stop times list
			if (mListAdapter.getCount() > 0) {
				mListView.getEmptyView().setVisibility(View.GONE);
				mListView.setVisibility(View.VISIBLE);
			} else {
				mListView.setVisibility(View.VISIBLE);
				mListView.getEmptyView().setVisibility(View.GONE);
			}
		}


		// Refresh spinner in Action Bar
		if (mRefreshItem != null) {
			if (isRefreshing) {
				if (mRefreshIndeterminateProgressView == null) {
					LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					mRefreshIndeterminateProgressView = inflater.inflate(R.layout.actionbar_progress, null);
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
	private void displayTramRun() {

		mShowDialog = true;
		getTramRun();
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
	private void getTramRun() {
		// Really dumb check that we're not starting two update threads
		// at once.
		if (!mIsRefreshing)
			new GetTramRun().execute();
	}

	/**
	 * Background task for fetching tram times
	 */
	private class GetTramRun extends AsyncTask<TramRun, Void, TramRun> {
		
		@Override
		protected void onPreExecute() {
			mIsRefreshing = true;
			if (mShowDialog)
				showLoadingView(true);
		}

		@Override
		protected TramRun doInBackground(final TramRun... params) {
			if (LOGV)
				Log.v(TAG, "Fetching getNextPredictedRoutesCollection...");
			try {
				// Get out next trams
				mTramRun = ttService.getNextPredictedArrivalTimeAtStopsForTramNo(mVehicleNumber);

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
			return mTramRun;
		}

		@Override
		protected void onPostExecute(TramRun tramRun) {
			if (mErrorRetry == MAX_ERRORS) {

				// Toast: Error fetching departure information
				if (mFirstDepartureReqest) {
					final CharSequence message = getString(R.string.dialog_error_fetching, mErrorMessage);
					Toast.makeText(TramRunActivity.this, message, Toast.LENGTH_LONG).show();
				}
				mErrorMessage = null; // Reset
				mErrorRetry = 0;

				// Stop our refresh thread because we had errors
				mRefreshThread.interrupt();
			} else {
				if (tramRun.getTramRunTimeCount() > 0) {
					// Start the refresh thread if stopped
					if (mRefreshThread.isInterrupted()) {
						mRefreshThread.start();
					}

					setListAdapter(mListAdapter);

					// Reset the first departure request
					mFirstDepartureReqest = false;
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
		
		Date now = new Date();

		public int getCount() {
			if (mTramRun == null)
				return 0;
			return mTramRun.getTramRunTimeCount();
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
				pv = inflater.inflate(R.layout.tram_run_row, parent, false);
			} else {
				pv = convertView;
			}

			TramRunTime runTime = (TramRunTime) mTramRun.getTramRunTime(position);
			((TextView) pv.findViewById(R.id.stopName)).setText(runTime.getStop().getPrimaryName());

			// We factor in the clock offset here to fix the times. See the SOAP
			// provider for info about why we do this.
			long diff = runTime.getPredictedArrivalDateTime().getTime() - now.getTime() + mPreferenceHelper.getClockOffset();
			
			// +1 min to adjust for out of whack values, or we get 0 min for the first response
			int minutes = (int)diff/60000 + 1;
			
			((TextView) pv.findViewById(R.id.nextTime)).setText(minutes + " min");

			return pv;
		}
	}

}