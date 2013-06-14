package org.rockettrack;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import net.sf.marineapi.nmea.sentence.PositionSentence;
import android.location.Location;

public class RocketTrackState {

	private static RocketTrackState instance = new RocketTrackState();;
	
	public static RocketTrackState getInstance() {
		return instance;
	}
	
	PositionSentence rocketPosition;
	
	Location myLocation;

	private Queue<String> lines = new ArrayBlockingQueue<String>(100);

	public PositionSentence getRocketPosition() {
		return rocketPosition;
	}

	public void setRocketPosition(PositionSentence rocketPosition) {
		this.rocketPosition = rocketPosition;
	}

	public Location getMyLocation() {
		return myLocation;
	}

	public void setMyLocation(Location myLocation) {
		this.myLocation = myLocation;
	}
	
	public void addLine(String s ) {
		if ( lines.offer(s)  == true ) {
			return;
		}
		lines.poll();
		lines.add(s);
	}

	public void resizeLines( int newSize ) {
		lines = new ArrayBlockingQueue<String>(newSize);
	}
	
	public String[] getLines() {
		return lines.toArray(new String[0] );
	}
}
