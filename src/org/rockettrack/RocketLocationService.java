package org.rockettrack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class RocketLocationService extends Service implements LocationListener {
	
	private final static String TAG = "RocketLocationService";

	static final int MSG_REGISTER_CLIENT   = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_CONNECT           = 3;
	static final int MSG_CONNECTED         = 4;
	static final int MSG_CONNECT_FAILED    = 5;
	static final int MSG_DISCONNECTED      = 6;
	static final int MSG_TELEMETRY         = 7;

	// internally track state of bluetooth connection
	private int state = STATE_NONE;

	private static final int STATE_NONE       = 0;
	private static final int STATE_READY      = 1;
	private static final int STATE_CONNECTING = 2;
	private static final int STATE_CONNECTED  = 3;

	// Timer - we wake up every now and then to decide if the service should stop
	private Timer timer = new Timer();

	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
	final Handler   mHandler   = new IncomingHandler(this);
	final Messenger mMessenger = new Messenger(mHandler); // Target we publish for clients to send messages to IncomingHandler.

	// Last data seen; send to UI when it starts
	private Location last_location;

	// Name of the connected device
	private BluetoothDevice device           = null;
	private RocketTrackBluetooth  mAltosBluetooth  = null;
	
	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.telemetry_service_label;

	// Handler of incoming messages from clients.
	static class IncomingHandler extends Handler {
		private final WeakReference<RocketLocationService> service;
		IncomingHandler(RocketLocationService s) { service = new WeakReference<RocketLocationService>(s); }

		@Override
		public void handleMessage(Message msg) {
			RocketLocationService s = service.get();
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				s.mClients.add(msg.replyTo);
				try {
					// Now we try to send the freshly connected UI any relavant information about what
					// we're talking to - Basically state and Config Data.
					msg.replyTo.send(Message.obtain(null, Main.MSG_STATE_CHANGE, 1/*s.state*/, -1, null/*s.mConfigData*/));
					// We also send any recent telemetry or location data that's cached
//					if (s.last_location   != null) msg.replyTo.send(Message.obtain(null, AltosDroid.MSG_LOCATION , s.last_location  ));
				} catch (RemoteException e) {
					s.mClients.remove(msg.replyTo);
				}
				Log.d(TAG, "Client bound to service");
				break;
			case MSG_UNREGISTER_CLIENT:
				s.mClients.remove(msg.replyTo);
				Log.d(TAG, "Client unbound from service");
				break;
			case MSG_CONNECT:
				Log.d(TAG, "Connect command received");
				s.device = (BluetoothDevice) msg.obj;
				s.startAltosBluetooth();
				break;
			case MSG_CONNECTED:
				Log.d(TAG, "Connected to device");
				s.connected();
				break;
			case MSG_CONNECT_FAILED:
				Log.d(TAG, "Connection failed... retrying");
				s.startAltosBluetooth();
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
				s.sendMessageToClients(Message.obtain(null, Main.MSG_TELEMETRY, msg.obj));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private synchronized void setState(int s) {
		Log.d(TAG, "setState(): " + state + " -> " + s);
		state = s;

		sendMessageToClients(Message.obtain(null, Main.MSG_STATE_CHANGE, state, -1, null));
	}

	private void sendMessageToClients(Message m) {
		for (int i=mClients.size()-1; i>=0; i--) {
			try {
				mClients.get(i).send(m);
			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	private void startAltosBluetooth() {
		if (device == null) {
			return;
		}
		if (mAltosBluetooth == null) {
			Log.d(TAG, String.format("startAltosBluetooth(): Connecting to %s (%s)", device.getName(), device.getAddress()));
			mAltosBluetooth = new RocketTrackBluetooth(device, mHandler);
			setState(STATE_CONNECTING);
		} else {
			// This is a bit of a hack - if it appears we're still connected, we treat this as a restart.
			// So, to give a suitable delay to teardown/bringup, we just schedule a resend of a message
			// to ourselves in a few seconds time that will ultimately call this method again.
			// ... then we tear down the existing connection.
			// We do it this way around so that we don't lose a reference to the device when this method
			// is called on reception of MSG_CONNECT_FAILED in the handler above.
			mHandler.sendMessageDelayed(Message.obtain(null, MSG_CONNECT, device), 3000);
			stopAltosBluetooth();
		}
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

	private void onTimerTick() {
		Log.d(TAG, "Timer wakeup");
		try {
			if (mClients.size() <= 0 && state != STATE_CONNECTED) {
				stopSelf();
			}
		} catch (Throwable t) {
			Log.e(TAG, "Timer failed: ", t);
		}
	}

	@Override
	public void onCreate() {
		// Create a reference to the NotificationManager so that we can update our notifcation text later
		//mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		setState(STATE_READY);

		// Start our timer - first event in 10 seconds, then every 10 seconds after that.
		timer.scheduleAtFixedRate(new TimerTask(){ public void run() {onTimerTick();}}, 10000L, 10000L);

		// Listen for GPS and Network position updates
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("TelemetryService", "Received start id " + startId + ": " + intent);

		CharSequence text = getText(R.string.telemetry_service_started);

		// Create notification to be displayed while the service runs
		Notification notification = new Notification(R.drawable.am_status_c, text, 0);

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

	@Override
	public void onDestroy() {

		// Stop listening for location updates
		((LocationManager) getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);

		// Stop the bluetooth Comms threads
		stopAltosBluetooth();

		// Demote us from the foreground, and cancel the persistent notification.
		stopForeground(true);

		// Stop our timer
		if (timer != null) {timer.cancel();}

		// Tell the user we stopped.
		Toast.makeText(this, R.string.telemetry_service_stopped, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onLocationChanged(Location location) {
		last_location = location;
		sendMessageToClients(Message.obtain(null, Main.MSG_LOCATION, location));
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}

}
