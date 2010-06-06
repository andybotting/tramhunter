package com.andybotting.tramhunter.activity;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.andybotting.tramhunter.MapItemizedOverlay;
import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.Stop;
import com.andybotting.tramhunter.R.drawable;
import com.andybotting.tramhunter.R.id;
import com.andybotting.tramhunter.R.layout;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import com.google.android.maps.MapView.LayoutParams;  
import android.view.View;
import android.widget.LinearLayout;

public class StopMapActivity extends MapActivity 
{    
	
	List<Overlay> mapOverlays;
	Drawable drawable;
	MapItemizedOverlay itemizedOverlay;
	
	LinearLayout linearLayout;
	MapView mapView;
	MapController mapController;
	
	Stop stop;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_map);
     
        int tramTrackerId = 0;
        
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			tramTrackerId = extras.getInt("tramTrackerId");	
		} 
		      
		// Get our Stop object
		TramHunterDB db = new TramHunterDB(this);
		stop = db.getStop(tramTrackerId);
		db.close();
				
		// Set title
		String title = "Stop " + stop.getFlagStopNumber() + ": " + stop.getStopName();
		setTitle(title);
		

       	mapView = (MapView) findViewById(R.id.mapView);
        mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.map_marker);
        
        itemizedOverlay = new MapItemizedOverlay(drawable);
        
        GeoPoint point = stop.getGeoPoint();
        OverlayItem overlayitem = new OverlayItem(point, stop.getStopName(), stop.getStopName());
        
        itemizedOverlay.addOverlay(overlayitem);
        mapOverlays.add(itemizedOverlay);
        
        mapController.setZoom(16);
        mapController.setCenter(point);
    }
 
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
