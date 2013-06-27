package org.rockettrack;

import org.rockettrack.service.AppService;
import org.rockettrack.service.AppServiceConnection;
import org.rockettrack.service.BroadcastIntents;
import org.rockettrack.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 * 
 */
public class Main extends FragmentActivity {
	private final static String TAG = "RocketTrack.Main";

	private String PREFERED_DEVICE_KEY;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;

	/**
	 * Service connection object
	 */
	private AppServiceConnection serviceConnection;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private ViewPager viewFlow;

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = false;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "+++ ON CREATE +++");

		PREFERED_DEVICE_KEY = getResources().getString(R.string.prefered_device_key);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		setContentView(R.layout.activity_main);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
		.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(
								android.R.integer.config_shortAnimTime);
					}
					controlsView
					.animate()
					.translationY(visible ? 0 : mControlsHeight)
					.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE
							: View.GONE);
				}

				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		viewFlow = (ViewPager) findViewById(R.id.viewflow);
		viewFlow.setOnTouchListener(mDelayHideTouchListener);
		viewFlow.setAdapter(new PageAdapter(getSupportFragmentManager()));

	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		if ( AUTO_HIDE ) {
			delayedHide(100);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.e(TAG, "++ ON RESUME ++");
		Sounds.init(this);
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(BroadcastIntents.BLUETOOTH_STATE_CHANGE));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.e(TAG, "++ ON START ++");
		serviceConnection = new AppServiceConnection(this, appServiceConnectionCallback);
		serviceConnection.startService();
		serviceConnection.bindAppService();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.e(TAG, "-- ON STOP --");

		serviceConnection.unbindAppService();

	}

	@Override
	public void onPause() {
		super.onPause();
		Log.e(TAG, "++ ON START ++");
		Sounds.release();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
	}

	protected void connectOrSelectDevice() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String prefered_device = prefs.getString(PREFERED_DEVICE_KEY, "");
		if ( ! "".equals(prefered_device) ) {
			// There was a mac address stored in preferences.  See if it's still paired.
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(prefered_device);
			if ( device != null ) {
				// It's paired.  connect to it.
				connectDevice(prefered_device);
				return;
			}
		}

		// The previous device either didn't exist, or wasn't valid...

		// forget the old value used.
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(PREFERED_DEVICE_KEY,"");
		prefEditor.commit();

		// Ask the user to choose a new one.
		selectDevice();

	}

	private void selectDevice() {
		Intent serverIntent = new Intent(Main.this, DeviceListActivity.class);
		serverIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			selectDevice();
			return true;
		case R.id.save_recording:
			return true;
		}
		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect to
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				connectDevice(address);
			}
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			break;
		}
	}

	public void onDoStop() {
		serviceConnection.unbindAppService();
		serviceConnection.stopService();

	}

	private void connectDevice(String macAddress) {
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
		Log.d(TAG, "Connecting to " + device.getName());
		serviceConnection.getService().connectToRocketTracker(device);
	}

	public class PageAdapter extends FragmentPagerAdapter {

		private final Fragment[] frags = new Fragment[3];

		public PageAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return frags.length;
		}

		@Override
		public Fragment getItem(int position) {
			Fragment f = frags[position];
			if ( f == null ) {
				switch (position) {
				case 0:
					f = Fragment.instantiate(Main.this, "org.rockettrack.ConsoleOutputFragment");
					break;
				case 1:
					f = Fragment.instantiate(Main.this, "org.rockettrack.CurrentInfoFragment");
					break;
				case 2:
					f = Fragment.instantiate(Main.this,  "org.rockettrack.MapFragment");
					break;
				}
				frags[position] = f;
			}
			return f;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

	}

	/**
	 * 
	 */
	private Runnable appServiceConnectionCallback = new Runnable() {
		@Override
		public void run() {

			if (serviceConnection == null) {
				return;
			}

			AppService appService = serviceConnection.getService();

			if (appService == null) {
				Toast.makeText(Main.this, R.string.gps_not_enabled, Toast.LENGTH_SHORT).show();
				return;
			}

			appService.startSensorUpdates();

			Main.this.connectOrSelectDevice();

//			currentLocation = appService.getCurrentLocation();

		}
	};

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context ctx, Intent intent) {
			Log.d(TAG,"Got Message: " + intent.getAction());
			Bundle b = intent.getExtras();
			int state = b.getInt(BroadcastIntents.STATE);
			BluetoothDevice d = (BluetoothDevice) b.getParcelable(BroadcastIntents.DEVICE);
			if ( state == 3 ) {
				onDeviceConnected(d);
			}
		}
		
	};
	
	private void onDeviceConnected( BluetoothDevice device ) {
		Toast.makeText(this, "Connected to " + device.getName() , Toast.LENGTH_SHORT).show();
		// We connected to a device - so save its mac for next time.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(PREFERED_DEVICE_KEY, device.getAddress());
		prefEditor.commit();
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
