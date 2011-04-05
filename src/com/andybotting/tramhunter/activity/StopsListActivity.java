package com.andybotting.tramhunter.activity;


import java.util.List;

import android.app.ListActivity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;

public class StopsListActivity extends ListActivity {

	private final static int CONTEXT_MENU_VIEW_STOP = 0;
	private final static int CONTEXT_MENU_STAR_STOP = 1;
	
	private ListView mListView;
	private StopsListAdapter mListAdapter;
	private List<Stop> mStops;
	private TramHunterDB mDB;
	private Destination mDestination;
	private Context mContext;
	private PreferenceHelper mPreferenceHelper;
	
	
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
		
		
        mContext = this.getBaseContext();
        mPreferenceHelper = new PreferenceHelper(mContext);
		mDB = new TramHunterDB(mContext);
		
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    	mDB.close();
    }
	
	public void displaySearchStops(String search) {
		mStops = mDB.getStopsForSearch(search);
		displayStops();
	}
	
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
	
	public void displayStops() {
		
		mListView.setOnItemClickListener(mListView_OnItemClickListener);		
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		mListAdapter = new StopsListAdapter();
		setListAdapter(mListAdapter);
	}
	
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(StopsListActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		
		startActivityForResult(intent, 1);
	}
	
	private OnItemClickListener mListView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			viewStop((Stop)mStops.get(position));
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

			Stop thisStop = (Stop)mStops.get(info.position);
			menu.add(0, CONTEXT_MENU_VIEW_STOP, 0, "View Stop");
			menu.add(0, CONTEXT_MENU_STAR_STOP, 0, (mPreferenceHelper.isStarred(thisStop.getTramTrackerID()) ? "Unfavourite" : "Favourite"));
		}
    };
    
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        	Stop thisStop = (Stop)mStops.get(info.position);
        	
        	switch (item.getItemId()) {
    			case CONTEXT_MENU_VIEW_STOP:
    				viewStop(thisStop);
    				return true;
    			case CONTEXT_MENU_STAR_STOP:
    				// Toggle favourite
    				mPreferenceHelper.setStopStar(thisStop.getTramTrackerID(), !mPreferenceHelper.isStarred(thisStop.getTramTrackerID()));
    				// Refresh the adapter to show fav/unfav changes in list
    				mListAdapter.notifyDataSetChanged();
    				
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
				
			if (mPreferenceHelper.isStarred(thisStop.getTramTrackerID()))
				((ImageView) pv.findViewById(R.id.starImageView)).setVisibility(View.VISIBLE);
			else
				((ImageView) pv.findViewById(R.id.starImageView)).setVisibility(View.INVISIBLE);
			
			return pv;
		}
			
	}
	
}