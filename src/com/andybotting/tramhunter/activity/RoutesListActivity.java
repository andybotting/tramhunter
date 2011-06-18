/*  
 * Copyright 2010 Andy Botting <andy@andybotting.com>  
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This file is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 * General Public License for more details.  
 *  
 * You should have received a copy of the GNU General Public License  
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 *  
 * This file incorporates work covered by the following copyright and  
 * permission notice:
 * 
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Destination;
import com.andybotting.tramhunter.objects.Route;
import com.andybotting.tramhunter.ui.UIUtils;

import android.app.ExpandableListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;


import android.widget.ExpandableListView.OnChildClickListener;

public class RoutesListActivity extends ExpandableListActivity {

	private List<Route> routes;
	private List<Destination> destinations;
	private ExpandableListAdapter mAdapter;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);	
		
		setContentView(R.layout.routes_list);
		
		String title = "Routes";
		((TextView) findViewById(R.id.title_text)).setText(title);
		
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(RoutesListActivity.this);
		    }
		});	

		// Search title button
		findViewById(R.id.title_btn_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goSearch(RoutesListActivity.this);
		    }
		});	
		
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
	
	
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }
	
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
	
		
	public void displayRoutes() {
				
		TramHunterDB db = new TramHunterDB();
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
                new int[] { R.id.route_name, R.id.route_dest },
                childData,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { "destination" },
                new int[] { android.R.id.text1, android.R.id.text2 }
                );
        
        setListAdapter(mAdapter);

	}

	  
}
