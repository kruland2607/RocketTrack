package org.rockettrack.service;

import java.lang.ref.WeakReference;

import org.rockettrack.Main;
import org.rockettrack.R;
import org.rockettrack.RocketTrackState;
import org.rockettrack.nmea.Parser;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
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
	NotificationCompat.Builder notificationBuilder;

	private static boolean running = false;

	// Name of the connected device
	private BluetoothDevice device           = null;
	private RocketTrackBluetooth  mAltosBluetooth  = null;
	private final Handler   mHandler   = new IncomingHandler(this);

	// internally track state of bluetooth connection
	private int state = STATE_NONE;

	public static final int STATE_NONE       = 0;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED  = 3;
	public static final int STATE_CONNECT_FAILED = 4;
	public static final int STATE_SHUTDOWN = 5;

	static final int MSG_CONNECTED         = 4;
	static final int MSG_CONNECT_FAILED    = 5;
	static final int MSG_DISCONNECTED      = 6;
	static final int MSG_TELEMETRY         = 7;

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

		AppService.running = true;

	}

	/**
	 * Service destructor
	 */
	@Override
	public void onDestroy() {

		AppService.running = false;

		setState(STATE_SHUTDOWN);

		// Demote us from the foreground, and cancel the persistent notification.
		stopForeground(true);

		if (device != null) {
			Log.d(TAG, "Disconnected from " + device.getName());
			stopAltosBluetooth();
		}

		// Tell the user we stopped.
		Toast.makeText(this, R.string.telemetry_service_stopped, Toast.LENGTH_SHORT).show();

		super.onDestroy();

	}

	/**
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);

		CharSequence text = getText(R.string.telemetry_service_started);

		// Create notification to be displayed while the service runs
		notificationBuilder = new NotificationCompat.Builder( this );
		notificationBuilder.setSmallIcon(R.drawable.ic_stat_notify_icon);
		notificationBuilder.setTicker(text);
		notificationBuilder.setContentTitle(getText(R.string.telemetry_service_label));
		notificationBuilder.setContentText(text);
		notificationBuilder.setOngoing(true);
		notificationBuilder.setContentIntent(contentIntent);
		
		Notification notification = notificationBuilder.build();

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
		if ( state == STATE_CONNECTED &&  mAltosBluetooth != null && device.getAddress().equals( this.device.getAddress()) ) {
			return;
		}

		if (mAltosBluetooth != null ) {
			stopAltosBluetooth();
		}

		String text = getText(R.string.telemetry_service_connecting).toString();
		text += " " + device.getName();
		notificationBuilder.setContentText(text);
		notificationBuilder.setProgress(0, 0, true);
		updateNotification();

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
		if ( state != STATE_SHUTDOWN ) {
			Log.d(TAG, "setState(): " + state + " -> " + s);
			state = s;
			Intent intent = new Intent( BroadcastIntents.BLUETOOTH_STATE_CHANGE );
			Bundle b = new Bundle();
			b.putInt(BroadcastIntents.STATE, s);
			b.putParcelable(BroadcastIntents.DEVICE, device);
			intent.putExtras(b);

			sendLocalBroadcast(intent);
		}
	}

	private void stopAltosBluetooth() {
		Log.d(TAG, "stopAltosBluetooth(): begin");
		setState(STATE_NONE);
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

		String text = getText(R.string.telemetry_service_connected).toString();
		text += " " + device.getName();
		notificationBuilder.setContentText(text);
		notificationBuilder.setProgress(0, 0, false);
		updateNotification();
		
		setState(STATE_CONNECTED);

		// Send setup commands: 
		// this should increase the run time of the tx.
		mAltosBluetooth.print("$PMTK314,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0*29\r\n");
		// This one sets the dgps mode to sbas:
		mAltosBluetooth.print("$PMTK301,2*2E\r\n");
		// And this one enables sbas:
		mAltosBluetooth.print("$PMTK313,1*2E\r\n");
	}

	private void connectFailed() {
		setState(STATE_CONNECT_FAILED);

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
				s.connectFailed();
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

	private void updateNotification() {
		NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mgr.notify(NOTIFICATION, notificationBuilder.build());
	}
	
}
