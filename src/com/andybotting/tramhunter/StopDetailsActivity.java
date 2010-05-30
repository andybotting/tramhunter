package com.andybotting.tramhunter;

import java.util.Collections;
import java.util.Vector;

import android.app.ListActivity;
import android.app.ProgressDialog;

import android.content.Context;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import android.widget.BaseAdapter;

import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class StopDetailsActivity extends ListActivity {

	ListView listView;
	TextView firstLineField;
	TextView secondLineField;

	public Route selectedRoute;

	public Vector<NextTram> nextTrams = new Vector<NextTram>();
	public Stop stop;
	public int tramTrackerId;
	
	CompoundButton cb;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		setContentView(R.layout.stop_details);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		String title = getResources().getText(R.string.app_name) + ": Details for Stop";
		setTitle(title);

		// Display stop data from DB
		displayStop();
			
		// Get tram times from TramTracker using AsyncTask
		new GetNextTramTimes().execute();
	}


	public void displayStop() {
		  
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			tramTrackerId = extras.getInt("tramTrackerId");	
		}  
		
		// Create out DB instance
		TramHunterDB db = new TramHunterDB(this);
		stop = db.getStop(tramTrackerId);
		db.close();



		firstLineField = (TextView)findViewById(R.id.firstLine);
		secondLineField = (TextView)findViewById(R.id.secondLine);
					
		// Set labels from Stop hash map
		String firstLineText = stop.getPrimaryName();	
		
		String secondLineText = "Stop " + stop.getFlagStopNumber();
		// If the stop has a secondary name, add it
		if (stop.getSecondaryName().length() > 0) {
			secondLineText += ": " + stop.getSecondaryName();
		}
		secondLineText += " - " + stop.getCityDirection();
			
		firstLineField.setText(firstLineText);
		secondLineField.setText(secondLineText);
		
		cb = (CompoundButton)findViewById(R.id.stopStar);
		cb.setChecked(stop.isStarred());

		cb.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TramHunterDB db = new TramHunterDB(getBaseContext());
				db.setStopStar(tramTrackerId, cb.isChecked());
				stop.setStarred(cb.isChecked());
				db.close();
			}
		});
		
			
		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		LinearLayout layout = new LinearLayout(this);
		
		layout.setPadding(10, 10, 10, 0);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));		
			
		layout.setOrientation(LinearLayout.VERTICAL);	

	}

	
	private class GetNextTramTimes extends AsyncTask<Vector, Void, Vector> {
		private final ProgressDialog dialog = new ProgressDialog(StopDetailsActivity.this);

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("Fetching Tram Times...");
			this.dialog.show();
		}

		// automatically done on worker thread (separate from UI thread)
		protected Vector doInBackground(final Vector... params) {
			TramTrackerRequest ttRequest = new TramTrackerRequest();
			//nextTrams = ttRequest.GetNextPredictedRoutesCollection(stop);

			try {
				nextTrams = ttRequest.GetNextPredictedRoutesCollection(stop);
			} catch (Exception e) {
				// Fail :(
			}
			
			return nextTrams;
		}

		// can use UI thread here
		protected void onPostExecute(Vector nextTrams) {
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			
			if(nextTrams.size() > 0) {
				// Sort trams by minutesAway
				Collections.sort(nextTrams);
				setListAdapter(new NextTramsListAdapter(StopDetailsActivity.this));	
			}
			else {
				Context context = getApplicationContext();
				CharSequence text = "Failed to fetch tram times";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}	
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
//				if (position == 0) {
//					usenameHeight = wrapper.getNextTramDestination().getHeight();
//				}
					
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
		// TODO: Fix icon
		return true;
	}

	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			new GetNextTramTimes().execute();
			return true;
		case 1:
			TramHunterDB db = new TramHunterDB(getBaseContext());
			cb = (CompoundButton)findViewById(R.id.stopStar);
			
			if (stop.isStarred()) {
				db.setStopStar(tramTrackerId, false);
				cb.setChecked(false);
				stop.setStarred(false);
			}	
			else {
				db.setStopStar(tramTrackerId, true);
				cb.setChecked(true);
				stop.setStarred(true);
			}
			db.close();
			return true;
		}
		return false;

	}
}
