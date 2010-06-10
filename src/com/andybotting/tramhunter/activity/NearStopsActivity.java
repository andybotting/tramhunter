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
	private final int MAXSTOPS = 20;
	private final String m_title = "Nearest Stops";//±	
 
	// Only show loading dialog at first load
	private boolean m_showBusy = true;
	private boolean m_isListeningForNetworkLocation;
	private boolean m_isCalculatingStopDistances;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	 
			
		m_isListeningForNetworkLocation = true;
		m_isCalculatingStopDistances = false;
		
		setContentView(R.layout.stops_list);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		// Set the title
		setTitle(m_title);
		
		// Get our stops from the DB
		TramHunterDB db = new TramHunterDB(this);
		m_allStops = db.getAllStops();

		// Get the location
		m_locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
	  	Location location = m_locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
  	
	  	if (location != null) {
	  		new StopDistanceCalculator(location).execute();			
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
	
	private class StopDistanceCalculator extends AsyncTask<Stop, Void, ArrayList<Stop>> {
		private final ProgressDialog dialog = new ProgressDialog(NearStopsActivity.this);
		private final Location m_location;
		private boolean m_refreshListOnly;
		
		public StopDistanceCalculator(Location location){
			m_location = location;
		}
		
		// Can use UI thread here
		protected void onPreExecute() {
			m_isCalculatingStopDistances = true;
			m_refreshListOnly = !m_showBusy;
			
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
	
	    		if(sortedStops.size() >= MAXSTOPS)
	    			break;
	    	}
			
			return sortedStops;
		}
		
		// Can use UI thread here
		protected void onPostExecute(final ArrayList<Stop> sortedStops) {
			listView.setOnItemClickListener(listView_OnItemClickListener);
			
			if(m_refreshListOnly){
				// Just update the list
				StopsListAdapter stopsListAdapter = (StopsListAdapter)getListAdapter();
				stopsListAdapter.updateStopList(sortedStops, m_location);
			}else{
				// Refresh the entire list
				StopsListAdapter stopsListAdapter = new StopsListAdapter(sortedStops, m_location);
				setListAdapter(stopsListAdapter);	
			}
			
			// Hide dialog
			if (this.dialog.isShowing())
				this.dialog.dismiss();	
			
			m_isCalculatingStopDistances = false;
		}
	}
		  
	private class StopsListAdapter extends BaseAdapter {

		private ArrayList<Stop> m_stops;
		private Location m_location;
		
		public StopsListAdapter(ArrayList<Stop> stops, Location location){
			m_stops = stops;
			m_location = location;
		}
		
		public void updateStopList(ArrayList<Stop> stops, Location location){
			m_stops = stops;
			m_location = location;
			this.notifyDataSetChanged();			
		}
		
		public ArrayList<Stop> getStops() {
			return m_stops;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View inflatedView = getLayoutInflater().inflate(R.layout.near_stops_list_row, parent, false);
			ViewWrapper viewWrapper = new ViewWrapper(inflatedView);
			inflatedView.setTag(viewWrapper);
			fillViewWrapper(viewWrapper, m_stops.get(position));
			
			return inflatedView;
		}

		private void fillViewWrapper(ViewWrapper viewWrapper, Stop stop){
			String stopNameLabel = stop.getStopName();
			String directionLabel = "Stop " + stop.getFlagStopNumber();

			// If the stop has a secondary name, add it 
			if (stop.getSecondaryName().length() > 0)
				directionLabel += ": " + stop.getSecondaryName();
			
			directionLabel += " - " + stop.getCityDirection();
			String distanceLabel = "" + stop.formatDistanceTo(m_location);
			
			viewWrapper.getStopNameTextView().setText(stopNameLabel);
			viewWrapper.getDirectionTextView().setText(directionLabel);
			viewWrapper.getDistanceTextView().setText(distanceLabel);	
		}
		
		public int getCount() {
			return m_stops.size();
		}

		// TODO: LUKEK: Determine what these are meant to do and implement them properly...
		public Object getItem(int position) {return position;}
		public long getItemId(int position) {return position;}
		
	}

	class ViewWrapper {
		View base;
				
		TextView m_stopNameTextView = null;
		TextView m_directionTextView = null;
		TextView m_distanceTextView = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getStopNameTextView() {
			if (m_stopNameTextView == null) {
				m_stopNameTextView = (TextView) base.findViewById(R.id.stopNameText);
			}
			return (m_stopNameTextView);
		}

		TextView getDirectionTextView() {
			if (m_directionTextView == null) {
				m_directionTextView = (TextView) base.findViewById(R.id.directionText);
			}
			return (m_directionTextView);
		}

		TextView getDistanceTextView() {
			if (m_distanceTextView == null) {
				m_distanceTextView = (TextView) base.findViewById(R.id.distanceText);
			}
			return (m_distanceTextView);
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
        	if(location.getProvider().equals("gps")&&m_isListeningForNetworkLocation){
        		stopLocationListening();
        		startLocationListening(false);
        	}
        	
        	if(shouldCalculateNewDistance(location)){       		
        		new StopDistanceCalculator(location).execute();
        		m_lastKnownLocation = location;
        	}
        	
        	if(location.hasAccuracy()){
        		setTitle(m_title + "   ±" + (int)location.getAccuracy() + "m");	
        	}else{
        		setTitle(m_title);
        	}
        	
    	}
    }
    
    private boolean shouldCalculateNewDistance(Location location){
		boolean result = false;
		
    	if(m_lastKnownLocation!=null&&m_lastKnownLocation.distanceTo(location)>1)
    	{
    		result = true;	
    	}else if(m_lastKnownLocation==null){
    		result = true;
    	}
    	
    	return result && (!m_isCalculatingStopDistances);
    }
                
    private void stopLocationListening() {
        if (m_locationManager != null)
        	m_locationManager.removeUpdates(this);
	}
                 
    private void startLocationListening(boolean subscribeToNetworkLocation) {
    	if (m_locationManager!=null){
    		m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        	m_isListeningForNetworkLocation = subscribeToNetworkLocation;
        	
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
