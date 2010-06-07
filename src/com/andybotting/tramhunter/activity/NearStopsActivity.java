package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.dao.TramHunterDB;
 
public class NearStopsActivity extends ListActivity implements LocationListener {
	
	private ListView listView;
	private List<Stop> stops = new ArrayList<Stop>();
	private List<Stop> sortedStops = new ArrayList<Stop>();
	private SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();
	private TramHunterDB db;

	private LocationManager locationManager;
	private Location location;
	
	// Maximum stops to list
	final static int MAXSTOPS = 20;
 
	// Only show loading dialog at first load
	boolean showDialog = true;
	
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
    	if (location != null) { 
    		new GetNearStops().execute();
    	}
    	else {
    		// TODO: This should a nicer dialog explaining that no location services
    		// e.g. GPS or network are available and to enable them. 		
			Context context = getApplicationContext();
			CharSequence text = "No Location Services Available!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			
			// Finish the activity, and go back to the main menu
			this.finish();
    	}
	}

	
	private Location findLocation() {	
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	  	Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		return location;
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

		setListAdapter(new StopsListAdapter());
	}

	
	
	
	private class GetNearStops extends AsyncTask<Stop, Void, List<Stop>> {
		private final ProgressDialog dialog = new ProgressDialog(NearStopsActivity.this);

		// Can use UI thread here
		protected void onPreExecute() {
			if (showDialog) {
				// Show the dialog window
				this.dialog.setMessage("Finding tram stops...");
				this.dialog.show();
				showDialog = false;
			}
		}

		// Automatically done on worker thread (separate from UI thread)
		protected List<Stop> doInBackground(final Stop... params) {
		
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
		protected void onPostExecute(final List<Stop> sortedStops) {

			if (location != null) {
			
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
	
				setListAdapter(new StopsListAdapter());
			}
			else {
				Context context = getApplicationContext();
				CharSequence text = "Failed to get location";
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
			
			// Hide dialog
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			
		}
	}
	
	
	
	  
	private class StopsListAdapter extends BaseAdapter {

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
    protected void onPause() {
    	super.onPause();
        stopLocationListening();
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        startLocationListening();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    	stopLocationListening();
    }
    
    public void onLocationChanged(Location location) {
    	if (location != null)
    		new GetNearStops().execute();
    }
    
    private void stopLocationListening() {
        if (locationManager != null)
        	locationManager.removeUpdates(this);
	}
    
    private void startLocationListening() {
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
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
