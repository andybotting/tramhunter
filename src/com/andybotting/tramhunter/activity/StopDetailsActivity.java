package com.andybotting.tramhunter.activity;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.NextTram;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;
import com.andybotting.tramhunter.util.GenericUtil;
import com.andybotting.tramhunter.util.PreferenceHelper;


public class StopDetailsActivity extends ListActivity {
	
	private TramHunterDB mDB;
	private TextView mStopNameTextView;
	private TextView mStopDetailsTextView;
	private TextView mStopRoutesTextView;
	private CompoundButton mStarButton;
	//private TramTrackerService mTTService;

	private List<NextTram> mNextTrams = new ArrayList<NextTram>();
	private Stop mStop;
	private int mTramTrackerId;
    private volatile Thread mRefreshThread;
    
    boolean mLoadingError = false;
    boolean mShowDialog = true;
    
    private PreferenceHelper mPreferenceHelper;
    private TramTrackerService ttService;
     
    // Handle the timer
    Handler UpdateHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		new GetNextTramTimes().execute();
    	}
	};

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		// Set the window to have a spinner in the title bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.stop_details);		
		
		// Preferences
		mPreferenceHelper = new PreferenceHelper(getBaseContext());
		
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			mTramTrackerId = extras.getInt("tramTrackerId");	
		}  
		
		// Create out DB instance
		mDB = new TramHunterDB(this);
		mStop = mDB.getStop(mTramTrackerId);

		String title = "Stop " + mStop.getFlagStopNumber() + ": " + mStop.getStopName();
		setTitle(title);

		// Display stop data from DB
		displayStop(mStop);
		
		
		mStarButton = (CompoundButton)findViewById(R.id.stopStar);
		mStarButton.setChecked(mStop.isStarred());

		mStarButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDB.setStopStar(mTramTrackerId, mStarButton.isChecked());
				mStop.setStarred(mStarButton.isChecked());
			}
		});

		// Get our TramTracker service
		ttService = new TramTrackerServiceSOAP(getBaseContext());
		
		
		
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

	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, 0, 0, "Refresh");
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_refresh);

		menu.add(0, 1, 0, ""); // Title set in onMenuOpened()
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(R.drawable.ic_menu_star);
		
		menu.add(0, 2, 0, "Map");
		MenuItem menuItem3 = menu.findItem(2);
		menuItem3.setIcon(R.drawable.ic_menu_mapmode);
		
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		// Ensure the 'Favourite' menu item has the correct text
		menu.getItem(1).setTitle((mStop.isStarred() ? "Unfavourite" : "Favourite"));

		return super.onMenuOpened(featureId, menu);
	}

	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			// Refresh
			mShowDialog = true;
			new GetNextTramTimes().execute();
			return true;
		case 1:
			// Star/Favourite
			mStarButton = (CompoundButton)findViewById(R.id.stopStar);
			// Toggle starred
			mDB.setStopStar(mTramTrackerId, !mStop.isStarred());
			mStarButton.setChecked(!mStop.isStarred());
			mStop.setStarred(!mStop.isStarred());
			
			return true;
			
		case 2:
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
		mStopNameTextView = (TextView)findViewById(R.id.stopNameTextView);
		mStopDetailsTextView = (TextView)findViewById(R.id.stopDetailsTextView);
		mStopRoutesTextView = (TextView)findViewById(R.id.stopRoutesTextView);
		
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
			
		mStopNameTextView.setText(firstLineText);
		mStopDetailsTextView.setText(secondLineText);
		mStopRoutesTextView.setText(thirdLineText);
		
	}
	
    private class CountDown implements Runnable {
        public void run() {
        	while(!Thread.currentThread().isInterrupted()){
        		Message m = new Message();
        		UpdateHandler.sendMessage(m);
        		try {
        			// 30 Seconds
        			Thread.sleep(30 * 1000);
        		} 
        		catch (InterruptedException e) {
        			Thread.currentThread().interrupt();
        		}
        	}
        }
	}
	
	private class GetNextTramTimes extends AsyncTask<NextTram, Void, List<NextTram>> {
		private final ProgressDialog dialog = new ProgressDialog(StopDetailsActivity.this);
		
		TextView dateStringText = (TextView)findViewById(R.id.bottomLine);

		// Can use UI thread here
		@Override
		protected void onPreExecute() {

			if (mShowDialog) {
				// Show the dialog window
				this.dialog.setMessage("Fetching Tram Times...");
				this.dialog.show();
				
			}
			// Show the spinner in the title bar
			setProgressBarIndeterminateVisibility(true);

		}

		// Automatically done on worker thread (separate from UI thread)
		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {
			
			
			try {
				mNextTrams = ttService.getNextPredictedRoutesCollection(mStop);
				
				// Only send stats if user has specifically clicked
				if (mShowDialog) {
					// Upload stats
										
					if (mPreferenceHelper.isSendStatsEnabled()) {
						new Thread() {
							public void run() {
								uploadStats();
							}
						}.start();
					}
				}

				// We'll remove all NextTrams which are longer than
				// 300 minutes away - e.g. not running on weekend
				for(int i=0; i < mNextTrams.size(); i++) {
					if (mNextTrams.get(i).minutesAway() > 300) {
						mNextTrams.remove(i);
					}
				}
				
			} catch (Exception e) {
				// Don't do anything
			}
			
			return mNextTrams;
		}

		// Can use UI thread here
		@Override
		protected void onPostExecute(List<NextTram> nextTrams) {
			
			if(nextTrams.size() > 0) {
				// Sort trams by minutesAway
				Collections.sort(nextTrams);
				mLoadingError = false;
				
			    SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy h:mm a");
			    Calendar today = Calendar.getInstance(); // today				
				
				dateStringText.setText("Last updated: " + sdf.format(today.getTime()));
				
				// > 10 because ksoap2 fills in anytype{} instead of null
				if (nextTrams.get(0).getSpecialEventMessage().length() > 10) {
					if (mShowDialog) {
						Context context = getApplicationContext();
						CharSequence text = nextTrams.get(0).getSpecialEventMessage();
						int duration = Toast.LENGTH_LONG;
						Toast toast = Toast.makeText(context, text, duration);
						toast.show();
					}
				}
				else {
					// Show trams list
					setListAdapter(new NextTramsListAdapter());
				}
			}
			else {
				// If we've not had a loading error already
				if (!mLoadingError) {				
					Context context = getApplicationContext();
					CharSequence text = "Failed to fetch tram times";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				mLoadingError = true;
			}	

			// Hide our dialog window
			if (this.dialog.isShowing()) {
				mShowDialog = false;
				this.dialog.dismiss();
			}
			setProgressBarIndeterminateVisibility(false);
			
			
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

			View pv = convertView;
			ViewWrapper wrapper = null;
				
			if (pv == null) {
				LayoutInflater inflater = getLayoutInflater();
				pv = inflater.inflate(R.layout.stop_details_row, parent, false);
					
				wrapper = new ViewWrapper(pv);
				
				pv.setTag(wrapper);
					
				wrapper = new ViewWrapper(pv);
				pv.setTag(wrapper);
			} 
			else {
				wrapper = (ViewWrapper) pv.getTag();
			}
				
			pv.setId(0);
			
			NextTram thisTram = (NextTram) mNextTrams.get(position);
			
			wrapper.getNextTramRouteNumber().setText(thisTram.getRouteNo());
			wrapper.getNextTramDestination().setText(thisTram.getDestination());
			
			if (mPreferenceHelper.isTramImageEnabled()) {
				mDB = new TramHunterDB(getBaseContext());
				// Get the tram class
				
				String tramClassImage = mDB.getTramImage(thisTram.getVehicleNo());
				
				if (tramClassImage != null) {
					int resID = getResources().getIdentifier(tramClassImage, "drawable", "com.andybotting.tramhunter");
					wrapper.getNextTramClass().setPadding(3, 3, 3, 3);
					wrapper.getNextTramClass().setBackgroundResource(resID);
				}
				else {
					Log.d("Testing", "Null image for tram number: " + thisTram.getVehicleNo());
				}
				mDB.close();
			}
			
			// Show red for 'Now' only
//			if (thisTram.humanMinutesAway() == "Now") {
//				Log.d("Testing", "Marking " + thisTram.minutesAway() + " and " + thisTram.humanMinutesAway() + " as RED");
//				wrapper.getNextTramTime().setTextColor(Color.RED);
//			}
			
			wrapper.getNextTramTime().setText(thisTram.humanMinutesAway());   

			return pv;
		}

	}

	private class ViewWrapper {
		View base;
			
		TextView nextTramRouteNumber = null;
		TextView nextTramDestination = null;
		TextView nextTramTime = null;
		ImageView nextTramClass = null;
			

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getNextTramRouteNumber() {
			if (nextTramRouteNumber == null) {
				nextTramRouteNumber = (TextView) base.findViewById(R.id.routeNumber);
			}
			return (nextTramRouteNumber);
		}
		
		TextView getNextTramDestination() {
			if (nextTramDestination == null) {
				nextTramDestination = (TextView) base.findViewById(R.id.routeDestination);
			}
			return (nextTramDestination);
		}
		
		TextView getNextTramTime() {
			if (nextTramTime == null) {
				nextTramTime = (TextView) base.findViewById(R.id.nextTime);
			}
			return (nextTramTime);
		}		

		ImageView getNextTramClass() {
			if (nextTramClass == null) {
				nextTramClass = (ImageView) base.findViewById(R.id.tramClass);
			}
			return (nextTramClass);
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