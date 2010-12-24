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
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class StopDetailsActivity extends ListActivity {
	
    private static final String TAG = "StopDetailsActivity";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.DEBUG);
		
	private final static int MENU_ITEM_REFRESH = 0;
	private final static int MENU_ITEM_FAVOURITE = 1;
	private final static int MENU_ITEM_MAP = 2;
	
	private TramHunterDB mDB;
	private CompoundButton mStarButton;

	private List<NextTram> mNextTrams = new ArrayList<NextTram>();
	private Stop mStop;
	private int mTramTrackerId;
    private volatile Thread mRefreshThread;
    
    boolean mLoadingError = false;
    boolean mShowDialog = true;
    
	private ListAdapter mListAdapter;
	private ListView mListView;
    
    private Context mContext;
    private PreferenceHelper mPreferenceHelper;
    private TramTrackerService ttService;
    
	private String mErrorMessage;
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
		mContext = getBaseContext();
		mPreferenceHelper = new PreferenceHelper(mContext);
		
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			mTramTrackerId = extras.getInt("tramTrackerId");	
		}  
		
		// Create out DB instance
		mDB = new TramHunterDB(this);
		mStop = mDB.getStop(mTramTrackerId);

		String title = mStop.getStopName();
		((TextView) findViewById(R.id.title_text)).setText(title);

		// Display stop data from DB
		displayStop(mStop);
		
		mStarButton = (CompoundButton)findViewById(R.id.stopStar);
		mStarButton.setChecked(mPreferenceHelper.isStarred(mTramTrackerId));

		mStarButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mPreferenceHelper.setStopStar(mTramTrackerId, mStarButton.isChecked());
			}
		});

		// Get our TramTracker service
		ttService = new TramTrackerServiceSOAP(mContext);
		
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
		
		if (mListAdapter.getCount() == 0)
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
	
	
	// Add settings to menu
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

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// Ensure the 'Favourite' menu item has the correct text
		menu.getItem(MENU_ITEM_FAVOURITE).setTitle((mPreferenceHelper.isStarred(mStop.getTramTrackerID()) ? "Unfavourite" : "Favourite"));
		return super.onMenuOpened(featureId, menu);
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) { 
		case MENU_ITEM_REFRESH:
			mShowDialog = true;
			new GetNextTramTimes().execute();
			return true;
		case MENU_ITEM_FAVOURITE:
			mStarButton = (CompoundButton)findViewById(R.id.stopStar);
			boolean isStarred = mPreferenceHelper.isStarred(mStop.getTramTrackerID());
			mPreferenceHelper.setStopStar(mTramTrackerId, !isStarred);
			mStarButton.setChecked(!isStarred);
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
		stop.setRoutes(mDB.getRoutesForStop(mTramTrackerId));
		
		String thirdLineText = stop.getRoutesString();
			
		((TextView)findViewById(R.id.stopNameTextView)).setText(firstLineText);
		((TextView)findViewById(R.id.stopDetailsTextView)).setText(secondLineText);
		((TextView)findViewById(R.id.stopRoutesTextView)).setText(thirdLineText);
		
	}
	
	
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
	
	private class GetNextTramTimes extends AsyncTask<NextTram, Void, List<NextTram>> {

		// Can use UI thread here
		@Override
		protected void onPreExecute() {
			// Show the spinner in the title bar
			if (mShowDialog)
				showLoadingView(true);
			
			showRefreshSpinner(true);
		}

		// Automatically done on worker thread (separate from UI thread)
		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {
			try {
				mNextTrams = ttService.getNextPredictedRoutesCollection(mStop);

				// We'll remove all NextTrams which are longer than
				// 300 minutes away - e.g. not running on weekend
				for(int i=0; i < mNextTrams.size(); i++) {
					if (mNextTrams.get(i).minutesAway() > 300) {
						mNextTrams.remove(i);
					}
				}
			} 
			catch (Exception e) {
				// Need some better exception handling
				if (mErrorRetry < MAX_ERRORS) {
					mErrorRetry++;
					//if (LOGV) Log.v(TAG, "Error " + mErrorRetry + " of " + MAX_ERRORS + ": " + e.getMessage());
					this.doInBackground(params);
				}
			}
			
			return mNextTrams;
		}

		// Can use UI thread here
		@Override
		protected void onPostExecute(List<NextTram> nextTrams) {
			Log.d("Testing", "onPostExecute");
        	if (mErrorRetry == MAX_ERRORS) {
            	// Display a toast with the error
        		String error = "Failed to fetch tram times";
        		UIUtils.popToast(mContext, error);
        		mErrorMessage = null;
        		mErrorRetry = 0;
        	}
        	else {
	
				if(nextTrams.size() > 0) {
					// Sort trams by minutesAway
					Collections.sort(nextTrams);
					mLoadingError = false;
					
					// > 10 because ksoap2 fills in anytype{} instead of null
					if (nextTrams.get(0).getSpecialEventMessage().length() > 10) {
						CharSequence text = nextTrams.get(0).getSpecialEventMessage();
						int duration = Toast.LENGTH_LONG;
						Toast toast = Toast.makeText(mContext, text, duration);
						toast.show();
					}
					
					if (nextTrams.size() > 1) {
						// Show trams list
						setListAdapter(new NextTramsListAdapter());
					}
				}

        		// Upload stats
        		if (mFirstDepartureReqest) {
   					if (mPreferenceHelper.isSendStatsEnabled()) {
   						new Thread() {
   							public void run() {
   								uploadStats();
   							}
   						}.start();
   					}
   					mFirstDepartureReqest = false;
        		}

        	}
        	        	
        	// Hide the loading spinners
        	showLoadingView(false);
        	showRefreshSpinner(false);
    		mShowDialog = false;
        	setListAdapter(mListAdapter);
		}
	}
	

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
				mDB = new TramHunterDB(getBaseContext());
				
				int tramNumber = thisTram.getVehicleNo();
				
				if (LOGV) Log.v(TAG, thisTram.toString() + " has tram number: " + tramNumber);
				if (tramNumber > 0) {
					String tramClass = mDB.getTramClass(tramNumber);
					String tramClassImage = UIUtils.getTramImage(tramClass);
					
					if (LOGV) Log.v(TAG, "Tram Class: " + tramClass + " Tram Image: " + tramClassImage);					
				
					int resID = getResources().getIdentifier(tramClassImage, "drawable", getPackageName());
					((ImageView) pv.findViewById(R.id.tramClass)).setPadding(3, 5, 3, 3);
					((ImageView) pv.findViewById(R.id.tramClass)).setBackgroundResource(resID);
				}
				else {
					
				}
				mDB.close();
			}
			return pv;
        }
			
	}		

	
    private void uploadStats() {
    	Log.d("Testing", "Sending stop request statistics");
    	
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			try {
				HttpResponse response = client.execute(post);
				response.getStatusLine().getStatusCode();
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