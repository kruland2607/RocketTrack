package org.rockettrack;

import org.rockettrack.util.Unit;
import org.rockettrack.views.CompassNaviView;
import org.rockettrack.views.NavigationTarget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CompassNaviFragment extends RocketTrackBaseFragment {
	private final static String TAG = "CompassNaviFragment";

	private CompassNaviView mCompassNaviView;
	private NavigationTarget mNavigationTarget = new NavigationTarget();


	// Compass values
	float[] inR = new float[16];
	float[] I = new float[16];
	float[] gravity;
	float[] orientVals = new float[3];

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.navicompass, null, false);
		this.mCompassNaviView = (CompassNaviView) v.findViewById(R.id.CompassNaviView);
		return v;
	}

	@Override
	protected void onCompassChange() {
		mCompassNaviView.setAzimuth(getAzimuth());
	}
	
	@Override
	protected void onRocketLocationChange() {
		Location l = getRocketLocation();
		setNavigationTarget("Rocket", l);
	}

	/**
	 * 
	 */
	@Override
	public void onResume()
	{
		super.onResume();

		this.mCompassNaviView.setUnitForDistance(unitDistance);
		this.mCompassNaviView.setUnitForAltitude(unitDistance);

	}


	/**
	 * 
	 */
	@Override
	public void onPause()
	{
		super.onPause();

		this.mCompassNaviView.setKeepScreenOn(false);

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
	protected void onMyLocationChange() 
	{
		Location location = getMyLocation();
		if (location != null)
		{
			this.mCompassNaviView.setLocation(location);
			// this.mLastLocationMillis = SystemClock.elapsedRealtime();
			this.mCompassNaviView.invalidate();
		}
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

}
