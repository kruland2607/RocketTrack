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
	
	public RawDataAdapter getRawDataAdapter(){
		return rawData;
	}
	
	public LocationDataAdapter getLocationDataAdapter() {
		return locData;
	}
}
