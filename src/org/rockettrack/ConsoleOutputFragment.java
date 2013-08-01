package org.rockettrack;

import org.rockettrack.views.ConsoleOutputView;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ConsoleOutputFragment extends RocketTrackBaseFragment {

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
		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		final ConsoleOutputView v = (ConsoleOutputView) getView().findViewById(R.id.console);
		mObserver = new DataSetObserver() {

			@Override
			public void onChanged() {
				v.invalidate();
			}

			@Override
			public void onInvalidated() {
				v.invalidate();
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
