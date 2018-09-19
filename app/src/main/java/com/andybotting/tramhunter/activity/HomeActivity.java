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

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.TramHunter;
import com.andybotting.tramhunter.TramHunterConstants;
import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.Tweet;
import com.andybotting.tramhunter.service.TwitterFeed;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.FavouriteStopUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static android.content.Context.LOCATION_SERVICE;

public class HomeActivity extends AppCompatActivity {

	private static final String TAG = "Home";

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
				// problem on upgrade, so we'll just leave it
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
			// String[] quotes = getResources().getStringArray(R.array.welcomeMessages);
			// InfoFragmentAdapter infoFragmentAdapter = new InfoFragmentAdapter(getSupportFragmentManager(), quotes);
			// mPager.setAdapter(infoFragmentAdapter);
			// updateRefreshStatus(INFO_SHOW);
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

				AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
				View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.vehicle_number_dialog, null, false);
				final EditText editText = (EditText) view.findViewById(R.id.editText);
				final DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int vehicleNumber = Integer.parseInt(editText.getText().toString().trim());
							Intent intent = new Intent(HomeActivity.this, TramRunActivity.class);
							intent.putExtra("vehicleNumber", vehicleNumber);
							startActivity(intent);
							if (which == DialogInterface.BUTTON_NEUTRAL) //from keyEvent, need to manually dismiss.
								dialog.dismiss();
						} catch (NumberFormatException e) {
							Toast.makeText(HomeActivity.this, R.string.enter_vehicle_number_error, Toast.LENGTH_SHORT).show();
						}
					}
				};
				builder.setView(view)
						.setTitle(R.string.enter_vehicle_number_hint)
						.setPositiveButton("Ok", positiveListener)
						.setNegativeButton("Cancel", null)
				.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if(event.getAction() == KeyEvent.ACTION_DOWN) switch (keyCode){
							case KeyEvent.KEYCODE_ENTER:
							case KeyEvent.KEYCODE_SEARCH:
								positiveListener.onClick(dialog, DialogInterface.BUTTON_NEUTRAL);
								return true;
						}
						return false;
					}
				});
				AlertDialog dialog = builder.create();
				dialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialogInterface) {
						editText.requestFocus();
						InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
						imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
					}
				});
				dialog.show();
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
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialog);
		View aboutView = View.inflate(dialogBuilder.getContext(), R.layout.dialog_about, null);
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

		MenuInflater inflater = getMenuInflater();
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
	 * Async task for updating Twitter feed
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
			time.setText(StringUtil.humanFriendlyDate(tweet.getDateLong()));

			// We're going to use our down twitter images for now.
			final Bitmap bitmap = BitmapFactory.decodeFile(tweet.getImagePath());
			image.setImageBitmap(bitmap);
			// image.setImageResource(R.drawable.yarratrams_twitter);

			// Handle onClick to Twitter
			View tweetLayout = v.findViewById(R.id.tweet_layout);
			tweetLayout.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String url = "http://twitter.com/" + tweet.getUsername();
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(url));
					startActivity(i);
				}
			});

			return v;
		}
	}

	/**
	 * Fragment adapter for holding quotes
	 */
	public static class QuoteFragmentAdapter extends FragmentStatePagerAdapter {

		String[] quotes;
		Integer[] shuffledInts;

		public QuoteFragmentAdapter(FragmentManager fm, String[] quotes) {
			super(fm);
			this.quotes = quotes;

			// Shuffle an array of Integers for 'randomising' the quotes
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
			return QuoteArrayFragment.newInstance(quotes[quoteNumber], title);
		}
	}

	/**
	 * Quote fragment
	 */
	public static class QuoteArrayFragment extends Fragment {
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
