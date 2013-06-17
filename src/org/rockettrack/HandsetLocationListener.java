package org.rockettrack;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class HandsetLocationListener implements LocationListener {

	@Override
	public void onLocationChanged(Location location) {
		RocketTrackState.getInstance().getLocationDataAdapter().setMyLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
