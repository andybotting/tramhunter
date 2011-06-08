package com.andybotting.tramhunter.activity;


import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
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
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;

public class StopsListActivity extends ListActivity {

	private final static int CONTEXT_MENU_VIEW_STOP = 0;
	
	private ListView mListView;
	private StopsListAdapter mListAdapter;
	private List<Stop> mStops;
	private TramHunterDB mDB;
	private Destination mDestination;

	
	/**
	 * On Create
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		long destinationId = -1;
		String searchQuery = null;
		
		setContentView(R.layout.stops_list);
		mListView = (ListView) getListView();
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(StopsListActivity.this);
		    }
		});	

		// Search title button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(StopsListActivity.this);
		    }
		});	
		
		
		mDB = new TramHunterDB();
		
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
		  destinationId = extras.getLong("destinationId");
		  searchQuery = extras.getString("search_query");
		}  

		// Are we looking for stops for a route, or fav stops?
		if(searchQuery != null) {
			final CharSequence title = getString(R.string.search_results, searchQuery);
			((TextView) findViewById(R.id.title_text)).setText(title);
			displaySearchStops(searchQuery);
		}
		else if (destinationId != -1) {
			Log.d("Testing", "Getting destination: " + destinationId);
			mDestination = mDB.getDestination(destinationId);
			String title = "Route " + mDestination.getRouteNumber() + " to " + mDestination.getDestination();
			((TextView) findViewById(R.id.title_text)).setText(title);
			displayStopsForDestination(destinationId);
		}
		
	}

	
	/**
	 * Close the DB connection if this activity finishes
	 */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    	mDB.close();
    }
	
    
    /**
     * Display the list of stops for a search
     * @param search
     */
	public void displaySearchStops(String search) {
		mStops = mDB.getStopsForSearch(search);
		displayStops();
	}
	
	
	/**
	 * Display the list of stops for a destination
	 * @param destinationId
	 */
	public void displayStopsForDestination(long destinationId) {
		mStops = mDB.getStopsForDestination(destinationId);
		
		// Let's remove any Terminus stops, because we can't get times for them
		for(int i=0; i<mStops.size(); i++) {
			Stop stop = mStops.get(i);
			if (stop.getTramTrackerID() >= 8000) {
				mStops.remove(i);
			}
		}
		
		displayStops();
	}
	
	
	/**
	 * Display the list of stops
	 */
	public void displayStops() {
		
		mListView.setOnItemClickListener(mListView_OnItemClickListener);		
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		mListAdapter = new StopsListAdapter();
		setListAdapter(mListAdapter);
	}
	
	
	/**
	 * Start the activity to view a stop
	 * @param stop
	 */
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(StopsListActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		
		startActivityForResult(intent, 1);
	}
	
	
	/**
	 * List item click action
	 */
	private OnItemClickListener mListView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			viewStop((Stop)mStops.get(position));
		}
    };

    
    /**
     * Create the context menu
     */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info;
			try {
			    info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			} catch (ClassCastException e) {
			    return;
			}

			Stop thisStop = (Stop)mStops.get(info.position);
			menu.setHeaderIcon(R.drawable.icon);
			menu.setHeaderTitle(thisStop.getStopName());
			menu.add(0, CONTEXT_MENU_VIEW_STOP, 0, "View Stop");
		}
    };
    
    
    /**
     * Context menu actions
     */
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        	Stop thisStop = (Stop)mStops.get(info.position);
        	
        	switch (item.getItemId()) {
    			case CONTEXT_MENU_VIEW_STOP:
    				viewStop(thisStop);
    				return true;
        	}
    	} catch (ClassCastException e) {}
    	    	
		return super.onContextItemSelected(item);
    }
     
    
	private class StopsListAdapter extends BaseAdapter {

		public int getCount() {
			return mStops.size();
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
    			pv = inflater.inflate(R.layout.stops_list_row, parent, false);
            }
            else {
                pv = convertView;
            }
			
			Stop thisStop = (Stop) mStops.get(position);
			
			String stopName = thisStop.getPrimaryName();
			String stopDetails = "Stop " + thisStop.getFlagStopNumber();
			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName().length() > 0) {
				stopDetails += ": " + thisStop.getSecondaryName();
			}
			
			stopDetails += " - " + thisStop.getCityDirection();
			stopDetails += " (" + thisStop.getTramTrackerID() + ")";

			((TextView) pv.findViewById(R.id.stopNameTextView)).setText(stopName);
			((TextView) pv.findViewById(R.id.stopDetailsTextView)).setText(stopDetails);
			
			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(thisStop.getRoutesString());

			return pv;
		}
			
	}
	
}