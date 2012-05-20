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

import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
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
import com.andybotting.tramhunter.objects.Tweet;
import com.andybotting.tramhunter.service.TwitterFeed;

import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.FavouriteStopUtil;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
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

		checkStats();

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
			new FetchTweets().execute();
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
		String heading = getResources().getText(R.string.app_name) + "\n";

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			heading += "v" + pi.versionName + "\n\n";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// Build alert dialog
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(heading);
		View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, null);
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
	 * Async task for updating tube status data
	 */
	private class FetchTweets extends AsyncTask<Void, Void, ArrayList<Tweet>> {

		protected void onPreExecute() {
			updateRefreshStatus(INFO_LOADING);
		}

		@Override
		protected ArrayList<Tweet> doInBackground(Void... unused) {

			ArrayList<Tweet> tweets = null;
			TwitterFeed twitter = new TwitterFeed();

			try {
				Log.v(TAG, "Fetching Tweets...");
				tweets = twitter.getTweets();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return tweets;
		}

		protected void onPostExecute(ArrayList<Tweet> tweets) {
			if (tweets == null) {
				Log.w(TAG, "Error fetching Tweets");
				updateRefreshStatus(INFO_ERROR);
			} else {
				// Only if we're showing the panel
				if (mPager != null) {
					InfoFragmentAdapter infoFragmentAdapter = new InfoFragmentAdapter(getSupportFragmentManager(), tweets);
					mPager.setAdapter(infoFragmentAdapter);
					updateRefreshStatus(INFO_SHOW);
				}
			}
		}
	}

	/**
	 * Fragment adapter for holding tweets
	 */
	public static class InfoFragmentAdapter extends FragmentStatePagerAdapter {

		ArrayList<Tweet> tweets;

		public InfoFragmentAdapter(FragmentManager fm, ArrayList<Tweet> tweets) {
			super(fm);
			this.tweets = tweets;
		}

		@Override
		public int getCount() {
			return tweets.size();
		}

		@Override
		public Fragment getItem(int position) {
			return ArrayFragment.newInstance(tweets.get(position));
		}
	}

	/**
	 * Tweet fragment
	 */
	public static class ArrayFragment extends Fragment {
		Tweet tweet;

		static ArrayFragment newInstance(Tweet tweet) {
			ArrayFragment f = new ArrayFragment();
			Bundle args = new Bundle();

			args.putString("name", tweet.getName());
			args.putString("username", tweet.getUsername());
			args.putString("message", tweet.getMessage());
			args.putLong("date", tweet.getDateLong());
			args.putString("imagePath", tweet.getImagePath());

			f.setArguments(args);
			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			tweet = new Tweet();
			tweet.setName(getArguments().getString("name"));
			tweet.setUsername(getArguments().getString("username"));
			tweet.setMessage(getArguments().getString("message"));
			tweet.setDate(getArguments().getLong("date"));
			tweet.setImagePath(getArguments().getString("imagePath"));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.tweet_fragment, container, false);

			TextView name = (TextView) v.findViewById(R.id.tweet_name);
			TextView username = (TextView) v.findViewById(R.id.tweet_username);
			TextView message = (TextView) v.findViewById(R.id.tweet_message);
			TextView time = (TextView) v.findViewById(R.id.tweet_time);
			ImageView image = (ImageView) v.findViewById(R.id.tweet_image);

			name.setText(tweet.getName());
			username.setText("@" + tweet.getUsername());
			message.setText(tweet.getMessage());
			time.setText(tweet.twitterHumanFriendlyDate());

			// We're going to use our down twitter images for now.
			// final Bitmap bitmap =
			// BitmapFactory.decodeFile(tweet.getImagePath());
			// image.setImageBitmap(bitmap);
			image.setImageResource(R.drawable.yarratrams_twitter);

			return v;
		}
	}

	/**
	 * Check last time stats were sent, and send again if time greater than a
	 * week
	 */
	private void checkStats() {
		if (mPreferenceHelper.isSendStatsEnabled()) {
			long statsTimestamp = mPreferenceHelper.getStatsTimestamp();
			long timeDiff = UIUtils.dateDiff(statsTimestamp);

			if (LOGV)
				Log.v(TAG, "Lasts stats date was " + timeDiff + "ms ago");

			// Only once a week
			if (timeDiff > 604800000) {
				new Thread() {
					public void run() {
						uploadStats();
					}
				}.start();
				mPreferenceHelper.setStatsTimestamp();
			}
		}
	}

	/**
	 * Upload statistics
	 */
	private void uploadStats() {
		Log.d("Testing", "Sending app statistics");

		// gather all of the device info
		String app_version = "";
		try {
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
				app_version = pi.versionName;
			} catch (NameNotFoundException e) {
				app_version = "N/A";
			}

			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String device_uuid = tm.getDeviceId();
			String device_id = "00000000000000000000000000000000";
			if (device_uuid != null) {
				device_id = GenericUtil.MD5(device_uuid);
			}

			String mobile_country_code = tm.getNetworkCountryIso();
			String mobile_network_number = tm.getNetworkOperator();
			int network_type = tm.getNetworkType();

			// get the network type string
			String mobile_network_type = "N/A";
			switch (network_type)
				{
				case 0:
					mobile_network_type = "TYPE_UNKNOWN";
					break;
				case 1:
					mobile_network_type = "GPRS";
					break;
				case 2:
					mobile_network_type = "EDGE";
					break;
				case 3:
					mobile_network_type = "UMTS";
					break;
				case 4:
					mobile_network_type = "CDMA";
					break;
				case 5:
					mobile_network_type = "EVDO_0";
					break;
				case 6:
					mobile_network_type = "EVDO_A";
					break;
				case 7:
					mobile_network_type = "1xRTT";
					break;
				case 8:
					mobile_network_type = "HSDPA";
					break;
				case 9:
					mobile_network_type = "HSUPA";
					break;
				case 10:
					mobile_network_type = "HSPA";
					break;
				}

			String device_version = android.os.Build.VERSION.RELEASE;

			if (device_version == null) {
				device_version = "N/A";
			}

			String device_model = android.os.Build.MODEL;

			if (device_model == null) {
				device_model = "N/A";
			}

			String device_language = getResources().getConfiguration().locale.getLanguage();
			String home_function = mPreferenceHelper.defaultLaunchActivity();
			String welcome_message = String.valueOf(mPreferenceHelper.isWelcomeQuoteEnabled());

			// post the data
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost("http://tramhunter.andybotting.com/stats/app/send");
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("device_id", device_id));
			pairs.add(new BasicNameValuePair("app_version", app_version));
			pairs.add(new BasicNameValuePair("home_function", home_function));
			pairs.add(new BasicNameValuePair("welcome_message", welcome_message));
			pairs.add(new BasicNameValuePair("device_model", device_model));
			pairs.add(new BasicNameValuePair("device_version", device_version));
			pairs.add(new BasicNameValuePair("device_language", device_language));
			pairs.add(new BasicNameValuePair("mobile_country_code", mobile_country_code));
			pairs.add(new BasicNameValuePair("mobile_network_number", mobile_network_number));
			pairs.add(new BasicNameValuePair("mobile_network_type", mobile_network_type));

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
