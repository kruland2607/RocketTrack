package org.rockettrack.data;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.sentence.PositionSentence;
import net.sf.marineapi.nmea.util.Position;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


public class LocationDataAdapter extends BaseAdapter {

	Position rocketPosition;
	
	Location myLocation;

	public Position getRocketPosition() {
		return rocketPosition;
	}

	public void setRocketPosition(PositionSentence rocketPosition) {
		try {
			this.rocketPosition = rocketPosition.getPosition();
			this.notifyDataSetChanged();
		} catch ( DataNotAvailableException ex ) {
			
		}
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
