package com.andybotting.tramhunter;

import java.util.Vector;

import com.andybotting.tramhunter.RoutesListActivity.ViewWrapper;

import android.app.ListActivity;

import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class StopsListActivity extends ListActivity {

	ListView listView;
	Vector<Stop> stops;
	TramHunterDB db;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		long routeId = -1;
		
		setContentView(R.layout.stops_list);
		listView = (ListView)this.findViewById(android.R.id.list);
		
		String title = (String) getResources().getText(R.string.app_name);
				
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
		  routeId = extras.getLong("routeId");
		}  
		
		db = new TramHunterDB(this);
				
		// Are we looking for stops for a route, or fav stops?
		if (routeId > -1) {
			title += ": Stops for Route";
			setTitle(title);
			displayStopsForRoute(routeId);
		}
		else {
			title += ": Favourite Stops";
			setTitle(title);
			displayFavStops();
		}
		
	}
	  
	
	public void displayFavStops() {
		stops = db.getFavouriteStops();
		displayStops();
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

		setListAdapter(new StopsListAdapter(this));
	}

	
	  
	private class StopsListAdapter extends BaseAdapter {
			
		private Context mContext;		
		private int usenameHeight;

		public StopsListAdapter(Context context) {
			mContext = context;
		}

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