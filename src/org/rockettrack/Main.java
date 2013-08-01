package org.rockettrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import org.rockettrack.service.AppService;
import org.rockettrack.service.AppServiceConnection;
import org.rockettrack.service.BroadcastIntents;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

	
    private FragmentTabHost mTabHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "+++ ON CREATE +++");

		PREFERED_DEVICE_KEY = getResources().getString(R.string.prefered_device_key);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		setContentView(R.layout.activity_main);

        mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        mTabHost.addTab(mTabHost.newTabSpec("console").setIndicator("Console"),
                ConsoleOutputFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("map").setIndicator("Map"),
                MapFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("compass").setIndicator("Compass"),
                CompassNaviFragment.class, null);

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
			saveRawRecording();
			return true;
		case R.id.menu_preferences:
			Intent i = new Intent(this,Preferences.class);
			startActivity(i);
			break;
		case R.id.stop_service:
			onDoStop();
			break;
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
			break;
		}
	}

	private void onDoStop() {
		serviceConnection.stopService();
	}

	private void connectDevice(String macAddress) {
		
		// forget the old value used.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(PREFERED_DEVICE_KEY,"");
		prefEditor.commit();

		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(macAddress);
		Log.d(TAG, "Connecting to " + device.getName());
		RocketTrackState.getInstance().clear();
		serviceConnection.startService();
		serviceConnection.getService().connectToRocketTracker(device);
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
			switch( state ) {
			case AppService.STATE_CONNECTED:
				RocketTrackState.getInstance().getRawDataAdapter().addRawData("Connected to rx");
				onDeviceConnected(d);
				break;
			case AppService.STATE_CONNECT_FAILED:
				RocketTrackState.getInstance().getRawDataAdapter().addRawData("Connection to rx lost");
				onDeviceReconnect(d);
				break;
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
	
	private void onDeviceReconnect( BluetoothDevice device ) {
		Toast.makeText(this, "Failed to connect to " + device.getName() + ".  Reconnecting", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "Reconnect to " + device.getName());
		serviceConnection.getService().connectToRocketTracker(device);
	}

	private void saveRawRecording() {
		List<String> lines = RocketTrackState.getInstance().getRawDataAdapter().getRawData();
		if( lines == null || lines.size() == 0 ) {
			return;
		}
		// Fixme - move to separate thread.
		try {
			Date now = new Date();
			// Build File name
			String fileName = ((App)getApplication()).getAppDir() + "/raw_gps-" + now.getTime() + ".txt";
			File myFile = new File(fileName);
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			for( String s: lines ) {
				myOutWriter.append(s).append("\n");
			}
			myOutWriter.close();
			fOut.close();
			Toast.makeText(getBaseContext(),
					"Done writing file",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(getBaseContext(), e.getMessage(),
					Toast.LENGTH_LONG).show();
		}		

	}
}
