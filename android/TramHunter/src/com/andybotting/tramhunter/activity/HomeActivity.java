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

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.TramHunter;

import com.andybotting.tramhunter.objects.Favourite;
//import com.andybotting.tramhunter.objects.Tweet;
//import com.andybotting.tramhunter.service.TwitterFeed;

import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.FavouriteStopUtil;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.StringUtil;
//import com.andybotting.tramhunter.util.StringUtil;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
//import android.net.Uri;
//import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends SherlockFragmentActivity {

	private static final String TAG = "Home";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.DEBUG);

	private PreferenceHelper mPreferenceHelper;
	private FavouriteStopUtil mFavouriteStopUtil;
	private static String mLastUsedIntentUUID;

	// Info window states
	private static final int INFO_LOADING = 0;
	private static final int INFO_SHOW = 1;
	private static final int INFO_ERROR = 2;

	private View mInfoLoadingView;
	private View mInfoErrorView;
	private ViewPager mPager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreferenceHelper = new PreferenceHelper();

		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mFavouriteStopUtil = new FavouriteStopUtil(locationManager);

		// Show about dialog window on first launch (or just after an upgrade)
		if (mPreferenceHelper.isFirstLaunchThisVersion())
			showAbout();

		goDefaultLaunchActivity();
	}

	private void goDefaultLaunchActivity() {
		Bundle extras = getIntent().getExtras();
		boolean isNewIntentUUID = false;

		// Check to make sure we have not already used the UUID for a default
		// activity launch
		if (extras != null && extras.containsKey(TramHunter.KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH)) {
			String currentIntentUUID = extras.getString(TramHunter.KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH);
			isNewIntentUUID = !currentIntentUUID.equals(mLastUsedIntentUUID);
			mLastUsedIntentUUID = currentIntentUUID;
		}

		if (isNewIntentUUID) {
			String activityName = mPreferenceHelper.defaultLaunchActivity();
			Intent intent = null;

			if (activityName.equals("HomeActivity")) {
				intent = null;
			} else if (activityName.equals("StopsListActivity")) {
				// Should be renamed to FavStopsListActivity, but causes a
				// problem
				// on upgrade, so we'll just leave it
				startActivity(new Intent(this, FavouriteActivity.class));
			} else if (activityName.equals("ClosestStopsListActivity")) {
				Favourite closestFavouriteStop = mFavouriteStopUtil.getClosestFavouriteStop();
				if (closestFavouriteStop != null) {
					// Go to the closest favourite stop
					Bundle bundle = new Bundle();

					bundle.putInt("tramTrackerId", closestFavouriteStop.getStop().getTramTrackerID());
					if (closestFavouriteStop.getRoute() != null)
						bundle.putInt("routeId", closestFavouriteStop.getRoute().getId());

					intent = new Intent(HomeActivity.this, StopDetailsActivity.class);
					intent.putExtras(bundle);
				} else {
					Toast.makeText(this, R.string.toast_unable_closest_stop, Toast.LENGTH_SHORT).show();
				}
			} else if (activityName.equals("NearStopsActivity")) {
				startActivity(new Intent(this, NearStopsActivity.class));
			}

			if (intent != null)
				startActivityForResult(intent, 1);
		}

		showMainHome();

	}

	/**
	 * Show the main home layout
	 */
	private void showMainHome() {
		setContentView(R.layout.home);

		// Set up the Action Bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle(R.string.app_name);

		mInfoLoadingView = findViewById(R.id.info_window_loading);
		mInfoErrorView = findViewById(R.id.info_window_error);
		mPager = (ViewPager) findViewById(R.id.pager);

		// If we have a pager view (i.e. portrait mode and we have the
		// settings enabled, then we'll show the twitter feed
		if ((mPager != null) && (mPreferenceHelper.isWelcomeQuoteEnabled())) {
			String[] quotes =  getResources().getStringArray(R.array.welcomeMessages);
			InfoFragmentAdapter infoFragmentAdapter = new InfoFragmentAdapter(getSupportFragmentManager(), quotes);
			mPager.setAdapter(infoFragmentAdapter);
			updateRefreshStatus(INFO_SHOW);
		}

		// Favourite Stops
		findViewById(R.id.home_btn_starred).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(HomeActivity.this, FavouriteActivity.class));
			}
		});

		// Browse Stops
		findViewById(R.id.home_btn_browse).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(HomeActivity.this, RoutesListActivity.class));
			}
		});

		// Nearby Stops
		findViewById(R.id.home_btn_nearby).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(HomeActivity.this, NearStopsActivity.class));
			}
		});

		// Search
		findViewById(R.id.home_btn_search).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				UIUtils.goSearch(HomeActivity.this);
			}
		});

		// Network Map
		findViewById(R.id.home_btn_map).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(HomeActivity.this, NetworkMapActivity.class));
			}
		});

		// Search Stops
		findViewById(R.id.home_btn_settings).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		// setRandomWelcomeMessage();
	}

	/**
	 * Return a random quote
	 */
	@SuppressWarnings("unused")
	private String getRandomWelcomeMessage() {
		Random r = new Random(System.currentTimeMillis());
		String[] welcomeMessages = getResources().getStringArray(R.array.welcomeMessages);
		return welcomeMessages[r.nextInt(welcomeMessages.length - 1)];
	}

	/**
	 * Show the about dialog
	 */
	public void showAbout() {
		// Get the package name
		String heading = getResources().getText(R.string.app_name).toString();

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			heading += " v" + pi.versionName + "";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// Build alert dialog, using a ContextThemeWrapper for proper theming
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialog));
		View aboutView = View.inflate(new ContextThemeWrapper(this, R.style.AlertDialog), R.layout.dialog_about, null);
		dialogBuilder.setTitle(heading);
		dialogBuilder.setView(aboutView);
		dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mPreferenceHelper.setFirstLaunchThisVersion();
			}
		});
		dialogBuilder.setCancelable(false);
		dialogBuilder.setIcon(R.drawable.icon);
		dialogBuilder.show();
	}

	/**
	 * Options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.home, menu);

		return true;
	}

	/**
	 * Options menu item select handler
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId())
			{
			case R.id.menu_about:
				showAbout();
				return true;

			case R.id.menu_search:
				onSearchRequested();
				return true;

			case R.id.menu_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			}
		return false;

	}

	/**
	 * Change UI widgets when updating status data
	 */
	private void updateRefreshStatus(int state) {

		if (mPager != null) {

			switch (state)
				{
				case INFO_LOADING:
					mInfoErrorView.setVisibility(View.GONE);
					mPager.setVisibility(View.GONE);
					mInfoLoadingView.setVisibility(View.VISIBLE);
					break;

				case INFO_SHOW:
					mInfoLoadingView.setVisibility(View.GONE);
					mInfoErrorView.setVisibility(View.GONE);
					mPager.setVisibility(View.VISIBLE);
					break;

				case INFO_ERROR:
					mInfoLoadingView.setVisibility(View.GONE);
					mPager.setVisibility(View.GONE);
					mInfoErrorView.setVisibility(View.VISIBLE);
					break;
				}
		}
	}

	/**
	 * Fragment adapter for holding tweets
	 */
	public static class InfoFragmentAdapter extends FragmentStatePagerAdapter {

		String[] quotes;
		Integer[] shuffledInts;

		public InfoFragmentAdapter(FragmentManager fm, String[] quotes) {
			super(fm);
			this.quotes = quotes;

			// Shuffle an array of Integers for 'randomzing' the quotes
			this.shuffledInts = new Integer[quotes.length];
			for (int i = 0; i < quotes.length; i++) {
				this.shuffledInts[i] = i;
			}
			Collections.shuffle(Arrays.asList(this.shuffledInts));
		}

		@Override
		public int getCount() {
			return quotes.length;
		}

		@Override
		public Fragment getItem(int position) {
			int quoteNumber = shuffledInts[position];
			String title = "Famous Tram Quote #" + (quoteNumber + 1);
			return ArrayFragment.newInstance(quotes[quoteNumber], title);
		}
	}

	/**
	 * Tweet fragment - unused for now
	 */
	public static class ArrayFragment extends Fragment {
		String quote;
		String title;

		static ArrayFragment newInstance(String quote, String title) {
			ArrayFragment f = new ArrayFragment();
			Bundle args = new Bundle();
			args.putString("quote", quote);
			args.putString("title", title);
			f.setArguments(args);
			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			quote = getArguments().getString("quote");
			title = getArguments().getString("title");
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.quote_fragment, container, false);
			TextView tvTitle = (TextView) v.findViewById(R.id.quote_title);
			TextView tvMessage = (TextView) v.findViewById(R.id.quote_message);
			ImageView ivImage = (ImageView) v.findViewById(R.id.quote_image);
	
			tvTitle.setText(title);
			tvMessage.setText(quote);
			ivImage.setImageResource(R.drawable.icon);
			return v;
		}
	}

}
