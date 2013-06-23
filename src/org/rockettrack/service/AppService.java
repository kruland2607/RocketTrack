package org.rockettrack.service;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import org.rockettrack.Main;
import org.rockettrack.R;
import org.rockettrack.RocketTrackState;
import org.rockettrack.nmea.Parser;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * this service handles real time and scheduled track recording as well as
 * compass updates
 */
public class AppService extends Service {

	private final static String TAG = "AppService";

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private final static int NOTIFICATION = R.string.telemetry_service_label;

	private static boolean running = false;

	private LocationManager locationManager;

	private SensorManager sensorManager;

	/**
	 * current device location
	 */
	private Location currentLocation;

	/**
	 * is GPS in use?
	 */
	private boolean gpsInUse;

	/**
	 * listening for location updates flag  // FIXME  - change to recording..
	 */
	private boolean listening;

	// Name of the connected device
	private BluetoothDevice device           = null;
	private RocketTrackBluetooth  mAltosBluetooth  = null;
	private final Handler   mHandler   = new IncomingHandler(this);

	// internally track state of bluetooth connection
	private int state = STATE_NONE;

	static final int STATE_NONE       = 0;
	static final int STATE_READY      = 1;
	static final int STATE_CONNECTING = 2;
	static final int STATE_CONNECTED  = 3;

	static final int MSG_CONNECTED         = 4;
	static final int MSG_CONNECT_FAILED    = 5;
	static final int MSG_DISCONNECTED      = 6;
	static final int MSG_TELEMETRY         = 7;

	/**
	 * listening getter
	 */
	public boolean isListening() {
		return listening;
	}

	/**
	 * 
	 * @return
	 */
	public Location getCurrentLocation() {
		return this.currentLocation;
	}


	/**
	 * Defines a listener that responds to location updates
	 */
	private LocationListener locationListener = new LocationListener() {

		/**
		 * Called when a new location is found by the network location provider.
		 */
		@Override
		public void onLocationChanged(Location location) {

			listening = true;

			currentLocation = location;

			RocketTrackState.getInstance().getLocationDataAdapter().setMyLocation(location);

			// activities with registered receivers will get location updates
			broadcastLocationUpdate(location, BroadcastIntents.LOCATION_UPDATE);

		}

		/**
		 * Called when the provider status changes. This method is called when a
		 * provider is unable to fetch a location or if the provider has
		 * recently become available after a period of unavailability.
		 */
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
				listening = false;
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	};

	/**
	 * Broadcasting location update
	 */
	private void broadcastLocationUpdate(Location location, String action) {

		// let's broadcast location data to any activity waiting for updates
		Intent intent = new Intent(action);

		Bundle bundle = new Bundle();
		bundle.putParcelable("location", location);

		intent.putExtras(bundle);

		sendLocalBroadcast(intent);

	}

	private void broadcastRawRocketTelemetry( String telemetry ) {
		Intent intent = new Intent( BroadcastIntents.RAW_ROCKET_TELEMETRY);
		Bundle b = new Bundle();
		b.putString("telemetry", telemetry);
		intent.putExtras(b);

		sendLocalBroadcast(intent);
	}

