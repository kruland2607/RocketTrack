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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
	private String rocketDistance = "";
	private double maxAltitude;

	private boolean followMe = false;

	public MapFragment() {
		super();
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
		Log.d(TAG,"+++ onResume +++");
		super.onResume();
		setUpMapIfNeeded();
		// Force old markers to disappear
		mMap.clear();
		// Redraw rocket location.
		onRocketLocationChange();
	}

	@Override
	public void onPause() {
		super.onPause();
		// Clear out the map markers so they get recreated onResume.
		rocketMarker = null;
		rocketCircle = null;
		rocketLine = null;
		rocketPath = null;
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
	public void onMyLocationChange() {
		Log.d(TAG,"onLocationChagned");

		Location location = getMyLocation();
		if(mMap == null)
			return;

		LatLng myPosition = new LatLng(location.getLatitude(),location.getLongitude());

		Location rocketLocation = getRocketLocation();
		if( rocketLocation != null ){
			LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());
			updateRocketLine(location,rocketPosition);
		}

		if ( followMe ) {
			float heading = getAzimuth();
			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).target(myPosition).bearing(heading).zoom(20).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}

	}


	@Override
	protected void onCompassChange() {
		//Log.d(TAG,"onCompassChange");
		updateBearing();
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
		mUiSettings.setCompassEnabled(false);
		mUiSettings.setMyLocationButtonEnabled(false);
		mUiSettings.setZoomControlsEnabled(false);

	}

	private void  updateBearing() {
		if ( mMap == null ) {
			return;
		}
		float heading = getAzimuth();
		if ( followMe ) {
			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition()).bearing(heading).zoom(20).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));
		}

		Integer rocketBearing = getBearing();
		if ( rocketBearing != null ) {
			TextView lblBearing = (TextView) getView().findViewById(R.id.TextView01);
			lblBearing.setText("Bearing: " + rocketBearing );
		}
	}


	@Override
	protected void onRocketLocationChange() {
		if (mMap == null ) {
			return;
		}

		Location rocketLocation = getRocketLocation();

		if(rocketLocation == null)
			return;

		LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());

		//Draw marker at Rocket position
		if(rocketMarker == null) {
			BitmapDescriptor rocketIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_rocket_map);
			rocketMarker = mMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(rocketPosition).icon(rocketIcon));
		} else {
			rocketMarker.setPosition(rocketPosition);
		}
		if ( rocketCircle == null ) {
			CircleOptions options = new CircleOptions()
			.center( rocketPosition )
			.radius( rocketLocation.getAccuracy() )
			.strokeColor(Color.RED)
			.strokeWidth(1.0f)
			.fillColor(Color.RED & 0x22FFFFFF);
			rocketCircle = mMap.addCircle(options);
		} else {
			rocketCircle.setCenter(rocketPosition);
			rocketCircle.setRadius( rocketLocation.getAccuracy() );
		}

		Location myLoc = getMyLocation();
		//myLoc = null when the android gps is not initialized yet.
		if(myLoc == null)
		{
			rocketDistance = "";
			return;
		}

		//update camera
		updateBearing();

		//Rocket Distance
		rocketDistance = this.getDistanceTo();
		TextView lblDistance = (TextView) getView().findViewById(R.id.lblDistance);
		lblDistance.setText("Rocket Distance: " + rocketDistance );

		//Max Altitude
		double altitude = rocketLocation.getAltitude();// - myLoc.getAltitude();
		if(altitude > maxAltitude)
			maxAltitude = altitude;
		TextView lblMaxAltitude = (TextView) getView().findViewById(R.id.lblMaxAltitude);
		lblMaxAltitude.setText("Max Altitude: " + maxAltitude + "m");

		//Draw line between myPosition and Rocket
		LatLng myPosition = new LatLng(myLoc.getLatitude(),myLoc.getLongitude());

		List<Location> rocketLocationHistory = getRocketLocationHistory();
		if ( rocketLocationHistory != null && rocketLocationHistory.size() > 0 ) {
			List<LatLng> rocketPosList = new ArrayList<LatLng>(rocketLocationHistory.size());
			for( Location l : rocketLocationHistory ) {
				rocketPosList.add( new LatLng(l.getLatitude(), l.getLongitude()));
			}
			if ( rocketPath == null ) {
				rocketPath = mMap.addPolyline( new PolylineOptions().width(1.0f).color(Color.rgb(0,0,128)));
			}
			rocketPath.setPoints(rocketPosList);
		}

		updateRocketLine(rocketLocation,rocketPosition);
	}

	private void updateRocketLine(Location rocketLocation,LatLng rocketPosition) {
		if(getMyLocation() == null || rocketPosition == null)
			return;
		Location myLocation = getMyLocation();
		LatLng myPosition = new LatLng(myLocation.getLatitude(),myLocation.getLongitude());

		//Rocket Distance
		rocketDistance = this.getDistanceTo();
		TextView lblDistance = (TextView) getView().findViewById(R.id.lblDistance);
		lblDistance.setText("Rocket Distance: " + rocketDistance );

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