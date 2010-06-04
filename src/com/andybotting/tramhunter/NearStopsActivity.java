package com.andybotting.tramhunter;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import com.andybotting.tramhunter.RoutesListActivity.ViewWrapper;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
 
public class NearStopsActivity extends ListActivity implements LocationListener {
	
	ListView listView;
	Vector<Stop> stops = new Vector<Stop>();
	Vector<Stop> sortedStops = new Vector<Stop>();
	SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();
	TramHunterDB db;
	
	Location location;
	private LocationManager locationManager;
	private static final String PROVIDER = "passive";
	
	// Maximum stops
	final static int MAXSTOPS = 20;
 
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	 
	
		setContentView(R.layout.stops_list);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		// Set the title
		String title = "Nearest Stops";
		setTitle(title);
		
		// Get our stops from the DB
		db = new TramHunterDB(this);
		stops = db.getAllStops();

		// Get the location
		location = findLocation();
		if(location != null) {
			new GetNearStops().execute();
		}
		else {
			// Toast?
		}
	}

	
	private Location findLocation() {
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation(PROVIDER);
		return location;
	}
	
	
	public void sortStops(Location location) {
		
		// Iterate through stops, adding to a sortlist 
    	for(Stop stop : stops){
    		double distance = location.distanceTo(stop.getLocation());
    		sortedStopList.put(distance, stop);
    	}  

    	// Build a sorted list, of MAXSTOPS stops 
    	for(Entry<Double, Stop> item : sortedStopList.entrySet()) {

    		Stop stop = item.getValue();
    		sortedStops.add(stop);

    		if(sortedStops.size() > MAXSTOPS)
    			break;
    	}
	}
	
	
	public void displayStops() {
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
				
				Stop thisStop = sortedStops.get(position);
				
				int tramTrackerId = thisStop.getTramTrackerID();
							
				Bundle bundle = new Bundle();
				bundle.putInt("tramTrackerId", tramTrackerId);
				Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
			}

		});		

		setListAdapter(new StopsListAdapter(this));
	}

	
	
	
	private class GetNearStops extends AsyncTask<Vector, Void, Vector> {
		private final ProgressDialog dialog = new ProgressDialog(NearStopsActivity.this);

		// Can use UI thread here
		protected void onPreExecute() {
				this.dialog.setMessage("Finding tram stops...");
				this.dialog.show();

		}

		// Automatically done on worker thread (separate from UI thread)
		protected Vector doInBackground(final Vector... params) {
			
			// Iterate through stops, adding to a sortlist 
	    	for(Stop stop : stops){
	    		double distance = location.distanceTo(stop.getLocation());
	    		sortedStopList.put(distance, stop);
	    	}  

	    	// Build a sorted list, of MAXSTOPS stops 
	    	for(Entry<Double, Stop> item : sortedStopList.entrySet()) {

	    		Stop stop = item.getValue();
	    		sortedStops.add(stop);

	    		if(sortedStops.size() > MAXSTOPS)
	    			break;
	    	}
	    	
			return sortedStops;
		}

		
		// Can use UI thread here
		protected void onPostExecute(final Vector sortedStops) {


		
			listView.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
					
					Stop thisStop = (Stop) sortedStops.get(position);

					int tramTrackerId = thisStop.getTramTrackerID();
								
					Bundle bundle = new Bundle();
					bundle.putInt("tramTrackerId", tramTrackerId);
					Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
					intent.putExtras(bundle);
					startActivityForResult(intent, 1);
				}

			});		

			setListAdapter(new StopsListAdapter(NearStopsActivity.this));
			
			// Hide dialog
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			
		}
	}
	
	
	
	  
	private class StopsListAdapter extends BaseAdapter {
			
		private Context mContext;		
		private int usenameHeight;

		public StopsListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return sortedStops.size();
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
			pv = inflater.inflate(R.layout.near_stops_list_row, parent, false);
				
			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);
			
			Stop thisStop = sortedStops.get(position);
			String textLabel1 = thisStop.getStopName();
			String textLabel2 = "Stop " + thisStop.getFlagStopNumber();

			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName().length() > 0) {
				textLabel2 += ": " + thisStop.getSecondaryName();
			}
			
			textLabel2 += " - " + thisStop.getCityDirection();

			String textLabel3 = "" + thisStop.formatDistanceTo(location);
			
			wrapper.getTextLabel1().setText(textLabel1);
			wrapper.getTextLabel2().setText(textLabel2);
			wrapper.getTextLabel3().setText(textLabel3);
			
			return pv;

		}

	}


	class ViewWrapper {
		View base;
				
		TextView textLabel1 = null;
		TextView textLabel2 = null;
		TextView textLabel3 = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getTextLabel1() {
			if (textLabel1 == null) {
				textLabel1 = (TextView) base.findViewById(R.id.stopNameText);
			}
			return (textLabel1);
		}

		TextView getTextLabel2() {
			if (textLabel2 == null) {
				textLabel2 = (TextView) base.findViewById(R.id.directionText);
			}
			return (textLabel2);
		}

		TextView getTextLabel3() {
			if (textLabel3 == null) {
				textLabel3 = (TextView) base.findViewById(R.id.distanceText);
			}
			return (textLabel3);
		}
		
	}	
	
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

}
