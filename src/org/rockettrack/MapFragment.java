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

import android.database.DataSetObserver;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapFragment extends Fragment {

	private DataSetObserver mObserver = null;

	private GoogleMap mMap;
	private Marker rocketMarker;
	private float rocketDistance = 0;
	private Location rocketLocation;
	private double maxAltitude;
	
	List<LatLng> rocketPosList;
	
	private Polyline rocketLine;
	private Polyline rocketPath;
	
	
	public MapFragment() {
		super();
		rocketPosList = new ArrayList<LatLng>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.map_view, null, false);
		
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		mObserver = new DataSetObserver() {
			@Override
			public void onChanged() {
				MapFragment.this.refreshScreen();
			}

			@Override
			public void onInvalidated() {
				MapFragment.this.refreshScreen();
			}
		};
		
		RocketTrackState.getInstance().getLocationDataAdapter().registerDataSetObserver(mObserver);
		rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
		
		setUpMapIfNeeded();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		RocketTrackState.getInstance().getLocationDataAdapter().unregisterDataSetObserver(mObserver);
	}

	@Override
    public void onDestroyView() {
        super.onDestroyView(); 
        // Stackoverflow says this is necessary, but it seems to cause some troubles too.
//        Fragment fragment = (getFragmentManager().findFragmentById(R.id.map));  
//        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
//        ft.remove(fragment);
//        ft.commit();
}
	private void refreshScreen() {
		rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
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
		if(rocketLocation != null)
			updateRocketLocation();		
		/*
		 * mMap.moveCamera( CameraUpdateFactory.newCameraPosition(new
		 * CameraPosition());//.newLatLngZoom(new
		 * LatLng(myLoc.getLatitude(),myLoc.getLongitude()), 10));
		 */
		UiSettings mUiSettings = mMap.getUiSettings();
		mUiSettings.setCompassEnabled(true);
		// mUiSettings.
		
	}

	private void updateRocketLocation() {
		if(rocketLocation == null)
			return;
		
		LatLng rocketPosition = new LatLng(rocketLocation.getLatitude(), rocketLocation.getLongitude());
		rocketPosList.add(rocketPosition);
		
		//Draw marker at Rocket position
		if(rocketMarker == null)
			rocketMarker = mMap.addMarker(new MarkerOptions().position(rocketPosition));
		else
			rocketMarker.setPosition(rocketPosition);
		

		Location myLoc = mMap.getMyLocation();
		//myLoc = null when the android gps is not initialized yet.
		if(myLoc == null)
		{
			rocketDistance = 0;
			return;
		}

		//update camera
		float bearing = myLoc.bearingTo(rocketLocation);
		CameraPosition camPos = new CameraPosition.Builder()
				.target(rocketPosition).bearing(bearing).zoom(20).build();
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));


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
		LatLng myPosition = new LatLng(myLoc.getLatitude(),
				myLoc.getLongitude());

		
		if (rocketPath == null) {
			rocketPath = mMap.addPolyline(new PolylineOptions()
					.add(rocketPosList.get(0))					
					.width(1.0f)
					.color(Color.rgb(0, 0, 128)));
		} 
		rocketPath.setPoints(rocketPosList);
		
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