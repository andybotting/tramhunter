package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.Route;
import com.andybotting.tramhunter.Destination;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;


import android.widget.ExpandableListView.OnChildClickListener;

public class RoutesListActivity extends ExpandableListActivity {

	private List<Route> routes;
	private List<Destination> destinations;
	private ExpandableListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	  
		
		String title = "Routes";
		setTitle(title);
		
		getExpandableListView().setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				long destinationId = -1;
				
				Route route = routes.get(groupPosition);
				destinations = route.getDestinations();
				destinationId = destinations.get(childPosition).getId();
				
				Bundle bundle = new Bundle();
				bundle.putLong("destinationId", destinationId);
				Intent stopsListIntent = new Intent(RoutesListActivity.this, StopsListActivity.class);
				stopsListIntent.putExtras(bundle);
				startActivityForResult(stopsListIntent, 1);
				return true;
			}
		});
	 
		displayRoutes();
	}
	
	
		
	public void displayRoutes() {
				
		TramHunterDB db = new TramHunterDB(this);
		routes = db.getRoutes();
		db.close();
				
        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
        
        for (int i = 0; i < routes.size(); i++) {
        	Route route = routes.get(i);
        	Map<String, String> curGroupMap = new HashMap<String, String>();
            curGroupMap.put("route", "Route " + route.getNumber());
            curGroupMap.put("to", route.getDestinationString());
            groupData.add(curGroupMap);
            
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
            List<Destination> destinations = route.getDestinations();
            
            for (Destination d : destinations) {
            	Map<String, String> curChildMap = new HashMap<String, String>();
            	curChildMap.put("destination", d.getDestination());
            	children.add(curChildMap);
            }

            childData.add(children);
            
        }
		
		
        // Set up our adapter
        mAdapter = new SimpleExpandableListAdapter(
                this,
                groupData,
                R.layout.routes_list_row,
                new String[] { "route", "to" },
                new int[] { R.id.textLine1, R.id.textLine2 },
                childData,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "destination" },
                new int[] { android.R.id.text1, android.R.id.text2 }
                );
        
        setListAdapter(mAdapter);

	}

	  
}
