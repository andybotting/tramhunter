package com.andybotting.tramhunter.activity;

import java.util.List;

import android.app.ListActivity;
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
import com.andybotting.tramhunter.dao.TramHunterDB;

public class RoutesListActivity extends ListActivity {

	private ListView listView;
	private List<Route> routes;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		setContentView(R.layout.routes_list);
		
		listView = (ListView)this.findViewById(android.R.id.list);
		
		String title = "Routes";
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

		
		setListAdapter(new RoutesListAdapter());
	}
  
	
	private class RoutesListAdapter extends BaseAdapter {

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

			LayoutInflater inflater = getLayoutInflater();
			pv = inflater.inflate(R.layout.routes_row, parent, false);
					
			wrapper = new ViewWrapper(pv);
			pv.setTag(wrapper);

			Route thisRoute = routes.get(position);
				
			wrapper.getRouteNumber().setText("Route " + thisRoute.getNumber());
			wrapper.getRouteDestination().setText("To " + thisRoute.getDestination());

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
