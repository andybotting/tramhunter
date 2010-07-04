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
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.util.FavouriteStopUtil;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class HomeActivity extends ListActivity {

	// Menu items
	private static final int MENU_ABOUT = 0;
	private static final int MENU_SEARCH = 1;
	private static final int MENU_SETTINGS = 2;
	
	// Menu list items
	private static final int MENU_LIST_FAVOURITE = 0;
	private static final int MENU_LIST_BROWSE = 1;
	private static final int MENU_LIST_ENTERTTID = 2;
	private static final int MENU_LIST_NEARBY = 3;
	private static final int MENU_LIST_SEARCH = 4;
	
	private static final int NUMBER_OF_MENU_ITEMS = 5;

	// Menu list arrays
	private String[] mMenuItems = new String[NUMBER_OF_MENU_ITEMS];
	private String[] mMenuItemsDesc = new String[NUMBER_OF_MENU_ITEMS];
	
	private ListView mListView;
	private PreferenceHelper mPreferenceHelper;
	private TramHunterDB mDB;
	private FavouriteStopUtil mFavouriteStopUtil;
	private static Resources res;
	private static String mLastUsedIntentUUID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.home);
		res = getResources();
		
		// Get shared prefs
		mPreferenceHelper = new PreferenceHelper(this);	
		
		// Create db instance
		mDB = new TramHunterDB(this);
		final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mFavouriteStopUtil = new FavouriteStopUtil(mDB, locationManager);
    	
		// Test our intent action in case it's a search
		Intent intent = getIntent();
		
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // from click on search results
            int tramTrackerId = Integer.parseInt(intent.getDataString());
            Intent next = new Intent();
            next.setClass(this, StopDetailsActivity.class);
            next.putExtra("tramTrackerId", tramTrackerId);
            startActivity(next);
            finish();
        } 
        else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);      
            Intent next = new Intent();
            next.setClass(this, StopsListActivity.class);
            next.putExtra("search_query", query);
            startActivity(next);
            finish();
        }
        else {
			if (isFirstLaunch()) {
				showAbout();
			}
			goDefaultLaunchActivity();
        }
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
			
			// TODO: We really need unit testing for this stuff
			if(activityName.equals("HomeActivity")){
				intent = null;
				
			}else if(activityName.equals("StopsListActivity")){
				intent = new Intent(HomeActivity.this, StopsListActivity.class);
				
			}else if(activityName.equals("ClosestStopsListActivity")){
				Stop closestFavouriteStop = mFavouriteStopUtil.getClosestFavouriteStop();

				if (closestFavouriteStop != null) {
					// Go to the closest favourite stop
					Bundle bundle = new Bundle();
					bundle.putInt("tramTrackerId", closestFavouriteStop.getTramTrackerID());
					intent = new Intent(HomeActivity.this, StopDetailsActivity.class);
					intent.putExtras(bundle);
				}else{
					GenericUtil.popToast(this, "Unable to determine closest favourite stop!");
				}
				
			}else if(activityName.equals("NearStopsActivity")){
				intent = new Intent(HomeActivity.this, NearStopsActivity.class);
				
			}else if(activityName.equals("PredictionActivity")){
				intent = new Intent(HomeActivity.this, PredictionActivity.class);
			}
			
			if(intent!=null)
				startActivityForResult(intent, 1);
		}
		
		// Business as usual on return
		showHomeMenu();
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		// Comment out here to only show messages each time the app is opened.
		setRandomWelcomeMessage();
	}
	
	public boolean isFirstLaunch() {
		TramHunterDB db = new TramHunterDB(this);
		return !db.checkFirstLaunch();
	}

	private String getRandomWelcomeMessage(){
		Random r = new Random(System.currentTimeMillis());
		String[] welcomeMessages = res.getStringArray(R.array.welcomeMessages);
		return welcomeMessages[r.nextInt(welcomeMessages.length - 1)];
	}
	
	private void setRandomWelcomeMessage() {
		TextView welcomeMessageTextView = (TextView) findViewById(R.id.welcomeMessage);
		String welcomeText = "";
		
		if (mPreferenceHelper.isWelcomeQuoteEnabled())
			welcomeText = "\"" + getRandomWelcomeMessage()+ "\"";
		
        welcomeMessageTextView.setText(welcomeText);
    }
	
	public void showAbout() {
		// Get the package name
		String heading = getResources().getText(R.string.app_name) + "\n";
		
        // Get the package version
        PackageManager pm = getPackageManager();
        try {
			PackageInfo pi = pm.getPackageInfo("com.andybotting.tramhunter", 0);
			heading += "v" + pi.versionName + "\n\n";
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		// Build alert dialog
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(heading);
		dialogBuilder.setMessage(getResources().getText(R.string.about_msg));
		dialogBuilder.setPositiveButton("OK",
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					TramHunterDB db = new TramHunterDB(getBaseContext());
					db.setFirstLaunch();
					db.close();
				}
			});
		dialogBuilder.setCancelable(false);
		dialogBuilder.setIcon(R.drawable.icon);
		dialogBuilder.show();
	}
	
	public void showHomeMenu() { 
		setContentView(R.layout.home);
		
		setRandomWelcomeMessage();
		
		mListView = (ListView)this.findViewById(android.R.id.list);
		setTitle(getResources().getText(R.string.app_name));

		mListView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
				Intent intent = null;
				Context context = getBaseContext();
								
				switch ( (int)id ) {
					case MENU_LIST_FAVOURITE:
						intent = new Intent(context, StopsListActivity.class);
						break;
					case MENU_LIST_BROWSE:
						intent = new Intent(context, RoutesListActivity.class);
						break;
					case MENU_LIST_ENTERTTID:
						intent = new Intent(context, EnterTTIDActivity.class);
						break;
					case MENU_LIST_NEARBY:
						intent = new Intent(context, NearStopsActivity.class);
						break;
					case MENU_LIST_SEARCH:
						onSearchRequested();
						break;
				}
				
				if(intent != null)
					startActivityForResult(intent, 1);
  
			}
								
		});
		
		
		// Build our menu items
	    mMenuItems[MENU_LIST_FAVOURITE] = res.getString(R.string.menu_list_favourite);
	    mMenuItemsDesc[MENU_LIST_FAVOURITE] = res.getString(R.string.menu_list_favourite_desc);
		
	    mMenuItems[MENU_LIST_BROWSE] = res.getString(R.string.menu_list_browse);
	    mMenuItemsDesc[MENU_LIST_BROWSE] = res.getString(R.string.menu_list_browse_desc);
	    
	    mMenuItems[MENU_LIST_ENTERTTID] = res.getString(R.string.menu_list_enterttid);
	    mMenuItemsDesc[MENU_LIST_ENTERTTID] = res.getString(R.string.menu_list_enterttid_desc);
	    
	    mMenuItems[MENU_LIST_NEARBY] = res.getString(R.string.menu_list_nearby);
	    mMenuItemsDesc[MENU_LIST_NEARBY] = res.getString(R.string.menu_list_nearby_desc);
	    
	    mMenuItems[MENU_LIST_SEARCH] = res.getString(R.string.menu_list_search);
	    mMenuItemsDesc[MENU_LIST_SEARCH] = res.getString(R.string.menu_list_search_desc);
		
		
        // Set up our adapter
		setListAdapter(new MenuListAdapter());
	}

	private class MenuListAdapter extends BaseAdapter {

		public int getCount() {
			return mMenuItems.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
				
			View pv = convertView;
			ViewWrapper wrapper = null;

			LayoutInflater inflater = getLayoutInflater();
			pv = inflater.inflate(R.layout.home_row, parent, false);
					
			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);
									
			wrapper.getNameText().setText(mMenuItems[position]);
			wrapper.getDescriptionText().setText(mMenuItemsDesc[position]);

			return pv;
		}

	}
	
	class ViewWrapper {
		View base;
				
		TextView m_nameText = null;
		TextView m_descriptionText = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getNameText() {
			if (m_nameText == null) {
				m_nameText = (TextView) base.findViewById(R.id.nameText);
			}
			return (m_nameText);
		}

		TextView getDescriptionText() {
			if (m_descriptionText == null) {
				m_descriptionText = (TextView) base.findViewById(R.id.descriptionText);
			}
			return (m_descriptionText);
		}

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
			Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
			startActivityForResult(intent, 1);
			return true;
		}
		return false;

	}

	
	private void checkStats() {	
		mPreferenceHelper = new PreferenceHelper(getBaseContext());
		if (mPreferenceHelper.isSendStatsEnabled()) {
			long lastStatsDate = mDB.getStatsDate();
			long now = System.currentTimeMillis();
			// Only once a week
			if ((now - lastStatsDate) > 604800000) {
				new Thread() {
					public void run() {
						uploadStats();
					}
				}.start();
				mDB.setStatsDate();
			}
		}
	}

	private void uploadStats() {
		Log.d("Testing", "Sending app statistics");

		// gather all of the device info
		PackageManager pm = getPackageManager();
		String app_version = "";
		try {
			try {
				PackageInfo pi = pm.getPackageInfo("com.andybotting.tramhunter", 0);
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
			pairs.add(new BasicNameValuePair("device_version", device_version));
			pairs.add(new BasicNameValuePair("device_language", device_language));
			pairs.add(new BasicNameValuePair("mobile_country_code", mobile_country_code));
			pairs.add(new BasicNameValuePair("mobile_network_number", mobile_network_number));
			pairs.add(new BasicNameValuePair("mobile_network_type",	mobile_network_type));

			try {
				post.setEntity(new UrlEncodedFormEntity(pairs));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				HttpResponse response = client.execute(post);
				int responseCode = response.getStatusLine().getStatusCode();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
    
}
