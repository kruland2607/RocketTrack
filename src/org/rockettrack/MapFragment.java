/*
 * Copyright (C) 2013 François Girard
 * 
 * This file is part of Rocket Finder.
 *
 * Rocket Finder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Rocket Finder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Rocket Finder. If not, see <http://www.gnu.org/licenses/>.*/

package org.rockettrack;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapFragment extends RocketTrackBaseFragment {

	private final static String TAG ="MapFragment";
	
	// Map reference
	private GoogleMap mMap;

	// Map markers
	private Marker rocketMarker;
	private Circle rocketCircle;
	private Polyline rocketLine;
	private Polyline rocketPath;

	// Data
	private float rocketDistance = 0;
	private double maxAltitude;

	private List<LatLng> rocketPosList;

	private LatLng myPosition;

	private boolean followMe = false;

	public MapFragment() {
		super();
		rocketPosList = new ArrayList<LatLng>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.map_view, null, false);

		ToggleButton chkFollowMe = (ToggleButton) v.findViewById(R.id.chkFollowMe);
		followMe = chkFollowMe.isChecked();
		chkFollowMe.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				followMe = arg1;
			}

		});

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView(); 
		// Stackoverflow says this is necessary, but it seems to cause some troubles too.
		// It basically causes the map to be recreated with the view.  Look into the xml configuration
		// flags for the  SupportMapFragment object itself.
		Fragment fragment = (getFragmentManager().findFragmentById(R.id.map));  
		FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
		ft.remove(fragment);
		ft.commit();
		mMap = null;
	}


	@Override
	public void onLocationChanged(Location location) {
		super.onLocationChanged(location);
		Log.d(TAG,"onLocationChagned");
		if(mMap == null)
			return;

		myPosition = new LatLng(location.getLatitude(),location.getLongitude());

		if(rocketPosList.size() > 0){
			LatLng rocketPosition = rocketPosList.get(rocketPosList.size() -1);
			updateRocketLine(location,rocketPosition);
		}

		if( followMe ) {
			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).target(myPosition).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}
	}

	
	@Override
	protected void onDataChange() {
		Log.d(TAG,"onDataChange");
		updateRocketLocation();
	}

	private void setUpMapIfNeeded() {

		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = ((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}

	}

	private void setUpMap() {

		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
		mMap.setMyLocationEnabled(true);
		UiSettings mUiSettings = mMap.getUiSettings();
		mUiSettings.setCompassEnabled(true);
		mUiSettings.setMyLocationButtonEnabled(false);
		mUiSettings.setZoomControlsEnabled(false);

		updateRocketLocation();

	}

	private void  updateBearing() {
		float heading = getAzimuth();
		if ( followMe ) {
			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).bearing(heading).zoom(20).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}
		
		Float rocketBearing = getBearing();
		if ( rocketBearing != null ) {
			TextView lblBearing = (TextView) getView().findViewById(R.id.TextView01);
			lblBearing.setText("Bearing: " + rocketBearing );
		}
	}
	


	private void updateRocketLocation() {
		if (mMap == null ) {
			return;
		}
		
		Location rocketLocation = getRocketLocation();
		
		if(rocketLocation == null)
			return;

		LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());
		rocketPosList.add(rocketPosition);

		//Draw marker at Rocket position
		if(rocketMarker == null)
			rocketMarker = mMap.addMarker(new MarkerOptions().position(rocketPosition));
		else
			rocketMarker.setPosition(rocketPosition);
		if ( rocketCircle == null ) {
			CircleOptions options = new CircleOptions().center( rocketPosition ).radius( rocketLocation.getAccuracy() );
			rocketCircle = mMap.addCircle(options);
		} else {
			rocketCircle.setCenter(rocketPosition);
			rocketCircle.setRadius( rocketLocation.getAccuracy() );
		}

		Location myLoc = mMap.getMyLocation();
		//myLoc = null when the android gps is not initialized yet.
		if(myLoc == null)
		{
			rocketDistance = 0;
			return;
		}

		//update camera
		updateBearing();

		//Rocket Distance
		rocketDistance = myLoc.distanceTo(rocketLocation);
		TextView lblDistance = (TextView) getView().findViewById(R.id.lblDistance);
		lblDistance.setText("Rocket Distance: " + rocketDistance + "m");

		//Max Altitude
		double altitude = rocketLocation.getAltitude();// - myLoc.getAltitude();
		if(altitude > maxAltitude)
			maxAltitude = altitude;
		TextView lblMaxAltitude = (TextView) getView().findViewById(R.id.lblMaxAltitude);
		lblMaxAltitude.setText("Max Altitude: " + maxAltitude + "m");

		//Draw line between myPosition and Rocket
		LatLng myPosition = new LatLng(myLoc.getLatitude(),myLoc.getLongitude());

		if (rocketPath == null) {
			rocketPath = mMap.addPolyline(new PolylineOptions()
			.add(rocketPosList.get(0))
			.width(1.0f)
			.color(Color.rgb(0, 0, 128)));
		} 
		rocketPath.setPoints(rocketPosList);

		updateRocketLine(rocketLocation,rocketPosition);
	}

	private void updateRocketLine(Location rocketLocation,LatLng rocketPosition) {
		if(myPosition == null || rocketPosition == null)
			return;
		//Rocket Distance
		rocketDistance = mMap.getMyLocation().distanceTo(rocketLocation);
		TextView lblDistance = (TextView) getView().findViewById(R.id.lblDistance);
		lblDistance.setText("Rocket Distance: " + rocketDistance + "m");

		//Draw line between myPosition and Rocket

		if (rocketLine == null) {
			rocketLine = mMap.addPolyline(new PolylineOptions()
			.add(myPosition,rocketPosition)
			.width(1.0f)
			.color(Color.WHITE));
		} else {
			List<LatLng> positionList = new ArrayList<LatLng>();
			positionList.add(myPosition);
			positionList.add(rocketPosition);
			rocketLine.setPoints(positionList);
		}
	}

}