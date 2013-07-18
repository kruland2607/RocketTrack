package org.rockettrack;

import java.util.List;

import org.rockettrack.util.ExponentialAverage;

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
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public abstract class RocketTrackBaseFragment extends Fragment implements SensorEventListener, LocationListener, GpsStatus.Listener{

	private final static String TAG = "RocketTrackBaseFragment";

	protected abstract void onRocketLocationChange();
	protected abstract void onCompassChange();
	protected abstract void onMyLocationChange();

	// My current location;
	private Location myLocation;

	// Current location of Rocket
	private Location rocketLocation;

	// Time we last sent a compass update to the derived class.
	// This is used to limit the number of updates
	private long lastCompassTs = 0;

	// magnatometer vector
	private float[] magnet = new float[3];
	
	// accelerometer vector
	private float[] accel = new float[3];

	// orientation angles from accel and magnet
	private float[] accMagOrientation = new float[3];
	
	// accelerometer and magnetometer based rotation matrix
	private float[] rotationMatrix = new float[9];
	
	// Filters for sensor values
	private ExponentialAverage accelAverage = new ExponentialAverage(3,.4f);
	private ExponentialAverage magnetAverage = new ExponentialAverage(3,.25f);

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

	protected List<Location> getRocketLocationHistory() {
		return RocketTrackState.getInstance().getLocationDataAdapter().getLocationHistory();
	}
	
	protected float getAzimuth() {
		if ( accMagOrientation == null ) {
			return 0f;
		}
		//Log.d(TAG, "getAzimuth " + accMagOrientation[0]);
		float azimuth = (float)Math.toDegrees(accMagOrientation[0]);
		azimuth = (azimuth +360) % 360;
		//Log.d(TAG, "getAzimuth " + azimuth);
		if (geoField != null) {
			azimuth += geoField.getDeclination();
		}
		//Log.d(TAG, "getAzimuth " + azimuth);
		return azimuth;
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
	public void onStart() {
		super.onStart();
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
				//Log.d(TAG,"DataSetObserver.onChanged()");
				RocketTrackBaseFragment.this.onRocketLocationChange();
			}

			@Override
			public void onInvalidated() {
				rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
				//Log.d(TAG,"DataSetObserver.onInvalidated()");
				RocketTrackBaseFragment.this.onRocketLocationChange();
			}
		};

		RocketTrackState.getInstance().getLocationDataAdapter().registerDataSetObserver(mObserver);
		rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();


		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

		sman.registerListener(this,
				sman.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
		sman.registerListener(this,
				sman.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);


	}

	@Override
	public void onPause() {
		super.onPause();
		RocketTrackState.getInstance().getLocationDataAdapter().unregisterDataSetObserver(mObserver);

		SensorManager sman = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

		sman.unregisterListener(this);

		mLocationManager.removeGpsStatusListener(this);
		mLocationManager.removeUpdates(this);

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accel = accelAverage.average(event.values);
			break;

		case Sensor.TYPE_MAGNETIC_FIELD:

			magnet = magnetAverage.average(event.values);
			break;

		}

		if ( event.timestamp > lastCompassTs + 100000000 /* .1s in nanoseconds */) {
			lastCompassTs = event.timestamp;
			
			float[] unmappedRotationMatrix = new float[9];

			SensorManager.getRotationMatrix(unmappedRotationMatrix, null, accel, magnet);
			
			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			int currentOrientation = wm.getDefaultDisplay().getRotation();
			Log.d(TAG,"current orientation " + currentOrientation);
			
			switch ( currentOrientation ) {
			case Surface.ROTATION_0:
				SensorManager.remapCoordinateSystem(unmappedRotationMatrix,SensorManager.AXIS_X,SensorManager.AXIS_Y,rotationMatrix);
				 break;
			case Surface.ROTATION_90:
				SensorManager.remapCoordinateSystem(unmappedRotationMatrix,SensorManager.AXIS_Y,SensorManager.AXIS_X,rotationMatrix);
				break;
			case Surface.ROTATION_180:
				SensorManager.remapCoordinateSystem(unmappedRotationMatrix,SensorManager.AXIS_X,SensorManager.AXIS_MINUS_Y,rotationMatrix);
				break;
			case Surface.ROTATION_270:
				SensorManager.remapCoordinateSystem(unmappedRotationMatrix,SensorManager.AXIS_MINUS_Y,SensorManager.AXIS_MINUS_X,rotationMatrix);
				break;
			}
			SensorManager.getOrientation(rotationMatrix, accMagOrientation);

			onCompassChange();
		}

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
	final public void onLocationChanged(Location location) 
	{
		myLocation = location;
		geoField = new GeomagneticField(
				Double.valueOf(location.getLatitude()).floatValue(),
				Double.valueOf(location.getLongitude()).floatValue(),
				Double.valueOf(location.getAltitude()).floatValue(),
				System.currentTimeMillis()
				);
		this.onMyLocationChange();
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
