package org.rockettrack;

import android.content.Context;
import android.database.DataSetObserver;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

public abstract class RocketTrackBaseFragment extends Fragment implements SensorEventListener, LocationListener, GpsStatus.Listener{

	private final static String TAG = "RocketTrackBaseFragment";
	
	protected abstract void onDataChange();
	
	// My current location;
	private Location myLocation;
	
	// Current location of Rocket
	private Location rocketLocation;

	// Current handset azimuth
	private float azimuth;
	
	// Sensor values
	private int mPreviousState = -1;	

	protected LocationManager mLocationManager;
	
	private GeomagneticField geoField;

	// Listener for rocket location changes
	private DataSetObserver mObserver = null;
	
	protected Location getMyLocation() {
		return myLocation;
	}
	
	protected Location getRocketLocation() {
		return rocketLocation;
	}
	
	protected float getAzimuth() {
		if (geoField != null) {
			return azimuth + geoField.getDeclination();
		} else { 
			return azimuth;
		}
	}
	
	protected Float getBearing() {
		if ( rocketLocation == null || myLocation == null ) {
			return null;
		}
		float rocketBearing = normalizeDegree(myLocation.bearingTo(getRocketLocation()));
		return rocketBearing;
	}
	
	private float normalizeDegree(float value){
		if(value >= 0.0f && value <= 180.0f){
			return value;
		}else{
			return 180 + (180 + value);
		}	
	}

	@Override
	public void onResume() {
		super.onResume();

		mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

		// Read GPS minimum time preferences
		final int intMinTime = 1000;
		// Read GPS minimum distance preference
		final float fltMinDistance = 3;

		// Location (GPS)
		this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intMinTime, fltMinDistance, this);

		myLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		// Add GpsStatusListener
		this.mLocationManager.addGpsStatusListener(this);

		mObserver = new DataSetObserver() {
			@Override
			public void onChanged() {
				rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
				Log.d(TAG,"DataSetObserver.onChanged()");
				RocketTrackBaseFragment.this.onDataChange();
			}

			@Override
			public void onInvalidated() {
				rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
				Log.d(TAG,"DataSetObserver.onInvalidated()");
				RocketTrackBaseFragment.this.onDataChange();
			}
		};
		
		RocketTrackState.getInstance().getLocationDataAdapter().registerDataSetObserver(mObserver);
		rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();

		
		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		@SuppressWarnings("deprecation")
		Sensor magnetfield = sman.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		// Finally, register your listener
		sman.registerListener(this, magnetfield,SensorManager.SENSOR_DELAY_UI);

	}
	
	@Override
	public void onPause() {
		super.onPause();
		RocketTrackState.getInstance().getLocationDataAdapter().unregisterDataSetObserver(mObserver);

		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		@SuppressWarnings("deprecation")
		Sensor magnetfield = sman.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sman.unregisterListener(this, magnetfield);
		
		mLocationManager.removeGpsStatusListener(this);
		mLocationManager.removeUpdates(this);

	}

	@Override
	public void onSensorChanged(SensorEvent arg0) {
		azimuth = Math.round(arg0.values[0]);
		Log.d(TAG,"onSensorChanged()");
		RocketTrackBaseFragment.this.onDataChange();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onGpsStatusChanged(int event) 
	{
		// Default implemenation
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		myLocation = location;
		geoField = new GeomagneticField(
				Double.valueOf(location.getLatitude()).floatValue(),
				Double.valueOf(location.getLongitude()).floatValue(),
				Double.valueOf(location.getAltitude()).floatValue(),
				System.currentTimeMillis()
				);		
	}

	@Override
	public void onProviderDisabled(String provider) 
	{
	}

	@Override
	public void onProviderEnabled(String provider) 
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		if (status != this.mPreviousState)
		{
			String strNewStatus = String.format("GPS Status: ", provider);
			if (status == LocationProvider.AVAILABLE)
				strNewStatus += "Available";
			else if (status == LocationProvider.OUT_OF_SERVICE)
				strNewStatus += "Out of service";
			else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
				strNewStatus += "Temporarily unavailable";

			Toast.makeText(getActivity(), strNewStatus, Toast.LENGTH_SHORT).show();
			this.mPreviousState = status;
		}
	}


}
