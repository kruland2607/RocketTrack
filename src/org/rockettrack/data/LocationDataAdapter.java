package org.rockettrack.data;

import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class LocationDataAdapter extends BaseAdapter {

	long lastTime = 0;
	Location rocketPosition;
	
	private final float minDistance = 20f; // meters
	private final int minTime = 1000; // millis
	
	public Location getRocketPosition() {
		return rocketPosition;
	}

	public void setRocketLocation(Location rocketPosition) {
		long currentTime = System.currentTimeMillis();
		if ( lastTime == 0 || currentTime - lastTime > minTime ) {
			this.lastTime = currentTime;
			this.rocketPosition = rocketPosition;
			this.notifyDataSetChanged();
		} else {
			float distance = this.rocketPosition.distanceTo(rocketPosition);
			if ( distance >= minDistance) {
				this.lastTime = currentTime;
				this.rocketPosition = rocketPosition;
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
