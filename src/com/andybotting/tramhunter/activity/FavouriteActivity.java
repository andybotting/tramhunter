package com.andybotting.tramhunter.activity;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.andybotting.tramhunter.ui.TouchListView;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Favourite;
import com.andybotting.tramhunter.objects.FavouriteList;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;

public class FavouriteActivity extends ListActivity {
	
    private static final String TAG = "FavouriteActivity";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.INFO);
	
	private final static int CONTEXT_MENU_VIEW_STOP = 1;
	private final static int CONTEXT_MENU_SET_NAME = 0;

	private FavouritesListAdapter mListAdapter;
	private FavouriteList mFavourites;	
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
		    	UIUtils.goHome(FavouriteActivity.this);
		    }
		});	

		// Search title button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(FavouriteActivity.this);
		    }
		});	

		mFavourites = new FavouriteList();

		String title = "Favourite Stops";
		((TextView) findViewById(R.id.title_text)).setText(title);
		displayFavStops(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayFavStops(false);
	}
	
	
	/**
	 * 
	 * @param alertIfNoFavourites
	 */
	public void displayFavStops(boolean alertIfNoFavourites) {
		if (alertIfNoFavourites && !mFavourites.hasFavourites())
			alertNoFavourites();
		
		displayStops();
	}

	
	
	/**
	 * 
	 */
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
	
	
	/**
	 * 
	 */
	public void displayStops() {
		mFavourites = new FavouriteList();
		mListAdapter = new FavouritesListAdapter();
		
		mListView.setOnItemClickListener(mListView_OnItemClickListener);		
		mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
		setListAdapter(mListAdapter);
	}
	
	
	/**
	 * 
	 * @param stop
	 * @param route
	 */
	private void viewStop(Stop stop, Route route) {
		int tramTrackerId = stop.getTramTrackerID();
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		
		if (route != null)
			bundle.putInt("routeId", route.getId());
		
		Intent intent = new Intent(FavouriteActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		startActivityForResult(intent, 1);
	}
	
	
	/**
	 * 
	 * @param favourite
	 */
	private void nameStop(final Favourite favourite) {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setTitle(R.string.dialog_set_name);
		alert.setIcon(R.drawable.icon);
		
		final CharSequence message = getString(R.string.dialog_set_name_message, favourite.getName());
		alert.setMessage(message);

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = input.getText().toString().trim();
				favourite.setName(name);
				mFavourites.writeFavourites();
				displayStops();
			}
		});

        alert.setNeutralButton("Reset", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	favourite.clearName();
				mFavourites.writeFavourites();
				displayStops();
            }
        });
		
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		alert.show();
		
	}
	
	
	/**
	 * 
	 */
	private OnItemClickListener mListView_OnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View row, int position, long id) {
			Favourite favourite = mFavourites.getFavourite(position);
			viewStop(favourite.getStop(), favourite.getRoute());
		}
    };

    
    /**
     * 
     */
	private OnCreateContextMenuListener mListView_OnCreateContextMenuListener = new OnCreateContextMenuListener() {
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info;
			try {
			    info = (AdapterView.AdapterContextMenuInfo)menuInfo;
			} catch (ClassCastException e) {
			    return;
			}

			Favourite favourite = mFavourites.getFavourite(info.position);
			
			menu.add(0, CONTEXT_MENU_SET_NAME, 0, "Set Stop Name");
			menu.add(0, CONTEXT_MENU_VIEW_STOP, 1, "View Stop");
		}
    };
    
    
    /**
     * 
     */
    @Override
    public boolean onContextItemSelected (MenuItem item){
    	try {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    		Favourite favourite = mFavourites.getFavourite(info.position);
        	
        	switch (item.getItemId()) {
        	
    			case CONTEXT_MENU_VIEW_STOP:
    				viewStop(favourite.getStop(), favourite.getRoute());
    				return true;
    				
    			case CONTEXT_MENU_SET_NAME:
    				// Toggle favourite
    				nameStop(favourite);
    				return true;
        	}
    	} catch (ClassCastException e) {}
    	    	
		return super.onContextItemSelected(item);
    }
    
    
    /**
     * 
     */
	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			Favourite favourite = mListAdapter.getItem(from);
			mListAdapter.remove(favourite);
			mListAdapter.insert(favourite, to);
			mListAdapter.notifyDataSetChanged();
			//mPreferenceHelper.setStarredStopsString(mFavourites.toString());
		}
	};

	
	/**
	 * 
	 * @author andy
	 *
	 */
	private class FavouritesListAdapter extends BaseAdapter {

		public int getCount() {
			return mFavourites.getCount();
		}

		public void insert(Favourite fav, int to) {
			mFavourites.addFavourite(fav, to);
		}

		public void remove(Favourite fav) {
			mFavourites.removeFavourite(fav);
		}

		public Favourite getItem(int position) {
			return mFavourites.getFavourite(position);
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
			
			Favourite favourite = mFavourites.getFavourite(position);
			Stop stop = favourite.getStop();
			
			String stopName;
			String stopDetails;
			
			if (favourite.hasName()) {
				stopName = favourite.getName();
				stopDetails = stop.getPrimaryName();
				stopDetails += ", Stop " + stop.getFlagStopNumber();
				// If the stop has a secondary name, add it
				if (stop.getSecondaryName().length() > 0)
					stopDetails += ": " + stop.getSecondaryName();
				
				stopDetails += " - " + stop.getCityDirection();
				stopDetails += " (" + stop.getTramTrackerID() + ")";				
			}
			else {
				stopName = stop.getPrimaryName();
				stopDetails = "Stop " + stop.getFlagStopNumber();
				// If the stop has a secondary name, add it
				if (stop.getSecondaryName().length() > 0)
					stopDetails += ": " + stop.getSecondaryName();
				
				stopDetails += " - " + stop.getCityDirection();
				stopDetails += " (" + stop.getTramTrackerID() + ")";
			}

			((TextView) pv.findViewById(R.id.stopNameTextView)).setText(stopName);
			((TextView) pv.findViewById(R.id.stopDetailsTextView)).setText(stopDetails);
			((TextView) pv.findViewById(R.id.stopRoutesTextView)).setText(favourite.getRouteName());
			
			return pv;
		}
			
	}
	
}