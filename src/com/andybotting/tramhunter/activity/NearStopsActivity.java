package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Route;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.StopsList;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.util.GenericUtil;
 
public class NearStopsActivity extends ListActivity implements LocationListener {
	
	private ListView mListView;
	private List<Stop> mAllStops;
	private StopsList mNearStopsList;
	private LocationManager mLocationManager;
	private Location mLastKnownLocation;
	
	private TramHunterDB mDB;
		
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
		mListView = (ListView)this.findViewById(android.R.id.list);
		
		mListView.setOnItemClickListener(listView_OnItemClickListener);
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		
		// Set the title
		setTitle(m_title);
		
		// Get our stops from the DB
		mDB = new TramHunterDB(this);
		mAllStops = mDB.getAllStops();
		
		// Make our Near Stops List for the map
		mNearStopsList = new StopsList();

		// Get the location
		mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
	  	Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
  	
	  	if (location != null) {
	  		new StopDistanceCalculator(location).execute();			
    	}else{
    		// TODO: This should a nicer dialog explaining that no location services
    		GenericUtil.popToast(this, "Unable to determine location!");			
			// Finish the activity, and go back to the main menu
			this.finish();
    	}
	}	
	
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(NearStopsActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		
		startActivityForResult(intent, 1);
	}
	
	// Add menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "Map");
		MenuItem menuItem0 = menu.findItem(0);
		menuItem0.setIcon(R.drawable.ic_menu_mapmode);
		
		return true;
	}
	
	// Menu actions
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Bundle bundle = new Bundle();
			bundle.putParcelable("stopslist", mNearStopsList);
			Intent intent = new Intent(NearStopsActivity.this, StopMapActivity.class);
			intent.putExtras(bundle);
			startActivityForResult(intent, 1);
		}
		return false;

	}
		
	private OnItemClickListener listView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			StopsListAdapter stopsListAdapter = (StopsListAdapter)adapterView.getAdapter();
			viewStop(stopsListAdapter.getStops().get(position));
		}
    };
    
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info;
			try {
			    info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			} catch (ClassCastException e) {
			    return;
			}

			StopsListAdapter stopsListAdapter = (StopsListAdapter)getListAdapter();
			Stop thisStop = stopsListAdapter.getStops().get(info.position);
			menu.add(0, 0, 0, "View Stop");
			menu.add(0, 1, 0, (thisStop.isStarred() ? "Unfavourite" : "Favourite"));
		}
    };
    
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			StopsListAdapter stopsListAdapter = (StopsListAdapter)getListAdapter();
			Stop thisStop = stopsListAdapter.getStops().get(info.position);
        	
        	switch (item.getItemId()) {
    			case 0:
    				viewStop(thisStop);
    				return true;
    			case 1:
    				// Toggle favourite
    				mDB.setStopStar(thisStop.getTramTrackerID(), !thisStop.isStarred());
    				thisStop.setStarred(!thisStop.isStarred());
    				return true;
        	}
    	} catch (ClassCastException e) {}
    	    	
		return super.onContextItemSelected(item);
    }
    	
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
			
	    	for(Stop stop : mAllStops){
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
	    	
			// Find the routes for our nearest stops
			for(Stop stop: sortedStops) {
				// Add this stop to our near StopsList
				mNearStopsList.add(stop);
				List<Route> routes = mDB.getRoutesForStop(stop.getTramTrackerID());
				stop.setRoutes(routes);
			}
			
			return sortedStops;
		}
		
		// Can use UI thread here
		protected void onPostExecute(final ArrayList<Stop> sortedStops) {
		
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
			String stopNameLabel = stop.getPrimaryName();
			String directionLabel = "Stop " + stop.getFlagStopNumber();

			// If the stop has a secondary name, add it 
			if (stop.getSecondaryName().length() > 0)
				directionLabel += ": " + stop.getSecondaryName();
			
			directionLabel += " - " + stop.getCityDirection();
			String distanceLabel = "" + stop.formatDistanceTo(m_location);
			String routesLabel = stop.getRoutesString();
			
			viewWrapper.getStopNameTextView().setText(stopNameLabel);
			viewWrapper.getStopDetailsTextView().setText(directionLabel);
			viewWrapper.getStopDistanceTextView().setText(distanceLabel);
			viewWrapper.getStopRoutesTextView().setText(routesLabel);
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
		TextView m_stopDetailsTextView = null;
		TextView m_stopDistanceTextView = null;
		TextView m_stopRoutesTextView = null;

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getStopNameTextView() {
			if (m_stopNameTextView == null) {
				m_stopNameTextView = (TextView) base.findViewById(R.id.stopNameTextView);
			}
			return (m_stopNameTextView);
		}

		TextView getStopDetailsTextView() {
			if (m_stopDetailsTextView == null) {
				m_stopDetailsTextView = (TextView) base.findViewById(R.id.stopDetailsTextView);
			}
			return (m_stopDetailsTextView);
		}

		TextView getStopDistanceTextView() {
			if (m_stopDistanceTextView == null) {
				m_stopDistanceTextView = (TextView) base.findViewById(R.id.stopDistanceTextView);
			}
			return (m_stopDistanceTextView);
		}
		
		TextView getStopRoutesTextView() {
			if (m_stopRoutesTextView == null) {
				m_stopRoutesTextView = (TextView) base.findViewById(R.id.stopRoutesTextView);
			}
			return (m_stopRoutesTextView);
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
    	mDB.close();
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
        		mLastKnownLocation = location;
        	}
        	
        	if(location.hasAccuracy()){
        		setTitle(m_title + "    ±" + (int)location.getAccuracy() + "m");	
        	}else{
        		setTitle(m_title);
        	}
        	
    	}
    }
    
    private boolean shouldCalculateNewDistance(Location location){
		boolean result = false;
		
    	if(mLastKnownLocation!=null&&mLastKnownLocation.distanceTo(location)>1)
    	{
    		result = true;	
    	}else if(mLastKnownLocation==null){
    		result = true;
    	}
    	
    	return result && (!m_isCalculatingStopDistances);
    }
                
    private void stopLocationListening() {
        if (mLocationManager != null)
        	mLocationManager.removeUpdates(this);
	}
                 
    private void startLocationListening(boolean subscribeToNetworkLocation) {
    	if (mLocationManager!=null){
    		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        	m_isListeningForNetworkLocation = subscribeToNetworkLocation;
        	
        	if(subscribeToNetworkLocation)       		
        		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);    		
    	}
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
}
