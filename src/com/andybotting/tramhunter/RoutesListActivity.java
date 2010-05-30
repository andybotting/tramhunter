package com.andybotting.tramhunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import android.app.Activity;
import android.app.ListActivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.content.ContentUris;

import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RoutesListActivity extends ListActivity {

	ListView listView;
	Route selectedRoute;
	Vector<Route> routes;
	  
	public String[] routeNumbers;
	public String[] routeDestinations;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		setContentView(R.layout.routes_list);
		
		listView = (ListView)this.findViewById(android.R.id.list);
		
		String title = getResources().getText(R.string.app_name) + ": Routes";
		setTitle(title);
		
		displayRoutes();
	  
	}
		
	public void displayRoutes() {

		TramHunterDB db = new TramHunterDB(this);
		routes = db.getRoutes();

		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
				Bundle bundle = new Bundle();
				bundle.putLong("routeId", id);
				Intent stopsListIntent = new Intent(RoutesListActivity.this, StopsListActivity.class);
				stopsListIntent.putExtras(bundle);
				startActivityForResult(stopsListIntent, 1);
			}

		});

		ScrollView sv = new ScrollView(this);
		sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		LinearLayout layout = new LinearLayout(this);
		layout.setPadding(10, 10, 10, 0);
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));		
		
		layout.setOrientation(LinearLayout.VERTICAL);	
		
		routeNumbers = new String[routes.size()];
		routeDestinations = new String[routes.size()];
		
		for (int i = 0; i < routes.size(); i++) {
			Route thisRoute = routes.get(i);
			routeNumbers[i] = thisRoute.getNumber();
			routeDestinations[i] = thisRoute.getDestination();
		}
		
		setListAdapter(new RoutesListAdapter(this));
	}
  
	
	private class RoutesListAdapter extends BaseAdapter {
		
		private Context mContext;		
		private int usenameHeight;

			public RoutesListAdapter(Context context) {
				mContext = context;
			}

			public int getCount() {
				return routes.size();
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
				
				if (pv == null) {
					LayoutInflater inflater = getLayoutInflater();
					pv = inflater.inflate(R.layout.routes_row, parent, false);
					
					wrapper = new ViewWrapper(pv);
					if (position == 0) {
						usenameHeight = wrapper.getRouteDestination().getHeight();
					}
					
					pv.setTag(wrapper);
					
					wrapper = new ViewWrapper(pv);
					pv.setTag(wrapper);
				} else {
					wrapper = (ViewWrapper) pv.getTag();
				}
				
				pv.setId(0);
				
				if (wrapper.getRouteDestination().getHeight() == 0) {
					wrapper.getRouteDestination().setHeight( (int) wrapper.getRouteNumber().getTextSize() + wrapper.getRouteDestination().getPaddingBottom());
				}

				wrapper.getRouteNumber().setText("Route " + routeNumbers[position]);
				wrapper.getRouteDestination().setText("To " + routeDestinations[position]);

				return pv;

			}

			

		}

		class ViewWrapper {
			View base;
			
			TextView routeNumber = null;
			TextView routeDestination = null;
			

			ViewWrapper(View base) {
				this.base = base;
			}

			TextView getRouteNumber() {
				if (routeNumber == null) {
					routeNumber = (TextView) base.findViewById(R.id.routeNumber);
				}
				return (routeNumber);
			}

			TextView getRouteDestination() {
				if (routeDestination == null) {
					routeDestination = (TextView) base.findViewById(R.id.routeDestination);
				}
				return (routeDestination);
			}

		}
	  
	  
}
