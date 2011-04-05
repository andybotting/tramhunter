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

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.TramHunter;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.map.AndroidBigImage;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.FavouriteStopUtil;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class HomeActivity extends Activity {

    private static final String TAG = "Home";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.DEBUG);
	
	// Menu items
	private static final int MENU_ABOUT = 0;
	private static final int MENU_SEARCH = 1;
	private static final int MENU_SETTINGS = 2;
	
	private Context mContext;
	private PreferenceHelper mPreferenceHelper;
	
	private FavouriteStopUtil mFavouriteStopUtil;
	private static String mLastUsedIntentUUID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = getBaseContext();
		mPreferenceHelper = new PreferenceHelper(mContext);

        // Create db instance
		TramHunterDB db = new TramHunterDB(mContext);
		db.getDatabase();
		db.close();

        final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mFavouriteStopUtil = new FavouriteStopUtil(db, locationManager, mContext);
		
		// Show about dialog window on first launch (or just after an upgrade)
		if (mPreferenceHelper.isFirstLaunchThisVersion())
			showAbout();

		goDefaultLaunchActivity();
	}
	
	private void goDefaultLaunchActivity(){
		Bundle extras = getIntent().getExtras();
		boolean isNewIntentUUID = false;
		
		checkStats();
		
		// Check to make sure we have not already used the UUID for a default activity launch
		if(extras != null && extras.containsKey(TramHunter.KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH)) {
			String currentIntentUUID = extras.getString(TramHunter.KEY_PERFORM_DEFAULT_ACTIVITY_LAUNCH);
			isNewIntentUUID = !currentIntentUUID.equals(mLastUsedIntentUUID); 
			mLastUsedIntentUUID = currentIntentUUID;
		}
		
		if(isNewIntentUUID) {
			String activityName = mPreferenceHelper.defaultLaunchActivity();
			Intent intent = null;
			
			if(activityName.equals("HomeActivity")) {
				intent = null;
			}
			else if (activityName.equals("StopsListActivity")) {
				// Should be renamed to FavStopsListActivity, but causes a problem
				// on upgrade, so we'll just leave it
				startActivity(new Intent(this, FavStopsListActivity.class));
			}
			else if (activityName.equals("ClosestStopsListActivity")) {
				Stop closestFavouriteStop = mFavouriteStopUtil.getClosestFavouriteStop();
				if (closestFavouriteStop != null) {
					// Go to the closest favourite stop
					Bundle bundle = new Bundle();
					bundle.putInt("tramTrackerId", closestFavouriteStop.getTramTrackerID());
					intent = new Intent(HomeActivity.this, StopDetailsActivity.class);
					intent.putExtras(bundle);
				}
				else {
					GenericUtil.popToast(this, "Unable to determine closest favourite stop!");
				}
			}
			else if(activityName.equals("NearStopsActivity")) {
				startActivity(new Intent(this, NearStopsActivity.class));
			}
			
			if (intent != null)
				startActivityForResult(intent, 1);
		}
		
		setContentView(R.layout.home);
		
		// Search button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(HomeActivity.this);
		    }
		});	
		
		// Starred
		findViewById(R.id.home_btn_starred).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	startActivity(new Intent(HomeActivity.this, FavStopsListActivity.class));
		    }
		});
		
		// Browse
		findViewById(R.id.home_btn_browse).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	startActivity(new Intent(HomeActivity.this, RoutesListActivity.class));
		    }
		});
		
		// Nearby
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

		// Enter TTID
		findViewById(R.id.home_btn_map).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	startActivity(new Intent(HomeActivity.this, AndroidBigImage.class));
		    }
		});

		// Search
		findViewById(R.id.home_btn_settings).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
		    }
		});
		
		// Show or hide the welcome quote
		View welcomeMessage = findViewById(R.id.welcomeMessage);
        if (welcomeMessage != null) {
			if(mPreferenceHelper.isWelcomeQuoteEnabled()) {
				findViewById(R.id.welcomeMessage).setVisibility(View.VISIBLE);
				setRandomWelcomeMessage();
			}
			else {
				findViewById(R.id.welcomeMessage).setVisibility(View.GONE);
			}
        }
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		setRandomWelcomeMessage();
	}

	private String getRandomWelcomeMessage(){
		Random r = new Random(System.currentTimeMillis());
		String[] welcomeMessages = getResources().getStringArray(R.array.welcomeMessages);
		return welcomeMessages[r.nextInt(welcomeMessages.length - 1)];
	}
	
	private void setRandomWelcomeMessage() {
		TextView welcomeMessageTextView = (TextView) findViewById(R.id.welcomeMessageText);
		if (welcomeMessageTextView != null) {
			String welcomeText = "";
			welcomeText = '"' + getRandomWelcomeMessage()+ '"';
	        welcomeMessageTextView.setText(welcomeText);
		}
    }
	
	public void showAbout() {
		// Get the package name
		String heading = getResources().getText(R.string.app_name) + "\n";

		try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			heading += "v" + pi.versionName + "\n\n";
		} 
		catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		// Build alert dialog
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(heading);
		View aboutView = getLayoutInflater().inflate(R.layout.dialog_about, null);
		dialogBuilder.setView(aboutView);
		dialogBuilder.setPositiveButton("OK",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					mPreferenceHelper.setFirstLaunchThisVersion();
				}
			});
		dialogBuilder.setCancelable(false);
		dialogBuilder.setIcon(R.drawable.icon);
		dialogBuilder.show();
	}
	
	// Add menu items
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, MENU_ABOUT, 0, R.string.menu_item_about)
			.setIcon(android.R.drawable.ic_menu_help);

		menu.add(0, MENU_SETTINGS, 0, R.string.menu_item_settings)
			.setIcon(android.R.drawable.ic_menu_preferences);
		
        menu.add(0, MENU_SEARCH, 0, R.string.menu_item_search)
        	.setIcon(android.R.drawable.ic_search_category_default)
        	.setAlphabeticShortcut(SearchManager.MENU_KEY);

		return true;
	}
	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ABOUT:
			showAbout();
			return true;
        case MENU_SEARCH:
            onSearchRequested();
            return true;
		case MENU_SETTINGS:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return false;

	}

	
    /**
     * Check last time stats were sent, and send again if time greater than a week
     */
	private void checkStats() {	
		if (mPreferenceHelper.isSendStatsEnabled()) {
			long statsTimestamp = mPreferenceHelper.getStatsTimestamp();
	        long timeDiff = UIUtils.dateDiff(statsTimestamp);
	        
	        if (LOGV) Log.v(TAG, "Lasts stats date was " + timeDiff + "ms ago" );
			
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

	private void uploadStats() {
		Log.d("Testing", "Sending app statistics");

		// gather all of the device info
		String app_version = "";
		try {
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
				app_version = pi.versionName;
			} 
			catch (NameNotFoundException e) {
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
			switch (network_type) {
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
			pairs.add(new BasicNameValuePair("mobile_network_type",	mobile_network_type));

			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} 
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			try {
				HttpResponse response = client.execute(post);
				response.getStatusLine().getStatusCode();
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		} 
		catch (Exception e) {
			e.printStackTrace();
		}

	}
	
    
}
