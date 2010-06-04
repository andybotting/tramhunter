package com.andybotting.tramhunter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class StopDetailsActivity extends ListActivity {
	
	TramHunterDB db;
	
	ListView listView;
	TextView firstLineField;
	TextView secondLineField;
	TextView thirdLineField;

	public Route selectedRoute;

	public Vector<NextTram> nextTrams = new Vector<NextTram>();
	public Stop stop;
	public int tramTrackerId;
	
	CompoundButton starButton;
	TramTrackerRequest ttRequest;

	

    protected boolean running = false;
   
    private volatile Thread refreshThread;
    
    boolean loadingError = false;
    boolean showDialog = true;
 
    Date melbourneTime;
    
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
		listView = (ListView)this.findViewById(android.R.id.list);

		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			tramTrackerId = extras.getInt("tramTrackerId");	
		}  
		
		// Create out DB instance
		db = new TramHunterDB(this);
		stop = db.getStop(tramTrackerId);
		db.close();

		String title = "Stop " + stop.getFlagStopNumber() + ": " + stop.getStopName();
		setTitle(title);

		// Display stop data from DB
		displayStop(stop);
		
		
		starButton = (CompoundButton)findViewById(R.id.stopStar);
		starButton.setChecked(stop.isStarred());

		starButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				db = new TramHunterDB(getBaseContext());
				db.setStopStar(tramTrackerId, starButton.isChecked());
				stop.setStarred(starButton.isChecked());
				db.close();
			}
		});

		// Our thread for updating the stops every 60 secs
        refreshThread = new Thread(new CountDown());
        refreshThread.setDaemon(true);
        refreshThread.start();	

	}
	
	public void onStop() {
		super.onStop();
		refreshThread.interrupt();
	}
	
	
	public void onResume() {
		super.onResume();
		if(refreshThread.isInterrupted()) {
			refreshThread.start();
		}
	}

	
    class CountDown implements Runnable {
        public void run() {
        	while(!Thread.currentThread().isInterrupted()){
        		Message m = new Message();
        		UpdateHandler.sendMessage(m);
        		try {
        			// 60 Seconds
        			Thread.sleep(60 * 1000);
        		} 
        		catch (InterruptedException e) {
        			Thread.currentThread().interrupt();
        		}
        	}
        }
	}
	

    
	public void displayStop(Stop stop) {
		firstLineField = (TextView)findViewById(R.id.firstLine);
		secondLineField = (TextView)findViewById(R.id.secondLine);
		thirdLineField = (TextView)findViewById(R.id.thirdLine);
		
		// Set labels from Stop hash map
		String firstLineText = stop.getPrimaryName();	
		
		String secondLineText = "Stop " + stop.getFlagStopNumber();
		// If the stop has a secondary name, add it
		if (stop.getSecondaryName().length() > 0) {
			secondLineText += ": " + stop.getSecondaryName();
		}
		secondLineText += " - " + stop.getCityDirection();
		
		db = new TramHunterDB(this);
		stop.setRoutes(db.getRoutesForStop(tramTrackerId));
		db.close();
		
		String thirdLineText = stop.getRoutesString();
			
		firstLineField.setText(firstLineText);
		secondLineField.setText(secondLineText);
		thirdLineField.setText(thirdLineText);
		
		
	}
	
	
	private class GetNextTramTimes extends AsyncTask<Vector, Void, Vector> {
		private final ProgressDialog dialog = new ProgressDialog(StopDetailsActivity.this);
		
		TextView dateStringText = (TextView)findViewById(R.id.bottomLine);

		// Can use UI thread here
		protected void onPreExecute() {

			if (showDialog) {
				// Show the dialog window
				this.dialog.setMessage("Fetching Tram Times...");
				this.dialog.show();
				showDialog = false;
			}
			// Show the spinner in the title bar
			setProgressBarIndeterminateVisibility(true);

		}

		// Automatically done on worker thread (separate from UI thread)
		protected Vector doInBackground(final Vector... params) {
			TramTrackerRequest ttRequest = new TramTrackerRequest(getBaseContext());
			
			try {
				nextTrams = ttRequest.GetNextPredictedRoutesCollection(stop);
				

				
				// We'll remove all NextTrams which are longer than
				// 99 minutes away
				for(int i=0; i < nextTrams.size(); i++) {
					if (nextTrams.get(i).minutesAway() > 99) {
						nextTrams.remove(i);
					}
				}
				
				
			} catch (Exception e) {
				// TODO: Put something here
			}
			
			return nextTrams;
		}

		// Can use UI thread here
		protected void onPostExecute(Vector nextTrams) {
			
			if(nextTrams.size() > 0) {
				// Sort trams by minutesAway
				Collections.sort(nextTrams);
				loadingError = false;
				
//				// Get the request time from the first tram
//				NextTram firstTram = (NextTram) nextTrams.get(0);
//				melbourneTime = firstTram.getRequestDateTime();
//				Date date = new Date();
				
			    //String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
				String DATE_FORMAT = "EEE, d MMM yyyy h:mm a";
			    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			    Calendar c1 = Calendar.getInstance(); // today				
				
				dateStringText.setText("Last updated: " + sdf.format(c1.getTime()));
				
				setListAdapter(new NextTramsListAdapter(StopDetailsActivity.this));	
			}
			else {
				// If we've not had a loading error already
				if (!loadingError) {				
					Context context = getApplicationContext();
					CharSequence text = "Failed to fetch tram times";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, text, duration);
					toast.show();
				}
				loadingError = true;
			}	

			// Hide our dialog window
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			setProgressBarIndeterminateVisibility(false);
			
			
		}
	}
	

	private class NextTramsListAdapter extends BaseAdapter {
		
		private Context mContext;		
		private int usenameHeight;

		public NextTramsListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return nextTrams.size();
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
			
			
			NextTram thisTram = (NextTram) nextTrams.get(position);
			
			wrapper.getNextTramRouteNumber().setText(thisTram.getRouteNo());
			wrapper.getNextTramDestination().setText(thisTram.getDestination());
			wrapper.getNextTramTime().setText(thisTram.humanMinutesAway());   		

			return pv;
		}

	}

	class ViewWrapper {
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
	
	
	
	// Add settings to menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, 0, 0, "Refresh");
		MenuItem menuItem1 = menu.findItem(0);
		menuItem1.setIcon(R.drawable.ic_menu_refresh);
		
		menu.add(0, 1, 0, "Favourite");
		MenuItem menuItem2 = menu.findItem(1);
		menuItem2.setIcon(R.drawable.ic_menu_star);
		
		menu.add(0, 2, 0, "Map");
		MenuItem menuItem3 = menu.findItem(2);
		menuItem3.setIcon(R.drawable.ic_menu_mapmode);
		
		return true;
	}

	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			// Refresh
			showDialog = true;
			new GetNextTramTimes().execute();
			return true;
		case 1:
			// Star/Favourite
			TramHunterDB db = new TramHunterDB(getBaseContext());
			starButton = (CompoundButton)findViewById(R.id.stopStar);
			
			if (stop.isStarred()) {
				db.setStopStar(tramTrackerId, false);
				starButton.setChecked(false);
				stop.setStarred(false);
			}	
			else {
				db.setStopStar(tramTrackerId, true);
				starButton.setChecked(true);
				stop.setStarred(true);
			}
			db.close();
			return true;
		case 2:
			// Map view
			Bundle bundle = new Bundle();
			bundle.putInt("tramTrackerId", tramTrackerId);
			Intent intent = new Intent(StopDetailsActivity.this, StopMapActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
			
			return true;
		}
		return false;

	}
}
