package org.rockettrack;

import java.util.Currency;
import java.util.List;

import org.rockettrack.util.ExponentialAverage;
import org.rockettrack.util.Unit;
import org.rockettrack.util.UnitConverter;
import org.rockettrack.views.CoordinateHelper;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public abstract class RocketTrackBaseFragment
extends Fragment
implements SensorEventListener, LocationListener, GpsStatus.Listener {

	private final static String TAG = "RocketTrackBaseFragment";

	// Preferences names and default values
	private static final String PREFS_KEY_UNIT_DISTANCE = "distUnitPref";
	private static final String PREFS_KEY_UNIT_ALTITUDE = "altUnitPref";
	private static final String PREFS_KEY_KEEP_SCREEN_ON = "keepScreenOn";

	// Some default values
	private static final String PREFS_DEFAULT_UNIT_DISTANCE = Unit.meter.toString();
	private static final String PREFS_DEFAULT_UNIT_ALTITUDE = Unit.meter.toString();
	private static final boolean PREFS_DEFAULT_KEEP_SCREEN_ON = false;

	protected Unit unitDistance = Unit.meter;
	protected Unit unitAltitude = Unit.meter;
	
	private TextView lblDistance;
	private TextView alt;
	private TextView lblMaxAltitude;
	private TextView lblBearing;
	private TextView lat;
	private TextView lon;

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
	
	protected String getDistanceTo() {
		if ( myLocation == null || rocketLocation == null ) {
			return "";
		}
		float distanceMeters = myLocation.distanceTo(rocketLocation);
		String distanceString = UnitConverter.convertWithUnit(Unit.meter, unitDistance, distanceMeters, "#");
		return distanceString;
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

	protected Integer getBearing() {
		if ( rocketLocation == null || myLocation == null ) {
			return null;
		}
		float rocketBearing = normalizeDegree(myLocation.bearingTo(getRocketLocation()));
		return Math.round(rocketBearing);
	}

	private float normalizeDegree(float value){
		if(value >= 0.0f && value <= 180.0f){
			return value;
		}else{
			return 180 + (180 + value);
		}	
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		lblDistance = (TextView) getView().findViewById(R.id.Distance);
		alt = (TextView) getView().findViewById(R.id.Altitude);
		lblMaxAltitude = (TextView) getView().findViewById(R.id.MaxAlt);
		lblBearing = (TextView) getView().findViewById(R.id.Bearing);
		lat = (TextView) getView().findViewById(R.id.Latitude);
		lon = (TextView) getView().findViewById(R.id.Longitude);
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
				//Log.d(TAG,"DataSetObserver.onChanged()");
				updateRocketLocation();
			}

			@Override
			public void onInvalidated() {
				//Log.d(TAG,"DataSetObserver.onInvalidated()");
				updateRocketLocation();
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

		getPreferences();
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

		if ( event.timestamp > lastCompassTs + 250000000 /* .25s in nanoseconds */) {
			lastCompassTs = event.timestamp;
			
			float[] unmappedRotationMatrix = new float[9];

			SensorManager.getRotationMatrix(unmappedRotationMatrix, null, accel, magnet);
			
			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			int currentOrientation = wm.getDefaultDisplay().getRotation();
			//Log.d(TAG,"current orientation " + currentOrientation);
			
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

			updateBearingAndDistance();
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
		updateBearingAndDistance();
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

	/**
	 * 
	 */
	protected void getPreferences()
	{
		// Load preferences
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		final String strUnitDistance = sharedPreferences.getString(PREFS_KEY_UNIT_DISTANCE, PREFS_DEFAULT_UNIT_DISTANCE);
		unitDistance = Unit.getUnitForString(strUnitDistance);

		final String strUnitAltitude = sharedPreferences.getString(PREFS_KEY_UNIT_ALTITUDE, PREFS_DEFAULT_UNIT_ALTITUDE);
		unitAltitude = Unit.getUnitForString(strUnitAltitude);

		// Set keep screen on property
		final boolean blnKeepScreenOn = sharedPreferences.getBoolean(PREFS_KEY_KEEP_SCREEN_ON, PREFS_DEFAULT_KEEP_SCREEN_ON);
		this.getView().setKeepScreenOn(blnKeepScreenOn);

	}

	private void updateBearingAndDistance() {
		if ( myLocation == null || rocketLocation == null ) {
			lblBearing.setText("");
			lblDistance.setText("");
			return;
		}
		
		Integer rocketBearing = getBearing();
		if ( rocketBearing != null ) {
			lblBearing.setText(String.valueOf(rocketBearing));
		}

		//Rocket Distance
		String rocketDistance = this.getDistanceTo();
		//TextView lblDistance = (TextView) getView().findViewById(R.id.Distance);
		lblDistance.setText(rocketDistance );

	}
	
	private void updateRocketLocation() {
		rocketLocation = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
		updateBearingAndDistance();

		// Lat & Lon
		{
			final CoordinateHelper coordinateHelper = new CoordinateHelper(rocketLocation.getLatitude(), rocketLocation.getLongitude());
			lat.setText(coordinateHelper.getLatitudeString());
			lon.setText(coordinateHelper.getLongitudeString());
		}

		//Max Altitude
		{
			double altitude = rocketLocation.getAltitude();

			String altString = UnitConverter.convertWithUnit(Unit.meter, unitAltitude, altitude, "#");
			alt.setText(altString);

			double maxAltitude = RocketTrackState.getInstance().getLocationDataAdapter().getMaxAltitude();
			String maxAltString = UnitConverter.convertWithUnit(Unit.meter, unitAltitude, maxAltitude, "#");
			lblMaxAltitude.setText(maxAltString);
		}
		
		RocketTrackBaseFragment.this.onRocketLocationChange();


	}

}
