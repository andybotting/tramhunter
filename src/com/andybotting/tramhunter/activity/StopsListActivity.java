package com.andybotting.tramhunter.activity;

import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
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
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;

public class StopsListActivity extends ListActivity {

	private boolean mIsFavouritesView;
	private ListView mListView;
	private StopsListAdapter mListAdapter;
	private List<Stop> mStops;
	private TramHunterDB mDB;
	private Destination mDestination;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		long destinationId = -1;
		String searchQuery = null;
		
		setContentView(R.layout.stops_list);
		mListView = (ListView)this.findViewById(android.R.id.list);
		
		String title = "";
		mDB = new TramHunterDB(this);
		
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
		  destinationId = extras.getLong("destinationId");
		  searchQuery = extras.getString("search_query");
		}  

		// Are we looking for stops for a route, or fav stops?
		if(searchQuery != null) {
			title = "Stops for search";
			setTitle(title);
			displaySearchStops(searchQuery);
		}
		else if (destinationId != -1) {
			Log.d("Testing", "Getting destination: " + destinationId);
			mDestination = mDB.getDestination(destinationId);
			
			title = "Stops for Route " + mDestination.getRouteNumber() + " to " + mDestination.getDestination();
			setTitle(title);
			displayStopsForDestination(destinationId);
		}
		else {
			mIsFavouritesView = true;
			title = "Favourite Stops";
			setTitle(title);
			displayFavStops(true);
		}
		
	}
	  
	@Override
	protected void onResume() {
		super.onResume();
		
		// Refresh favourites if the back button is pressed
		if (mIsFavouritesView)
		{
			displayFavStops(false);
		}
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    	mDB.close();
    }
	
	public void displayFavStops(boolean alertIfNoStops) {
		mStops = mDB.getFavouriteStops();
		
		if (alertIfNoStops && mStops.size() == 0) {
			alertNoFavourites();		
		}else{
			// Find the routes for our stops
			for(Stop stop: mStops) {
				List<Route> routes = mDB.getRoutesForStop(stop.getTramTrackerID());
				stop.setRoutes(routes);
			}
		}
		displayStops();
	}
	
	public void displaySearchStops(String search) {
		mStops = mDB.getStopsForSearch(search);
		displayStops();
	}

	private void alertNoFavourites() {
		final Intent routeListIntent = new Intent(this, RoutesListActivity.class);
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setMessage("You currently have no favourite tram stops.\n\nTo add favourite tram stops simply click the favourite star when browsing tram stops.\n\nDo you want to browse some now?")
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				// Action for 'Yes' Button				
					startActivity(routeListIntent);
					finish();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//  Action for 'NO' Button
					dialog.cancel();				
					// TODO: getParent() instead?
					finish();
				}
			});
		
		AlertDialog alert = dialogBuilder.create();
		// Title for AlertDialog
		alert.setTitle("No Favourite Stops");
		// Icon for AlertDialog
		alert.setIcon(R.drawable.icon);
		alert.show();
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
		
// Slows things down a bit!
//		// Find the routes for our stops
//		for(Stop stop: mStops) {
//			List<Route> routes = mDB.getRoutesForStop(stop.getTramTrackerID());
//			stop.setRoutes(routes);
//		}
		
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
			// TODO: These menu items should use final static variables
			menu.add(0, 0, 0, "View Stop");
			menu.add(0, 1, 0, (thisStop.isStarred() ? "Unfavourite" : "Favourite"));
		}
    };
    
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        	Stop thisStop = (Stop)mStops.get(info.position);
        	
        	switch (item.getItemId()) {
    			case 0:
    				viewStop(thisStop);
    				return true;
    			case 1:
    				// Toggle favourite
    				mDB.setStopStar(thisStop.getTramTrackerID(), !thisStop.isStarred());
    				thisStop.setStarred(!thisStop.isStarred());
    				// Refresh the favourites stops list
    				if(mIsFavouritesView)
    					displayFavStops(false);

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
	
			View pv = convertView;
			ViewWrapper wrapper = null;

			LayoutInflater inflater = getLayoutInflater();
			pv = inflater.inflate(R.layout.stops_list_row, parent, false);
					
			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);

			Stop thisStop = (Stop) mStops.get(position);
			
			String stopName = thisStop.getPrimaryName();
			String stopDetails = "Stop " + thisStop.getFlagStopNumber();
			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName().length() > 0) {
				stopDetails += ": " + thisStop.getSecondaryName();
			}
			
			stopDetails += " - " + thisStop.getCityDirection();
			stopDetails += " (" + thisStop.getTramTrackerID() + ")";
			
			
			wrapper.getStopNameTextView().setText(stopName);
			wrapper.getStopDetailsTextView().setText(stopDetails);
			

			
			// Only show the routes if this is the favourites view, it's too slow with LOTS of stops
			if(mIsFavouritesView){
				wrapper.getStopRoutesTextView().setText(thisStop.getRoutesString());	
			}
			else{
				wrapper.getStopRoutesTextView().setVisibility(View.GONE);
				
				if (thisStop.isStarred()) {
					wrapper.getStarImageView().setVisibility(View.VISIBLE);
				}
				
			}
						
			return pv;

		}

	}

	class ViewWrapper {
		View base;
				
		TextView stopNameTextView = null;
		TextView stopDetailsTextView = null;
		TextView stopRoutesTextView = null;
		ImageView starImageView = null;
		
		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getStopNameTextView() {
			if (stopNameTextView == null) {
				stopNameTextView = (TextView) base.findViewById(R.id.stopNameTextView);
			}
			return (stopNameTextView);
		}

		TextView getStopDetailsTextView() {
			if (stopDetailsTextView == null) {
				stopDetailsTextView = (TextView) base.findViewById(R.id.stopDetailsTextView);
			}
			return (stopDetailsTextView);
		}

		TextView getStopRoutesTextView() {
			if (stopRoutesTextView == null) {
				stopRoutesTextView = (TextView) base.findViewById(R.id.stopRoutesTextView);
			}
			return (stopRoutesTextView);
		}
		
		ImageView getStarImageView() {
			if (starImageView == null) {
				starImageView = (ImageView) base.findViewById(R.id.starImageView);
			}
			return (starImageView);
		}
	}	  
	  
	
}