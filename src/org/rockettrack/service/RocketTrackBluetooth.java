/*
 * Copyright © 2011 Keith Packard <keithp@keithp.com>
 * Copyright © 2012 Mike Beattie <mike@ethernal.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.rockettrack.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RocketTrackBluetooth implements Runnable {

	// Debugging
	private static final String TAG = "RocketTrackBluetooth";
	private static final boolean D = true;

	private final int ERROR = -2;
	private final int TIMEOUT = -3;

	private ConnectThread    connect_thread = null;
	private Thread           input_thread   = null;

	private Handler          handler;

	private BluetoothAdapter adapter;
	private BluetoothDevice  device;
	private BluetoothSocket  socket;
	private InputStream      input;
	private OutputStream     output;

	// Constructor
	public RocketTrackBluetooth(BluetoothDevice in_device, Handler in_handler) {
		adapter = BluetoothAdapter.getDefaultAdapter();
		device = in_device;
		handler = in_handler;

		connect_thread = new ConnectThread(device);
		connect_thread.start();

	}

	@Override
	public void run () {
		Log.d(TAG, "Start background thread");
		int c;
		byte[] line_bytes = null;
		int line_count = 0;

		for (;;) {
			c = getchar();
//			Log.d(TAG, "Read char = " + c);
			if (Thread.interrupted()) {
				Log.d(TAG,"INTERRUPTED\n");
				break;
			}
			if (c == ERROR) {
				Log.d(TAG,"ERROR\n");
				break;
			}
			if (c == TIMEOUT) {
				Log.d(TAG,"TIMEOUT\n");
				continue;
			}
			if (c == '\r')
				continue;
			synchronized(this) {
				if (c == '\n') {
					if (line_count != 0) {
						String string = new String(line_bytes, 0, line_count);
						//Log.v(TAG,"Line = " + string);
						Message.obtain(handler, AppService.MSG_TELEMETRY, string).sendToTarget();
						// Here we do something with the string.
						//							add_bytes(line_bytes, line_count);
						line_count = 0;
					}
				} else {
					if (line_bytes == null) {
						line_bytes = new byte[256];
					} else if (line_count == line_bytes.length) {
						byte[] new_line_bytes = new byte[line_count * 2];
						System.arraycopy(line_bytes, 0, new_line_bytes, 0, line_count);
						line_bytes = new_line_bytes;
					}
					line_bytes[line_count] = (byte) c;
					line_count++;
				}
			}
		}
	}

	private class ConnectThread extends Thread {
		private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp_socket = null;
			setName("ConnectThread");

			try {
				tmp_socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = tmp_socket;
		}

		public void run() {
			if (D) Log.d(TAG, "ConnectThread: BEGIN");

			// Always cancel discovery because it will slow down a connection
			adapter.cancelDiscovery();

			synchronized (RocketTrackBluetooth.this) {
				// Make a connection to the BluetoothSocket
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket.connect();

					input = socket.getInputStream();
					output = socket.getOutputStream();
				} catch (IOException e) {
					Log.e(TAG,"Exception connecting", e);
					// Close the socket
					try {
						socket.close();
					} catch (IOException e2) {
						if (D) Log.e(TAG, "ConnectThread: Failed to close() socket after failed connection");
					}
					input = null;
					output = null;
					// Sleep a little bit to prevent hogging all kinds of resources.
					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
					}
					RocketTrackBluetooth.this.notifyAll();
					handler.obtainMessage(AppService.MSG_CONNECT_FAILED).sendToTarget();
					if (D) Log.e(TAG, "ConnectThread: Failed to establish connection");
					return;
				}

				input_thread = new Thread(RocketTrackBluetooth.this);
				input_thread.setName("input_thread");
				input_thread.start();

				// Let TelemetryService know we're connected
				Message m = handler.obtainMessage(AppService.MSG_CONNECTED,device.getName());
				m.sendToTarget();

				// Notify other waiting threads, now that we're connected
				RocketTrackBluetooth.this.notifyAll();

				// Reset the ConnectThread because we're done
				connect_thread = null;

				if (D) Log.d(TAG, "ConnectThread: Connect completed");
			}
		}

		public void cancel() {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				if (D) Log.e(TAG, "ConnectThread: close() of connect socket failed", e);
			}
		}
	}

	private synchronized void wait_connected() throws InterruptedException, IOException {
		if (input == null) {
			wait();
			if (input == null) throw new IOException();
		}
	}

	private void connection_lost() {
		if (D) Log.e(TAG, "Connection lost during I/O");
		handler.obtainMessage(AppService.MSG_CONNECT_FAILED).sendToTarget();
	}

	public void print(String data) {
		byte[] bytes = data.getBytes();
		if (D) Log.d(TAG, "print(): begin");
		try {
			wait_connected();
			output.write(bytes);
			output.flush();
			if (D) Log.d(TAG, "print(): Wrote bytes: '" + data.replace('\n', '\\') + "'");
		} catch (IOException e) {
			connection_lost();
		} catch (InterruptedException e) {
			connection_lost();
		}
	}

	public int getchar() {
		try {
			wait_connected();
			return input.read();
		} catch (IOException e) {
			connection_lost();
		} catch (java.lang.InterruptedException e) {
			connection_lost();
		}
		return ERROR;
	}

	public void close() {
		if (D) Log.d(TAG, "close(): begin");
		synchronized(this) {
			if (D) Log.d(TAG, "close(): synched");

			if (connect_thread != null) {
				if (D) Log.d(TAG, "close(): stopping connect_thread");
				connect_thread.cancel();
				connect_thread = null;
			}
			if (D) Log.d(TAG, "close(): Closing socket");
			try {
				socket.close();
			} catch (IOException e) {
				if (D) Log.e(TAG, "close(): unable to close() socket");
			}
			if (input_thread != null) {
				if (D) Log.d(TAG, "close(): stopping input_thread");
				try {
					if (D) Log.d(TAG, "close(): input_thread.interrupt().....");
					input_thread.interrupt();
					if (D) Log.d(TAG, "close(): input_thread.join().....");
					input_thread.join();
				} catch (Exception e) {}
				input_thread = null;
			}
			input = null;
			output = null;
			notifyAll();
		}
	}

}
