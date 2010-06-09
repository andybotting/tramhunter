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
	private List<Stop> m_allStops;
	private LocationManager m_locationManager;
	private Location m_lastKnownLocation;
	
	// Maximum stops to list
	final static int MAXSTOPS = 20;
 
	// Only show loading dialog at first load
	private boolean m_showBusy = true;
	private boolean m_listeningForNetworkLocation;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	 
			
		m_listeningForNetworkLocation = true;
		
		setContentView(R.layout.stops_list);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		// Set the title
		String title = "Nearest Stops";
		setTitle(title);
		
		// Get our stops from the DB
		TramHunterDB db = new TramHunterDB(this);
		m_allStops = db.getAllStops();

		// Get the location
		m_locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
	  	Location location = m_locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

	  	if (location != null) { 
    		new GetNearStops(location).execute();
    	}else{
    		// TODO: This should a nicer dialog explaining that no location services
    		// e.g. GPS or network are available and to enable them. 		
			Context context = getApplicationContext();
			CharSequence text = "Failed to get location!";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			
			// Finish the activity, and go back to the main menu
			this.finish();
    	}
	}

    private OnItemClickListener listView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			StopsListAdapter stopsListAdapter = (StopsListAdapter)adapterView.getAdapter();
			
			Stop thisStop = stopsListAdapter.getStops().get(position);
			int tramTrackerId = thisStop.getTramTrackerID();
						
			Bundle bundle = new Bundle();
			bundle.putInt("tramTrackerId", tramTrackerId);
			Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
		}
    };
	
	private class GetNearStops extends AsyncTask<Stop, Void, ArrayList<Stop>> {
		private final ProgressDialog dialog = new ProgressDialog(NearStopsActivity.this);
		private final Location m_location;
				
		public GetNearStops(Location location){
			m_location = location;
		}
		
		// Can use UI thread here
		protected void onPreExecute() {
			if (m_showBusy) {
				// Show the dialog window
				this.dialog.setMessage("Finding tram stops...");
				this.dialog.show();
				m_showBusy = false;
			}
		}

		// Automatically done on worker thread (separate from UI thread)
		protected ArrayList<Stop> doInBackground(final Stop... params) {
			ArrayList<Stop> sortedStops = new ArrayList<Stop>();
			SortedMap<Double, Stop> sortedStopList = new TreeMap<Double, Stop>();
			
	    	for(Stop stop : m_allStops){
	    		double distance = m_location.distanceTo(stop.getLocation());
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
		protected void onPostExecute(final ArrayList<Stop> sortedStops) {
			listView.setOnItemClickListener(listView_OnItemClickListener);
			
			// TODO: Update the current stops if the list items haven't change and just the distance has, so the user can scroll without being inturrupted
			setListAdapter(new StopsListAdapter(sortedStops, m_location));
			
			// Hide dialog
			if (this.dialog.isShowing())
				this.dialog.dismiss();		
		}
	}
		  
	private class StopsListAdapter extends BaseAdapter {

		private ArrayList<Stop> m_stops;
		private Location m_location;
		
		public StopsListAdapter(ArrayList<Stop> stops, Location location){
			m_stops = stops;
			m_location = location;
		}
		
		public int getCount() {
			return m_stops.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public ArrayList<Stop> getStops() {
			return m_stops;
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
			
			Stop thisStop = m_stops.get(position);
			String textLabel1 = thisStop.getStopName();
			String textLabel2 = "Stop " + thisStop.getFlagStopNumber();

			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName().length() > 0) {
				textLabel2 += ": " + thisStop.getSecondaryName();
			}
			
			textLabel2 += " - " + thisStop.getCityDirection();

			String textLabel3 = "" + thisStop.formatDistanceTo(m_location);
			
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
        startLocationListening(true);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    	stopLocationListening();
    }
    
    public void onLocationChanged(Location location) {

    	if (location != null)
    	{
        	// If this is a GPS location then ignore and unsubscribe from network location updates.
        	if(location.getProvider().equals("gps")&&m_listeningForNetworkLocation){
        		stopLocationListening();
        		startLocationListening(false);
        	}
        	
        	// TODO: Remove this when the list items are updated instead of changed (when only distance has changed and the same 20 stops are listed)
        	if(m_lastKnownLocation!=null&&m_lastKnownLocation.distanceTo(location)>2)
        	{
        		new GetNearStops(location).execute();	
        	}else if(m_lastKnownLocation==null){
        		new GetNearStops(location).execute();
        	}      	
        	
        	m_lastKnownLocation = location;
        	
    	}

    }
        
    private void stopLocationListening() {
        if (m_locationManager != null)
        	m_locationManager.removeUpdates(this);
	}
                 
    private void startLocationListening(boolean subscribeToNetworkLocation) {
    	if (m_locationManager!=null){
    		m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        	m_listeningForNetworkLocation = subscribeToNetworkLocation;
        	
        	if(subscribeToNetworkLocation)       		
        		m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);    		
    	}
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

}
