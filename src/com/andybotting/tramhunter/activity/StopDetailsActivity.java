package com.andybotting.tramhunter.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.andybotting.tramhunter.NextTram;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.StopsList;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.service.TramTrackerService;
import com.andybotting.tramhunter.service.TramTrackerServiceSOAP;


public class StopDetailsActivity extends ListActivity {
	
	private TramHunterDB mDB;
	private TextView mStopNameTextView;
	private TextView mStopDetailsTextView;
	private TextView mStopRoutesTextView;
	private CompoundButton mStarButton;
	private TramTrackerService mTTService;

	private List<NextTram> mNextTrams = new ArrayList<NextTram>();
	private Stop mStop;
	private int mTramTrackerId;
    private volatile Thread mRefreshThread;
    
    boolean mLoadingError = false;
    boolean mShowDialog = true;
     
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
		MenuItem favouriteMenuItem =  menu.getItem(1).setTitle((mStop.isStarred() ? "Unfavourite" : "Favourite"));

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
				mShowDialog = false;
			}
			// Show the spinner in the title bar
			setProgressBarIndeterminateVisibility(true);

		}

		// Automatically done on worker thread (separate from UI thread)
		@Override
		protected List<NextTram> doInBackground(final NextTram... params) {
			TramTrackerService ttService = new TramTrackerServiceSOAP(getBaseContext());
			
			try {
				mNextTrams = ttService.getNextPredictedRoutesCollection(mStop);

				// We'll remove all NextTrams which are longer than
				// 300 minutes away - e.g. not running on weekend
				for(int i=0; i < mNextTrams.size(); i++) {
					if (mNextTrams.get(i).minutesAway() > 300) {
						mNextTrams.remove(i);
					}
				}
				
			} catch (Exception e) {
				// TODO: Put something here
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
				
				setListAdapter(new NextTramsListAdapter());	
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
			
			// Show red for 'Now' only
			if (thisTram.minutesAway() < 1)
				wrapper.getNextTramTime().setTextColor(R.drawable.red);
			
			wrapper.getNextTramTime().setText(thisTram.humanMinutesAway());   

			return pv;
		}

	}

	private class ViewWrapper {
		View base;
			
		TextView nextTramRouteNumber = null;
		TextView nextTramDestination = null;
		TextView nextTramTime = null;
			

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

	}
}