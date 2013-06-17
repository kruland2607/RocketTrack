package org.rockettrack.data;

import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class LocationDataAdapter extends BaseAdapter {

	Location rocketPosition;
	
	Location myLocation;

	public Location getRocketPosition() {
		return rocketPosition;
	}

	public void setRocketLocation(Location rocketPosition) {
		this.rocketPosition = rocketPosition;
		this.notifyDataSetChanged();
	}

	public Location getMyLocation() {
		return myLocation;
	}

	public void setMyLocation(Location myLocation) {
		this.myLocation = myLocation;
		this.notifyDataSetChanged();
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
