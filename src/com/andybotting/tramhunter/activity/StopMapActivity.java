package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.StopsList;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class StopMapActivity extends MapActivity 
{    
	
	List<Overlay> mapOverlays;
	Drawable drawable;
	MapItemizedOverlay itemizedOverlay;
	
	LinearLayout linearLayout;
	MapView mapView;
	MapController mapController;
	
	StopsList mStops;
	
	Stop stop;
	
	@SuppressWarnings("unchecked")
	public class MapItemizedOverlay extends ItemizedOverlay {
		
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		
		public MapItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		public void addOverlay(OverlayItem overlay) {
		    mOverlays.add(overlay);
		    populate();
		}
		
		@Override
		protected OverlayItem createItem(int i) {
		  return mOverlays.get(i);
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_map);
        
        double minLat = 0;
        double maxLat = 0;
        double minLng = 0;
        double maxLng = 0;
     
//        int tramTrackerId = 0;
        
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			//tramTrackerId = extras.getInt("tramTrackerId");
			mStops = extras.getParcelable("stopslist");
			Log.d("Testing", "Found stops: " + mStops.size());
		} 
		      
		// Get our Stop object
//		TramHunterDB db = new TramHunterDB(this);
//		stop = db.getStop(tramTrackerId);
//		db.close();
				
		// Set title
//		String title = "Stop " + stop.getFlagStopNumber() + ": " + stop.getStopName();
//		setTitle(title);
		

       	mapView = (MapView) findViewById(R.id.mapView);
        mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        
        mapOverlays = mapView.getOverlays();
        
        // Draw our markers
        
        for (Stop stop: mStops) {
        	Log.d("Testing", "Found Stop: " + stop.getPrimaryName());
        	drawable = this.getResources().getDrawable(R.drawable.map_marker);
        	itemizedOverlay = new MapItemizedOverlay(drawable);
        	GeoPoint point = stop.getGeoPoint();
        	OverlayItem overlayitem = new OverlayItem(point, stop.getStopName(), stop.getStopName());
        	itemizedOverlay.addOverlay(overlayitem);
        	mapOverlays.add(itemizedOverlay);

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
        
        mapController.setZoom(15);
        mapController.setCenter(point);
        
        
    }
 
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
