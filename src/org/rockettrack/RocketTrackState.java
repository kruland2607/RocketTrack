package org.rockettrack;

import org.rockettrack.data.LocationDataAdapter;
import org.rockettrack.data.RawDataAdapter;

public class RocketTrackState {

	private static RocketTrackState instance = new RocketTrackState();;
	
	public static RocketTrackState getInstance() {
		return instance;
	}
	
	private LocationDataAdapter locData = new LocationDataAdapter();
	
	private RawDataAdapter rawData = new RawDataAdapter();
	
	private double azimuth = 0.0;
	private double declination = 0.0;
	
	public RawDataAdapter getRawDataAdapter(){
		return rawData;
	}
	
	public LocationDataAdapter getLocationDataAdapter() {
		return locData;
	}

	public double getAzimuth() {
		return azimuth;
	}

	public void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
	}

	public double getDeclination() {
		return declination;
	}

	public void setDeclination(double declination) {
		this.declination = declination;
	}
	
}
