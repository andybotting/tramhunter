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

import java.util.List;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.ui.UIUtils;
import com.andybotting.tramhunter.util.PreferenceHelper;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

public class SearchActivity extends ListActivity {

	private final static int CONTEXT_MENU_VIEW_STOP = 0;
	private final static int CONTEXT_MENU_STAR_STOP = 1;
	
	private ListView mListView;
	private StopsListAdapter mListAdapter;
	private List<Stop> mStops;
	private TramHunterDB mDB;

	private Context mContext;
	private PreferenceHelper mPreferenceHelper;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_list);
        mListView = (ListView)this.findViewById(android.R.id.list);
        
		// Home button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {       
		    	UIUtils.goHome(SearchActivity.this);
		    }
		});	
        
		mContext = this.getBaseContext();
        mPreferenceHelper = new PreferenceHelper(mContext);
		mDB = new TramHunterDB(mContext);
        

		// Test our intent action in case it's a search
		Intent intent = getIntent();
		
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // from click on search results
            int tramTrackerId = Integer.parseInt(intent.getDataString());
            Intent next = new Intent();
            next.setClass(this, StopDetailsActivity.class);
            next.putExtra("tramTrackerId", tramTrackerId);
            startActivity(next);
            finish();
        } 
        else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY); 
            final CharSequence title = getString(R.string.title_search_query, query);
            ((TextView) findViewById(R.id.title_text)).setText(title);
			mStops = mDB.getStopsForSearch(query);
			
			mListView.setOnItemClickListener(mListView_OnItemClickListener);		
			mListView.setOnCreateContextMenuListener(mListView_OnCreateContextMenuListener);
			mListAdapter = new StopsListAdapter();
			setListAdapter(mListAdapter);
        }
        
    }

	
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(this, StopDetailsActivity.class);
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
//			menu.add(0, CONTEXT_MENU_STAR_STOP, 0, (mPreferenceHelper.isStarred(thisStop.getTramTrackerID()) ? "Unfavourite" : "Favourite"));
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
//    			case CONTEXT_MENU_STAR_STOP:
//    				// Toggle favourite
//    				mPreferenceHelper.setStopStar(thisStop.getTramTrackerID(), !mPreferenceHelper.isStarred(thisStop.getTramTrackerID()));
//    				// Refresh the adapter to show fav/unfav changes in list
//    				mListAdapter.notifyDataSetChanged();
//    				return true;
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

//			if (mPreferenceHelper.isStarred(thisStop.getTramTrackerID()))
//				((ImageView) pv.findViewById(R.id.starImageView)).setVisibility(View.VISIBLE);
//			else
//				((ImageView) pv.findViewById(R.id.starImageView)).setVisibility(View.INVISIBLE);

			
			return pv;
		}
			
	}
    
    
    

}