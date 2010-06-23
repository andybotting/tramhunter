package com.andybotting.tramhunter.activity;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.dao.TramHunterDB;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class StopMapActivity extends MapActivity 
{    
	
	private List<Overlay> mMapOverlays;

	private MapController mMapController;	
    private MapView mMapView;
    private MyLocationOverlay mMyLocationOverlay;
	
	private StopsList mStops;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_map);

//        int tramTrackerId = 0;
        
		// Get bundle data
		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			//tramTrackerId = extras.getInt("tramTrackerId");
			mStops = extras.getParcelable("stopslist");
		} 

       	mMapView = (MapView) findViewById(R.id.mapView);
       	mMapView.setBuiltInZoomControls(true);
       	
        mMapController = mMapView.getController();
        mMapOverlays = mMapView.getOverlays();
        
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMapOverlays.add(mMyLocationOverlay);

        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        
        displayStops(mStops);
    }
        
	
	
	@SuppressWarnings("unchecked")
	public class MapItemizedOverlay extends ItemizedOverlay {
		
		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		private	PopupPanel panel = new PopupPanel(R.layout.stop_map_popup);
		
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
		
		@Override
		protected boolean onTap(int i) {
			Stop stop = null;
			
			OverlayItem item = getItem(i);
			GeoPoint geoPoint = item.getPoint();
			Point pt = mMapView.getProjection().toPixels(geoPoint, null);
			
			View view = panel.getView();
			
			// Parse the marker title into an int
			int tramTrackerId = Integer.parseInt(item.getTitle());
			
			TramHunterDB db = new TramHunterDB(getBaseContext());
			stop = db.getStop(tramTrackerId);
	
			// Set labels from Stop hash map
			String firstLineText = stop.getPrimaryName();	
			
			String secondLineText = "Stop " + stop.getFlagStopNumber();
			// If the stop has a secondary name, add it
			if (stop.getSecondaryName().length() > 0) {
				secondLineText += ": " + stop.getSecondaryName();
			}
			secondLineText += " - " + stop.getCityDirection();
			
			stop.setRoutes(db.getRoutesForStop(tramTrackerId));
			
			String thirdLineText = stop.getRoutesString();
			
			((TextView)view.findViewById(R.id.stopNameTextView)).setText(firstLineText);
			((TextView)view.findViewById(R.id.stopDetailsTextView)).setText(secondLineText);
			((TextView)view.findViewById(R.id.stopRoutesTextView)).setText(thirdLineText);

			db.close();
			
			panel.show(pt.y*2 > mMapView.getHeight());
			
			return(true);
		}
		
	}

	class PopupPanel {
		View popup;
		boolean isVisible = false;
		
		PopupPanel(int layout) {
			ViewGroup parent = (ViewGroup) mMapView.getParent();
			popup = getLayoutInflater().inflate(layout, parent, false);
			popup.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					hide();
				}
			});
		}
		
		View getView() {
			return(popup);
		}
		
		void show(boolean alignTop) {
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			
			if (alignTop) {
				lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				lp.setMargins(0, 20, 0, 0);
			}
			else {
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				lp.setMargins(0, 0, 0, 60);
			}
			
			hide();
			
			((ViewGroup)mMapView.getParent()).addView(popup, lp);
			isVisible = true;
		}
		
		void hide() {
			if (isVisible) {
				isVisible = false;
				((ViewGroup)popup.getParent()).removeView(popup);
			}
		}
	}

	


    private void displayStops(StopsList mStops) {

    	double minLat = 0;
    	double maxLat = 0;
    	double minLng = 0;
    	double maxLng = 0;
    	
    	Drawable drawable = this.getResources().getDrawable(R.drawable.map_marker);
    	MapItemizedOverlay itemizedOverlay = new MapItemizedOverlay(drawable);
    	mMapOverlays.add(itemizedOverlay);
    	
    	for (Stop stop: mStops) {

    		GeoPoint point = stop.getGeoPoint();
        	OverlayItem overlayitem = new OverlayItem(point, String.valueOf(stop.getTramTrackerID()), null);
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
        
//        mMyLocationOverlay.runOnFirstFix(new Runnable() { 
//        	public void run() {
//        		mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
//        	}
//        });
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
