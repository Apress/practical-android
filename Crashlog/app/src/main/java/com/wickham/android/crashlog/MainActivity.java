package com.wickham.android.crashlog;

/*
 * Copyright (C) 2017 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.io.File;
import java.util.List;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity {

    private MapView mMapView;
    private IMapController mapController;
    private ScaleBarOverlay mScaleBarOverlay;
    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;
    private org.osmdroid.views.overlay.MinimapOverlay mMinimapOverlay;
    //private ResourceProxy mResProx;
    private org.osmdroid.views.overlay.compass.CompassOverlay mCompassOverlay = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DisplayMetrics dm = this.getResources().getDisplayMetrics();

        // Setup the ActionBar and the Spinner in the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle("Practical Android");
        getActionBar().setTitle("Crash Logging");

        setContentView(R.layout.main_layout);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(Environment.getExternalStorageDirectory(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Environment.getExternalStorageDirectory(), "osmdroid/tiles"));

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mMapView.setClickable(false);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        mapController = mMapView.getController();

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Get current location
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER

        GeoPoint p1;
        try {
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            int lat = (int) (lastKnownLocation.getLatitude() * 1E6);
            int lng = (int) (lastKnownLocation.getLongitude() * 1E6);
            p1 = new GeoPoint(lat, lng);
            mapController.setCenter(p1);
        } catch (SecurityException e) {
            // Let the user know there was a problem with GPS
        }

        mapController.setZoom(14);

        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mMapView);

        mMapView.getOverlays().add(this.mLocationOverlay);
        mLocationOverlay.enableMyLocation();
        
        mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        mMapView.getOverlays().add(this.mScaleBarOverlay);
        
        mMinimapOverlay = new org.osmdroid.views.overlay.MinimapOverlay(this, mMapView.getTileRequestCompleteHandler());
        mMinimapOverlay.setWidth(dm.widthPixels / 5);
        mMinimapOverlay.setHeight(dm.heightPixels / 5);
        mMapView.getOverlays().add(this.mMinimapOverlay);
        
        mCompassOverlay = new org.osmdroid.views.overlay.compass.CompassOverlay(this, mMapView);
        mCompassOverlay.enableCompass();
        mMapView.getOverlays().add(this.mCompassOverlay);
        
        mRotationGestureOverlay = new RotationGestureOverlay(this, mMapView);
        mRotationGestureOverlay.setEnabled(true);
    }
    
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	new MenuInflater(this).inflate(R.menu.actions, menu);
    	return(super.onCreateOptionsMenu(menu));
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.exit) {
    		finish();
    		return(true);
    	}
    	if (item.getItemId() == R.id.crash) {
			// simulate a crash right here, lets do an Index out of bounds condition ...
            String[] crashArray = new String[] {"one","two"};
			crashArray[2] = "oops";
    		return(true);
    	}
    	return(super.onOptionsItemSelected(item));
    }    
}