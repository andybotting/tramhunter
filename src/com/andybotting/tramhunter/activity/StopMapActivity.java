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
import java.util.List;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.andybotting.tramhunter.ui.BalloonItemizedOverlay;
import com.andybotting.tramhunter.ui.UIUtils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class StopMapActivity extends MapActivity {    
	
	private List<Overlay> mMapOverlays;

	private MapController mMapController;	
    private MapView mMapView;
    private MyLocationOverlay mMyLocationOverlay;
	private StopsList mStops;
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_map);

		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			mStops = extras.getParcelable("stopslist");
		} 

		String title = "Stops Map";
		if(mStops.size() == 1)
			title = mStops.get(0).getStopName();

		((TextView) findViewById(R.id.title_text)).setText(title);
		
		
       	mMapView = (MapView) findViewById(R.id.mapView);
       	mMapView.setBuiltInZoomControls(true);
       	
        mMapController = mMapView.getController();
        mMapOverlays = mMapView.getOverlays();
        
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMapOverlays.add(mMyLocationOverlay);

        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        
		// Home title button
		findViewById(R.id.title_btn_home).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	UIUtils.goHome(StopMapActivity.this);
		    }
		});	

		// My Location button
		findViewById(R.id.title_btn_myloc).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {     
		    	GeoPoint myLoc = mMyLocationOverlay.getMyLocation();
		    	if (myLoc != null) {
		    		mMapView.getController().animateTo(myLoc);
		    	}
		    	else {
		    		UIUtils.popToast(getApplicationContext(), "Unable to find your location");
		    	}
		    }
		});	
        
        displayStops(mStops);
    }
    
	
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }
	
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
    
    
	private void viewStop(Stop stop){
		int tramTrackerId = stop.getTramTrackerID();
		
		Bundle bundle = new Bundle();
		bundle.putInt("tramTrackerId", tramTrackerId);
		Intent intent = new Intent(StopMapActivity.this, StopDetailsActivity.class);
		intent.putExtras(bundle);
		
		startActivityForResult(intent, 1);
	}

	public class MyItemizedOverlay extends BalloonItemizedOverlay<OverlayItem> {

	    private ArrayList<OverlayItem> m_overlays = new ArrayList<OverlayItem>();

	    public MyItemizedOverlay(Drawable defaultMarker, MapView mapView) {
	        super(boundCenter(defaultMarker), mapView);
	    }

	    public void addOverlay(OverlayItem overlay) {
	        m_overlays.add(overlay);
	        populate();
	    }

	    @Override
	    protected OverlayItem createItem(int i) {
	        return m_overlays.get(i);
	    }

	    @Override
	    public int size() {
	        return m_overlays.size();
	    }

	    @Override
	    protected boolean onBalloonTap(int index) {
	    	Stop stop = mStops.get(index);
	        viewStop(stop);
	        return true;
	    }

	}

    private void displayStops(StopsList mStops) {

    	double minLat = 0;
    	double maxLat = 0;
    	double minLng = 0;
    	double maxLng = 0;
    	
    	Drawable redMarker = this.getResources().getDrawable(R.drawable.map_marker_red);
    	
    	int w = redMarker.getIntrinsicWidth();
    	int h = redMarker.getIntrinsicHeight();
    	redMarker.setBounds(-w/2, -h, w/2, 0);

    	
    	MyItemizedOverlay itemizedOverlay = new MyItemizedOverlay(redMarker, mMapView);
    	mMapOverlays.add(itemizedOverlay);
    	
    	for (Stop stop: mStops) {

    		GeoPoint point = stop.getGeoPoint();
    		
    		String title = stop.getPrimaryName();
    		
    		String snippet = "Stop " + stop.getFlagStopNumber();
    		// If the stop has a secondary name, add it
    		if (stop.getSecondaryName().length() > 0) {
    			snippet += ": " + stop.getSecondaryName();
    		}
    		snippet += " - " + stop.getCityDirection();
    		snippet += " (" + stop.getTramTrackerID() + ")";
    		
        	OverlayItem overlayitem = new OverlayItem(point, title, snippet);
        	
        	itemizedOverlay.addOverlay(overlayitem);
        	
        	Double lat = stop.getLatitude();
        	Double lng = stop.getLongitude();
        	
        	// Initialise our max/min values, if not set
        	if ((maxLat == minLat) && (minLat == 0)) {
        		minLat = lat;
        		maxLat = lat;
        		minLng = lng;
        		maxLng = lng;
        	}
        	
            if (lat < minLat) 
            	minLat = lat;
            
            if (lat > maxLat) 
            	maxLat = lat;
            
            if (lng < minLng) 
            	minLng = lng;
            
            if (lng > maxLng) 
            	maxLng = lng;
        }

        // Would be ideal to set map bounds here, but settle for center for now
        double centerLat = (maxLat + minLat) / 2;
        double centerLng = (maxLng + minLng) / 2;

        int lat1E6 = (int) (centerLat * 1E6);
        int lng1E6 = (int) (centerLng * 1E6);
        
        GeoPoint point = new GeoPoint(lat1E6, lng1E6);
        
        mMapController.setZoom(15);
        mMapController.setCenter(point);
    }
 
    
    @Override
    protected void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onStop() {
        mMyLocationOverlay.disableMyLocation();
        super.onStop();
    }
    
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
