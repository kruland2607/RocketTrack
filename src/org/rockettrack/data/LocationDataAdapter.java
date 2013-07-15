package org.rockettrack.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class LocationDataAdapter extends BaseAdapter {

	private long lastTime = 0;
	private Location currentRocketPosition;
	private final List<Location> locationHistory = new ArrayList<Location>(300);
	
	private final float minDistance = 20f; // meters
	private final int minTime = 1000; // millis
	
	public void clear() {
		lastTime = 0;
		currentRocketPosition = null;
		locationHistory.clear();
	}
	
	public Location getRocketPosition() {
		return currentRocketPosition;
	}
	
	public List<Location> getLocationHistory() {
		return Collections.unmodifiableList(locationHistory);
	}

	public void setRocketLocation(Location rocketPosition) {
		locationHistory.add(rocketPosition);
		long currentTime = System.currentTimeMillis();
		if ( lastTime == 0 || currentTime - lastTime > minTime ) {
			this.lastTime = currentTime;
			this.currentRocketPosition = rocketPosition;
			this.notifyDataSetChanged();
		} else {
			float distance = this.currentRocketPosition.distanceTo(rocketPosition);
			if ( distance >= minDistance) {
				this.lastTime = currentTime;
				this.currentRocketPosition = rocketPosition;
				this.notifyDataSetChanged();
			}
		}
	}

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return null;
	}

}