	private void sendLocalBroadcast( Intent i ) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
	}
	/**
	 * 
	 */
	private SensorEventListener sensorListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		@Override
		public void onSensorChanged(SensorEvent event) {

			// let's broadcast compass data to any activity waiting for updates
			Intent intent = new Intent(BroadcastIntents.COMPASS_UPDATE);

			// packing azimuth value into bundle
			Bundle bundle = new Bundle();
			bundle.putFloat("azimuth", event.values[0]);
			bundle.putFloat("pitch", event.values[1]);
			bundle.putFloat("roll", event.values[2]);

			RocketTrackState.getInstance().setAzimuth(event.values[0]);
			RocketTrackState.getInstance().setDeclination(event.values[1]);
			
			intent.putExtras(bundle);

			// broadcasting compass updates
			sendLocalBroadcast(intent);

		}

	};

	// //////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This is the object that receives interactions from clients
	 */
	private final IBinder mBinder = new LocalBinder();

	private boolean bound;

	@Override
	public IBinder onBind(Intent intent) {
		bound = true;
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		bound = false;
		return true;
	}

	public class LocalBinder extends Binder {
		public AppService getService() {
			return AppService.this;
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Initialize service
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		// Set up the kill switch timer.
		registerReceiver(nextTimeLimitCheckReceiver, new IntentFilter(IN_USE_CHECK));
		this.scheduleNextTimeLimitCheck();

		// location sensor
		// first time we call startLocationUpdates from MainActivity
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// orientation sensor
		this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		startLocationUpdates();

		AppService.running = true;

		this.requestLastKnownLocation();

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		AppService.running = false;

		// stop listener without delay
		this.stopLocationUpdatesNow();

		this.stopSensorUpdates();

		this.locationManager = null;
		this.sensorManager = null;

		// Demote us from the foreground, and cancel the persistent notification.
		stopForeground(true);

		if (device != null) {
			Log.d(TAG, "Disconnected from " + device.getName());
			stopAltosBluetooth();
		}

		// cancel alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.cancel(nextTimeLimitCheckSender);
		unregisterReceiver(nextTimeLimitCheckReceiver);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.telemetry_service_stopped, Toast.LENGTH_SHORT).show();

		super.onDestroy();

	}

	/**
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		CharSequence text = getText(R.string.telemetry_service_started);

		// Create notification to be displayed while the service runs
		Notification notification = new Notification(R.drawable.ic_launcher, text, 0);

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Main.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.telemetry_service_label), text, contentIntent);

		// Set the notification to be in the "Ongoing" section.
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		// Move us into the foreground.
		startForeground(NOTIFICATION, notification);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 */
	private PendingIntent nextTimeLimitCheckSender;
	private final static String IN_USE_CHECK = "inusecheck";

	/**
	 * Scheduling regular checks for GPS signal availability
	 */
	private void scheduleNextTimeLimitCheck() {

		Log.d(TAG, "AppService.scheduleNextRequestTimeLimitCheck");

		Intent intent = new Intent(IN_USE_CHECK);
		nextTimeLimitCheckSender = PendingIntent.getBroadcast(AppService.this, 0, intent, 0);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, 5);

		// schedule single alarm
		// if GPS signal is not available we will schedule this event again in
		// receiver
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), nextTimeLimitCheckSender);
	}

	/**
	 * Receives broadcast event every 5 seconds in order to control presence of
	 * GPS signal
	 */
	private BroadcastReceiver nextTimeLimitCheckReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			// controlling the time passed since requestStartTime
			if (bound /* || isRecording */ ) {
				scheduleNextTimeLimitCheck();
				return;
			}
			stopSelf();
		}
	};

	/**
	 * is service running?
	 */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isGpsInUse() {
		return this.gpsInUse;
	}

	/**
	 * Requesting last location from GPS or Network provider
	 */
	public void requestLastKnownLocation() {

		Location location;

		if (currentLocation != null) {
			return;
		}

		// get last known location from gps provider
		location = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		if (location != null) {
			broadcastLocationUpdate(location, BroadcastIntents.LOCATION_UPDATE);
		}

		currentLocation = location;

	}

	/**
	 * 
	 */
	private void startLocationUpdates() {

		this.listening = false;

		this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

		// setting gpsInUse to true, but listening is still false at this point
		// listening is set to true with first location update in
		// LocationListener.onLocationChanged
		gpsInUse = true;
	}

	/**
	 * stop location updates without giving a chance for other activities to
	 * grab GPS sensor
	 */
	private void stopLocationUpdatesNow() {
		locationManager.removeUpdates(locationListener);
		listening = false;
		gpsInUse = false;
	}

	public void startSensorUpdates() {
		this.sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * stop compass listener
	 */
	public void stopSensorUpdates() {
		this.sensorManager.unregisterListener(sensorListener);
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	public void connectToRocketTracker( BluetoothDevice device ) {
		Log.d(TAG, "Connect command received");
		if (device == null) {
			return;
		}
		if (mAltosBluetooth != null ) {
			stopAltosBluetooth();
		}

		if (mAltosBluetooth == null) {
			Log.d(TAG,"Device = " + String.valueOf(device));
			Log.d(TAG, String.format("startAltosBluetooth(): Connecting to %s (%s)", device.getName(), device.getAddress()));
			this.device = device;
			mAltosBluetooth = new RocketTrackBluetooth(device, mHandler);
			setState(STATE_CONNECTING);
		}
	}

	private synchronized void setState(int s) {
		Log.d(TAG, "setState(): " + state + " -> " + s);
		state = s;
		Intent intent = new Intent( BroadcastIntents.BLUETOOTH_STATE_CHANGE );
		Bundle b = new Bundle();
		b.putInt(BroadcastIntents.STATE, s);
		b.putParcelable(BroadcastIntents.DEVICE, device);
		intent.putExtras(b);

		sendLocalBroadcast(intent);
	}

	private void stopAltosBluetooth() {
		Log.d(TAG, "stopAltosBluetooth(): begin");
		setState(STATE_READY);
		if (mAltosBluetooth != null) {
			Log.d(TAG, "stopAltosBluetooth(): stopping AltosBluetooth");
			mAltosBluetooth.close();
			mAltosBluetooth = null;
		}
		device = null;
	}

	private void connected() {
		if (mAltosBluetooth == null) {
			// If this timed out, then we really want to retry it, but
			// probably safer to just retry the connection from scratch.
			mHandler.obtainMessage(MSG_CONNECT_FAILED).sendToTarget();
			return;
		}

		setState(STATE_CONNECTED);

		//		mTelemetryLogger = new TelemetryLogger(this, mAltosBluetooth);
	}

	// Handler of incoming messages from clients.
	static class IncomingHandler extends Handler {
		private final WeakReference<AppService> service;
		IncomingHandler(AppService s) { service = new WeakReference<AppService>(s); }

		@Override
		public void handleMessage(Message msg) {
			AppService s = service.get();
			switch (msg.what) {
			case MSG_CONNECTED:
				Log.d(TAG, "Connected to device");
				s.connected();
				break;
			case MSG_CONNECT_FAILED:
				Log.d(TAG, "Connection failed");
				// FIXME - notify user.
				break;
			case MSG_DISCONNECTED:
				// Only do the following if we haven't been shutdown elsewhere..
				if (s.device != null) {
					Log.d(TAG, "Disconnected from " + s.device.getName());
					s.stopAltosBluetooth();
				}
				break;
			case MSG_TELEMETRY:
				// forward telemetry messages
				String msgString = (String) msg.obj;
				RocketTrackState.getInstance().getRawDataAdapter().addRawData(msgString);
				s.broadcastRawRocketTelemetry(msgString);
				try {
					Location l = Parser.parse(msgString);
					if( l != null ) {
						// activities with registered receivers will get location updates
						RocketTrackState.getInstance().getLocationDataAdapter().setRocketLocation(l);
						s.broadcastLocationUpdate(l, BroadcastIntents.ROCKET_LOCATION_UPDATE);
					}
				} catch (Throwable t) {
					Log.d(TAG,"Exception: " + t);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

}
