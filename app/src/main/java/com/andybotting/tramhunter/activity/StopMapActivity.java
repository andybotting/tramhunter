/*
 * Copyright 2013 Andy Botting <andy@andybotting.com>
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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.andybotting.tramhunter.R;
import com.andybotting.tramhunter.objects.Stop;
import com.andybotting.tramhunter.objects.StopsList;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class StopMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final int REQUEST_PERMISSION_LOCATION = 101;
    private static final String TAG = "StopMapActivity";
    private GoogleMap mMapView;
    private StopsList mStops;

    private CameraPosition mRestoreCameraPosition;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stop_map);

        // Set up the Action Bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Get bundle data
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            mStops = extras.getParcelable("stopslist");

        if (mStops.size() == 1) {
            actionBar.setTitle(mStops.get(0).getStopName());
            actionBar.setSubtitle(mStops.get(0).getStopDetailsLine());
        }

        if(savedInstanceState!=null){
            mRestoreCameraPosition = savedInstanceState.getParcelable("mRestoreCameraPosition");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mMapView!=null)
            outState.putParcelable("mRestoreCameraPosition", mMapView.getCameraPosition());
    }

    /**
     * Options item select
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId())
        {

            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void viewStop(Stop stop) {
        int tramTrackerId = stop.getTramTrackerID();

        Bundle bundle = new Bundle();
        bundle.putInt("tramTrackerId", tramTrackerId);
        Intent intent = new Intent(StopMapActivity.this, StopDetailsActivity.class);
        intent.putExtras(bundle);

        startActivityForResult(intent, 1);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if(marker.getTag() instanceof Long){
            //clicked a marker with a StopId tag
            long stopId = (long) marker.getTag();
            for(Stop stop : mStops){
                if(stop.getId()==stopId){
                    viewStop(stop);
                    break;
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMapView = googleMap;
        mMapView.setOnInfoWindowClickListener(this);
        UiSettings uiSettings = mMapView.getUiSettings();
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setMapToolbarEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        }
        else{
            mMapView.setMyLocationEnabled(true);
        }
        displayStops(mStops, mRestoreCameraPosition==null);
        if(mRestoreCameraPosition!=null){
            mMapView.moveCamera(CameraUpdateFactory.newCameraPosition(mRestoreCameraPosition));
            mRestoreCameraPosition=null;
        }
    }

    private void displayStops(StopsList mStops, boolean reCentre) {

        if(mStops.size()==0) return;

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        for (Stop stop : mStops) {

            LatLng point = stop.getLatLng();

            String title = stop.getPrimaryName();
            String details = stop.getStopDetailsLine();


            bounds.include(point);

            MarkerOptions options = new MarkerOptions()
                    .title(title)
                    .snippet(details)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_red))
                    .anchor(0.5f, 1f)
                    .position(point);
            Marker marker = mMapView.addMarker(options);
            marker.setTag(stop.getId());

        }

        if(!reCentre) return;

        CameraUpdate cameraUpdate = null;
        if(mStops.size()>1) try {
            // calculate a zoom that will fit all the stops.
            // Don't do it if only 1 stop, because the zoom is too much
            int padding = getResources().getDimensionPixelSize(R.dimen.map_bounds_padding);
            //throws IllegalStateException if map has not undergone layout yet
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.build(), padding);
            mMapView.moveCamera(cameraUpdate);
        }catch (Exception e1) {
            Log.e(TAG, "Map not ready", e1);
            cameraUpdate=null;
        }
        if(cameraUpdate==null) try{
            //if that failed, or there's only 1 stop, zoom of 15.
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(bounds.build().getCenter(), 15);
            mMapView.moveCamera(cameraUpdate);
        }catch (Exception e2){
            Log.e(TAG, "Map still not ready", e2);
        }

    }



}
