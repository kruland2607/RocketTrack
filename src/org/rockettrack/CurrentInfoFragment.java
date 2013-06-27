package org.rockettrack;

import android.database.DataSetObserver;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CurrentInfoFragment extends Fragment {

	private DataSetObserver mObserver = null;

	TextView rocketAlt;
	TextView rocketLon;
	TextView rocketLat;
	
	TextView myAlt;
	TextView myLon;
	TextView myLat;
	
	TextView bearing;
	TextView distance;
	TextView azimuth;
	
	TextView declto;
	TextView decl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.current_view, null, false);
		rocketAlt = (TextView) root.findViewById(R.id.rocket_alt);
		rocketLon = (TextView) root.findViewById(R.id.rocket_lon);
		rocketLat = (TextView) root.findViewById(R.id.rocket_lat);

		myAlt = (TextView) root.findViewById(R.id.my_alt);
		myLon = (TextView) root.findViewById(R.id.my_lon);
		myLat = (TextView) root.findViewById(R.id.my_lat);
		
		bearing = (TextView) root.findViewById(R.id.bearing);
		distance = (TextView) root.findViewById(R.id.distance);
		
		azimuth = (TextView) root.findViewById(R.id.azimuth);
		declto = (TextView) root.findViewById(R.id.declto);
		decl = (TextView) root.findViewById(R.id.declination);

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		mObserver = new DataSetObserver() {

			@Override
			public void onChanged() {
				CurrentInfoFragment.this.refreshScreen();
			}

			@Override
			public void onInvalidated() {
				CurrentInfoFragment.this.refreshScreen();
			}
			
		};

		RocketTrackState.getInstance().getRawDataAdapter().registerDataSetObserver( mObserver );
	}

	@Override
	public void onPause() {
		super.onPause();
		if ( mObserver != null ) {
			RocketTrackState.getInstance().getRawDataAdapter().unregisterDataSetObserver( mObserver );
		}
		 mObserver = null;
	}

	private void refreshScreen() {
		Location rocketPos = RocketTrackState.getInstance().getLocationDataAdapter().getRocketPosition();
		if( rocketPos != null ) {
			rocketAlt.setText( String.valueOf(rocketPos.getAltitude() ));
			rocketLon.setText( String.valueOf(rocketPos.getLongitude() ));
			rocketLat.setText( String.valueOf(rocketPos.getLatitude() ));
		}
		
		Location handsetLoc = RocketTrackState.getInstance().getLocationDataAdapter().getMyLocation();
		if ( handsetLoc != null ) {
			myAlt.setText( String.valueOf(handsetLoc.getAltitude()));
			myLon.setText( String.valueOf(handsetLoc.getLongitude()));
			myLat.setText( String.valueOf(handsetLoc.getLatitude()));
		}
		
		if ( rocketPos != null && handsetLoc != null ) {
			bearing.setText( String.valueOf(handsetLoc.bearingTo(rocketPos)));
			distance.setText( String.valueOf(handsetLoc.distanceTo(rocketPos)));
		}
		
		azimuth.setText( String.valueOf( RocketTrackState.getInstance().getAzimuth()));
		decl.setText( String.valueOf(RocketTrackState.getInstance().getDeclination()));
	}
	
}
