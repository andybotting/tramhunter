package com.andybotting.tramhunter.activity;

import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Route;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.dao.TramHunterDB;

public class StopsListActivity extends ListActivity {

	private boolean isFavouritesView;
	private ListView listView;
	private List<Stop> stops;
	private TramHunterDB db;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		long routeId = -1;
		
		setContentView(R.layout.stops_list);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		String title = "";
		db = new TramHunterDB(this);
		
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
		  routeId = extras.getLong("routeId");
		}  
		
				
		// Are we looking for stops for a route, or fav stops?
		if (routeId > -1) {
			final Route route = db.getRoute(routeId);
			title = "Stops for Route " + route.getNumber();
			setTitle(title);
			displayStopsForRoute(routeId);
		}
		else {
			isFavouritesView = true;
			title = "Favourite Stops";
			setTitle(title);
			displayFavStops(true);
		}
		
	}
	  
	@Override
	protected void onResume() {
		super.onResume();
		
		// Refresh favourites if the back button is pressed
		if (isFavouritesView)
		{
			displayFavStops(false);
		}
	}



	public void displayFavStops(boolean alertIfNoStops) {
		stops = db.getFavouriteStops();
		
		if (alertIfNoStops && stops.size() == 0) {
			alertNoFavourites();		
		}
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
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//  Action for 'NO' Button
					dialog.cancel();				
					// TODO: getParent() instead?
					StopsListActivity.this.finish();
				}
			});
		
		AlertDialog alert = dialogBuilder.create();
		// Title for AlertDialog
		alert.setTitle("No Favourite Stops");
		// Icon for AlertDialog
		alert.setIcon(R.drawable.icon);
		alert.show();
	}
	
	public void displayStopsForRoute(long routeId) {
		stops = db.getStopsForRoute(routeId);
		displayStops();
	}

	
	public void displayStops() {
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
				
				Stop thisStop = (Stop) stops.get(position);
				int tramTrackerId = thisStop.getTramTrackerID();
							
				Bundle bundle = new Bundle();
				bundle.putInt("tramTrackerId", tramTrackerId);
				Intent intent = new Intent(StopsListActivity.this, StopDetailsActivity.class);
				intent.putExtras(bundle);
				startActivityForResult(intent, 1);
			}

		});		

		setListAdapter(new StopsListAdapter());
	}

	
	  
	private class StopsListAdapter extends BaseAdapter {

		public int getCount() {
			return stops.size();
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

			Stop thisStop = (Stop) stops.get(position);
			
			String textLabel1 = thisStop.getPrimaryName();
						
			String textLabel2 = "Stop " + thisStop.getFlagStopNumber();

			// If the stop has a secondary name, add it
			if (thisStop.getSecondaryName().length() > 0) {
				textLabel2 += ": " + thisStop.getSecondaryName();
			}
			
			textLabel2 += " - " + thisStop.getCityDirection();
			
			wrapper.getTextLabel1().setText(textLabel1);
			wrapper.getTextLabel2().setText(textLabel2);

			return pv;

		}

	}


	class ViewWrapper {
		View base;
				
		TextView textLabel1 = null;
		TextView textLabel2 = null;
		

		ViewWrapper(View base) {
			this.base = base;
		}

		TextView getTextLabel1() {
			if (textLabel1 == null) {
				textLabel1 = (TextView) base.findViewById(R.id.textLabel1);
			}
			return (textLabel1);
		}

		TextView getTextLabel2() {
			if (textLabel2 == null) {
				textLabel2 = (TextView) base.findViewById(R.id.textLabel2);
			}
			return (textLabel2);
		}

	}	  
	  
	
}