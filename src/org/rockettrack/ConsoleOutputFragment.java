package org.rockettrack;

import org.rockettrack.views.ConsoleOutputView;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class ConsoleOutputFragment extends RocketTrackBaseFragment {

	private boolean paused = false;
	
	private DataSetObserver mObserver = null;
	
	@Override
	protected void onRocketLocationChange() {
	}

	@Override
	protected void onCompassChange() {
	}

	@Override
	protected void onMyLocationChange() {
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.console_view, null, false);
		
		ToggleButton chkPause = (ToggleButton) root.findViewById(R.id.chkPause);
		paused = chkPause.isChecked();
		chkPause.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				paused = arg1;
			}

		});

		
		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		final ConsoleOutputView v = (ConsoleOutputView) getView().findViewById(R.id.console);
		mObserver = new DataSetObserver() {

			@Override
			public void onChanged() {
				if ( ! paused ) {
					v.invalidate();
				}
			}

			@Override
			public void onInvalidated() {
				if ( ! paused ) {
					v.invalidate();
				}
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

}
