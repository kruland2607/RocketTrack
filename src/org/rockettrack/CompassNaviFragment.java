package org.rockettrack;

import org.rockettrack.data.GravityMovingAverage;
import org.rockettrack.util.Unit;
import org.rockettrack.views.CompassNaviView;
import org.rockettrack.views.NavigationTarget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CompassNaviFragment extends Fragment implements SensorEventListener, LocationListener, GpsStatus.Listener
{
	private final static String TAG = "CompassNaviFragment";

	private DataSetObserver mObserver = null;

	private SensorManager mSensorManager;
	private Sensor mSensorMagneticField;
	private LocationManager mLocationManager;
	private CompassNaviView mCompassNaviView;
	private SharedPreferences mSharedPreferences;
	private NavigationTarget mNavigationTarget = new NavigationTarget();
	private GravityMovingAverage mGravityMovingAverage;

	// private static final String EXCEPTION_URL = "http://flux.dnsdojo.org/opengpx/trace/";

	// Preferences names and default values
	private static final String PREFS_KEY_UNIT_DISTANCE = "distUnitPref";
	private static final String PREFS_KEY_UNIT_ALTITUDE = "altUnitPref";
	private static final String PREFS_KEY_KEEP_SCREEN_ON = "keepScreenOn";
	private static final String PREFS_KEY_USE_WEIGHTED_AVG = "useWeightedAverage";

	// Some default values
	private static final String PREFS_DEFAULT_UNIT_DISTANCE = Unit.meter.toString();
	private static final String PREFS_DEFAULT_UNIT_ALTITUDE = Unit.meter.toString();
	private static final boolean PREFS_DEFAULT_KEEP_SCREEN_ON = false;
	private static final boolean PREFS_DEFAULT_USE_WEIGHTED_AVG = false;

	// Sensor values
	private int mPreviousState = -1;	

	// Compass values
	float[] inR = new float[16];
	float[] I = new float[16];
	float[] gravity;
	float[] geomag = new float[3];
	float[] orientVals = new float[3];

	double azimuth = 0;

	static final float ALPHA = 0.2f;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		// Create new instance for compass smoothing
		this.mGravityMovingAverage = new GravityMovingAverage();



	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
		this.mSensorMagneticField = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		this.mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.navicompass, null, false);
		this.mCompassNaviView = (CompassNaviView) v.findViewById(R.id.CompassNaviView);
		return v;
	}

	/**
	 * 
	 */
	@Override
	public void onResume()
	{
		super.onResume();

		// Read GPS minimum time preferences
		final int intMinTime = 10;
		// Read GPS minimum distance preference
		final float fltMinDistance = 3;

		// Orientation (compass)
		this.mGravityMovingAverage.clear();
		final int intWindowSize = 5;
		this.mGravityMovingAverage.setWindowSize(intWindowSize);
		this.mGravityMovingAverage.setUseWeightedAverage(this.mSharedPreferences.getBoolean(PREFS_KEY_USE_WEIGHTED_AVG, PREFS_DEFAULT_USE_WEIGHTED_AVG));

		this.mSensorManager.registerListener(this, this.mSensorMagneticField, SensorManager.SENSOR_DELAY_UI);

		// Location (GPS)
		this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, intMinTime, fltMinDistance, this);

		// Add GpsStatusListener
		this.mLocationManager.addGpsStatusListener(this);

		// Set keep screen on property
		final boolean blnKeepScreenOn = this.mSharedPreferences.getBoolean(PREFS_KEY_KEEP_SCREEN_ON, PREFS_DEFAULT_KEEP_SCREEN_ON);
		this.mCompassNaviView.setKeepScreenOn(blnKeepScreenOn);

		mObserver = new DataSetObserver() {

			@Override
			public void onChanged() {
				Location l = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
				CompassNaviFragment.this.setNavigationTarget("Rocket", l);
			}

			@Override
			public void onInvalidated() {
				Location l = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
				CompassNaviFragment.this.setNavigationTarget("Rocket", l);
			}

		};

		RocketTrackState.getInstance().getLocationDataAdapter().registerDataSetObserver( mObserver );

	}


	/**
	 * 
	 */
	@Override
	public void onStart()
	{
		super.onStart();

		this.getPreferences();
	}

	/**
	 * 
	 */
	@Override
	public void onPause()
	{
		super.onPause();

		this.mSensorManager.unregisterListener(this, this.mSensorMagneticField);
		this.mLocationManager.removeGpsStatusListener(this);
		this.mLocationManager.removeUpdates(this);

		this.mCompassNaviView.setKeepScreenOn(false);

		if ( mObserver != null ) {
			RocketTrackState.getInstance().getLocationDataAdapter().unregisterDataSetObserver( mObserver );
		}
		mObserver = null;

	}

	/**
	 * 
	 */
	private void getPreferences()
	{
		// Load preferences
		this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		final String strUnitDistance = this.mSharedPreferences.getString(PREFS_KEY_UNIT_DISTANCE, PREFS_DEFAULT_UNIT_DISTANCE);
		final Unit unitDistance = this.getUnit(strUnitDistance);
		this.mCompassNaviView.setUnitForDistance(unitDistance);

		final String strUnitAltitude = this.mSharedPreferences.getString(PREFS_KEY_UNIT_ALTITUDE, PREFS_DEFAULT_UNIT_ALTITUDE);
		final Unit unitAltitude = this.getUnit(strUnitAltitude);
		this.mCompassNaviView.setUnitForAltitude(unitAltitude);

		// Log.d("Bearing provider", bearingProvider.toString());
		// Log.d("Unit (distance)", unitDistance.toString());
		// Log.d("Unit (altitude)", unitAltitude.toString());
	}

	/**
	 * 
	 * @param unitSystem
	 * @return
	 */
	private Unit getUnit(final String unitSystem)
	{
		Unit unit;
		if (unitSystem.equalsIgnoreCase("metric"))
			unit = Unit.meter;
		else
			unit = Unit.feet;
		return unit;
	}

	/**
	 * 
	 * @param name
	 * @param latitude
	 * @param longitude
	 */
	private void setNavigationTarget(String name, Location l)
	{
		if ( l == null ) {
			return;
		}
		this.mNavigationTarget.setName(name);
		this.mNavigationTarget.setLatitude(l.getLatitude());
		this.mNavigationTarget.setLongitude(l.getLongitude());

		this.mCompassNaviView.setTarget(this.mNavigationTarget);
	}

	@Override
	public void onGpsStatusChanged(int event) 
	{
		switch (event) 
		{
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			/*
	            if (this.mCurrentLocation != null)
	                isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
			 */
			if (this.mLocationManager != null)
			{
				final GpsStatus gpsStatus = this.mLocationManager.getGpsStatus(null);
				Integer numberOfSatellites = 0; 
				for (final GpsSatellite gpsSatellite : gpsStatus.getSatellites())
				{
					if (gpsSatellite.usedInFix())	
						numberOfSatellites++;
				}
				this.mCompassNaviView.setSatelliteCount(numberOfSatellites);
				this.mCompassNaviView.invalidate();
			}

			/*
	            if (isGPSFix) 
	            { 
	            	// A fix has been acquired.
	            }
	            else
	            { 
	            	// The fix has been lost.
	            }
			 */
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			// Do something.
			// isGPSFix = true;

			break;
		}
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		if (location != null)
		{
			this.mCompassNaviView.setLocation(location);
			// this.mLastLocationMillis = SystemClock.elapsedRealtime();
			this.mCompassNaviView.invalidate();
		}
	}

	@Override
	public void onProviderDisabled(String provider) 
	{
		// TODO Auto-generated method stub	
	}

	@Override
	public void onProviderEnabled(String provider) 
	{
		// TODO Auto-generated method stub	
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
	 * @see http://developer.android.com/reference/android/hardware/Sensor.html#TYPE_ACCELEROMETER
	 */
	protected float[] lowPass( float[] input, float[] output ) {
		if ( output == null ) return input;

		for ( int i=0; i<input.length; i++ ) {
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
		}
		return output;
	}

	/**
	 * 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		// If the sensor data is unreliable return
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
			return;

		// Gets the value of the sensor that has been changed
		switch (event.sensor.getType()) {  
		case Sensor.TYPE_ACCELEROMETER:
			// gravity = event.values.clone();
			// gravity = lowPass(event.values, gravity);
			gravity = this.mGravityMovingAverage.add(event.values);
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			geomag = event.values.clone();
			break;
		}

		Log.d(TAG, "Gravity = " + String.valueOf(gravity));
		// If gravity and geomag have values then find rotation matrix
		if (gravity != null && geomag != null) 
		{
			// checks that the rotation matrix is found
			final boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
			if (success) {
				// SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
				SensorManager.getOrientation(inR, orientVals);
				azimuth = Math.toDegrees(orientVals[0]);
				azimuth = (azimuth + 360) % 360;
				
				Log.d(TAG, "Azimuth = " + azimuth);
				
				// pitch = Math.toDegrees(orientVals[1]);
				// roll = Math.toDegrees(orientVals[2]);

				this.mCompassNaviView.setAzimuth((float)azimuth);
				this.mCompassNaviView.invalidate();
			}
		}		
	}

}
