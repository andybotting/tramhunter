package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
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

import com.andybotting.tramhunter.ui.TouchListView;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;
import com.andybotting.tramhunter.util.StringUtil;

public class FavStopsListActivity extends ListActivity {

	private final static int CONTEXT_MENU_VIEW_STOP = 0;
	private final static int CONTEXT_MENU_STAR_STOP = 1;

	private StopsListAdapter mListAdapter;
	private ArrayList<Stop> mStops;
	private TramHunterDB mDB;
	private Context mContext;
	private PreferenceHelper mPreferenceHelper;
	
	private TouchListView mListView;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		setContentView(R.layout.stops_list);
		
		mListView = (TouchListView) getListView();
		mListView.setDropListener(onDrop);
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(FavStopsListActivity.this);
		    }
		});	

		// Search title button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(FavStopsListActivity.this);
		    }
		});	
		
		
        mContext = this.getBaseContext();
        mPreferenceHelper = new PreferenceHelper(mContext);
		mDB = new TramHunterDB(mContext);

		String title = "Favourite Stops";
		((TextView) findViewById(R.id.title_text)).setText(title);
		displayFavStops(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayFavStops(false);
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    	mDB.close();
    }
	
	public void displayFavStops(boolean alertIfNoStops) {
		mStops = mDB.getFavouriteStops(mContext);
		
		if (alertIfNoStops && mStops.size() == 0) {
			alertNoFavourites();		
		}
		else{
			// Find the routes for our stops
			for(Stop stop: mStops) {
				List<Route> routes = mDB.getRoutesForStop(stop.getTramTrackerID());
				stop.setRoutes(routes);
			}
		}
		displayStops();
	}

	private void alertNoFavourites() {
		final Intent routeListIntent = new Intent(this, RoutesListActivity.class);
		
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setMessage(R.string.no_favourite_stops)
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
		alert.setTitle("No Favourite Stops");
		alert.setIcon(R.drawable.icon);
		alert.show();
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
		Intent intent = new Intent(FavStopsListActivity.this, StopDetailsActivity.class);
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
    				boolean isStarred = mPreferenceHelper.isStarred(thisStop.getTramTrackerID());
    				mPreferenceHelper.setStopStar(thisStop.getTramTrackerID(), !isStarred);
    				// Refresh the adapter to show fav/unfav changes in list
    				mListAdapter.notifyDataSetChanged();		
    				return true;
        	}
    	} catch (ClassCastException e) {}
    	    	
		return super.onContextItemSelected(item);
    }
    
	private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			Stop item = mListAdapter.getItem(from);
			mListAdapter.remove(item);
			mListAdapter.insert(item, to);
			mListAdapter.notifyDataSetChanged();
			mPreferenceHelper.setStarredStopsString(StringUtil.makeStringFromStops(mStops));
		}
	};

	private class StopsListAdapter extends BaseAdapter {

		public int getCount() {
			return mStops.size();
		}

		public void insert(Stop item, int to) {
			mStops.add(to, item);
		}

		public void remove(Stop item) {
			mStops.remove(item);
		}

		public Stop getItem(int position) {
			return mStops.get(position);
		}
		
		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pv;
            if(convertView == null) {
    			LayoutInflater inflater = getLayoutInflater();
    			pv = inflater.inflate(R.layout.touch_list_row2, parent, false);
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
						
			// Only show the routes if this is the favourites view, it's too slow with LOTS of stops
			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(thisStop.getRoutesListString());
			
			return pv;
		}
			
	}
	
}