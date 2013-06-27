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

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
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

public class MapFragment extends Fragment  implements OnMyLocationChangeListener {

	// Listeners
	private DataSetObserver mObserver = null;
	private SensorEventListener magnetlistener = null;
	
	// Map reference
	private GoogleMap mMap;
	
	// Map markers
	private Marker rocketMarker;
	private Circle rocketCircle;
	private Polyline rocketLine;
	private Polyline rocketPath;

	// Data
	private float rocketDistance = 0;
	private Location rocketLocation;
	private double maxAltitude;

	List<LatLng> rocketPosList;

	protected int myAzimuth;
	LatLng myPosition;
	private GeomagneticField geoField;
	public long radarDelay;


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

		initCompass();

		setUpMapIfNeeded();
		
		radarDelay = -1;
		radarBeepThread radarBeep = new radarBeepThread();
		radarBeep.start();

	}

	@Override
	public void onPause() {
		super.onPause();
		RocketTrackState.getInstance().getLocationDataAdapter().unregisterDataSetObserver(mObserver);
		
		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		@SuppressWarnings("deprecation")
		Sensor magnetfield = sman.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sman.unregisterListener(magnetlistener, magnetfield);

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
	public void onMyLocationChange(Location location) {
		if(mMap == null || mMap.getMyLocation() == null)
			return;
		
		geoField = new GeomagneticField(
		         Double.valueOf(location.getLatitude()).floatValue(),
		         Double.valueOf(location.getLongitude()).floatValue(),
		         Double.valueOf(location.getAltitude()).floatValue(),
		         System.currentTimeMillis()
		      );		
		myPosition = new LatLng(location.getLatitude(),
				location.getLongitude());

		if(rocketPosList.size() > 0){
			LatLng rocketPosition = rocketPosList.get(rocketPosList.size() -1);
			updateRocketLine(rocketPosition);
		}
		
		ToggleButton chkFollowMe = (ToggleButton) getView().findViewById(R.id.chkFollowMe);
		
		if(chkFollowMe.isChecked()){				
			CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition())
			.target(myPosition).build();
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));		
		}
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
		mMap.setOnMyLocationChangeListener(this);
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
	
	private void initCompass() {
		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		@SuppressWarnings("deprecation")
		Sensor magnetfield = sman.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		magnetlistener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent arg0) {
				ToggleButton chkRocketCompass = (ToggleButton) getView().findViewById(R.id.chkRocketCompass);
				
				if(!chkRocketCompass.isChecked())
					return;
				myAzimuth = Math.round(arg0.values[0]);
				
				float heading;
				if(rocketLocation == null){
					if(geoField == null)
						heading = myAzimuth;
					else
						heading = myAzimuth + geoField.getDeclination();
				}
				else{
					float rocketBearing = normalizeDegree(mMap.getMyLocation().bearingTo(rocketLocation));
					rocketBearing += geoField.getDeclination();
					float deltaBearing = (rocketBearing - myAzimuth) *-1;
					heading =  rocketBearing + deltaBearing;		

					float delta2 = deltaBearing + geoField.getDeclination();
					radarDelay = getRadarDelay(delta2);
					
					TextView lblBearing = (TextView) getView().findViewById(R.id.TextView01);
					lblBearing.setText("Bearing: " + getRadarDelay(delta2) / 10);
					
				}
				CameraPosition camPos = new CameraPosition.Builder(mMap.getCameraPosition())
				.bearing(heading).build();
				mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));

			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};

		// Finally, register your listener
		sman.registerListener(magnetlistener, magnetfield,SensorManager.SENSOR_DELAY_NORMAL);
	}

	private float normalizeDegree(float value){
        if(value >= 0.0f && value <= 180.0f){
            return value;
        }else{
            return 180 + (180 + value);
        }	
	}
	
	private long getRadarDelay(float value){
		long ret = (long) Math.abs(value);
		if(ret > 180)
			ret = (long) (360 - ret);
		return ret * 10 + 20;
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
		LatLng myPosition = new LatLng(myLoc.getLatitude(),myLoc.getLongitude());

		if (rocketPath == null) {
			rocketPath = mMap.addPolyline(new PolylineOptions()
			.add(rocketPosList.get(0))
			.width(1.0f)
			.color(Color.rgb(0, 0, 128)));
		} 
		rocketPath.setPoints(rocketPosList);

		updateRocketLine(rocketPosition);
	}

	private void updateRocketLine(LatLng rocketPosition) {
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

	private class radarBeepThread extends Thread{
		public void run() {
			while(true){
				try {
					if(radarDelay < 0)
						Thread.sleep(100);
					else
						Thread.sleep(radarDelay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(radarDelay > 0)
					Sounds.radar_beep.start();
			}
		}		
	}

}