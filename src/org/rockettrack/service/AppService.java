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
import android.location.Location;
import android.location.LocationManager;
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

	// //////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This is the object that receives interactions from clients
	 */
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
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

		AppService.running = true;

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		AppService.running = false;

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
		Notification notification = new Notification(R.drawable.ic_stat_notify_icon, text, 0);

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
			if ( state == STATE_CONNECTED ) {
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

	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////////////

	public void connectToRocketTracker( BluetoothDevice device ) {
		Log.d(TAG, "Connect command received");
		if (device == null) {
			return;
		}
		// already connected to this device?
		if ( mAltosBluetooth != null && device.getAddress().equals( this.device.getAddress()) ) {
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

	public void stopService() {
		stopSelf();
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
				RocketTrackState.getInstance().clear();
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
