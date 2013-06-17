package org.rockettrack.views;

import org.rockettrack.R;
import org.rockettrack.RocketTrackState;

import android.content.Context;
import android.database.DataSetObserver;
import android.location.Location;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CurrentInfoView extends LinearLayout {

	TextView rocketAlt;
	TextView rocketLon;
	TextView rocketLat;
	
	TextView myAlt;
	TextView myLon;
	TextView myLat;
	
	public CurrentInfoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		RocketTrackState.getInstance().getLocationDataAdapter().registerDataSetObserver( new DataSetObserver() {

			@Override
			public void onChanged() {
				CurrentInfoView.this.updateFields();
			}

			@Override
			public void onInvalidated() {
				CurrentInfoView.this.updateFields();
			}
			
		});
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		
		inflater.inflate(R.layout.current_view_internal, this, true );
		
		rocketAlt = (TextView) findViewById(R.id.rocket_alt);
		rocketLon = (TextView) findViewById(R.id.rocket_lon);
		rocketLat = (TextView) findViewById(R.id.rocket_lat);

		myAlt = (TextView) findViewById(R.id.my_alt);
		myLon = (TextView) findViewById(R.id.my_lon);
		myLat = (TextView) findViewById(R.id.my_lat);
		
		updateFields();
	}

	public CurrentInfoView(Context context, AttributeSet attrs) {
		this(context,attrs,0);
	}

	public CurrentInfoView(Context context) {
		this(context,null);
	}

	
	private void updateFields() {
		
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
	}
	
}
