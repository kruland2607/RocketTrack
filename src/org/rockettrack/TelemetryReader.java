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


package org.rockettrack;

import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;

public class TelemetryReader extends Thread {

	private static final String TAG = "TelemetryReader";

	int         crc_errors;

	Handler     handler;

	RocketTrackBluetooth   link;

	LinkedBlockingQueue<String> telem;

	public String read() throws InterruptedException {
		String l = telem.take();
		return l;
	}

	public void close() {
		//		link.remove_monitor(telem);
		link = null;
		telem.clear();
		telem = null;
	}

	public void run() {
		try {
			for (;;) {
				String record = read();
				if (record == null)
					break;
				handler.obtainMessage(RocketLocationService.MSG_TELEMETRY, record).sendToTarget();
			}
		} catch (InterruptedException ee) {
		} finally {
			close();
		}
	}

	public TelemetryReader (RocketTrackBluetooth in_link, Handler in_handler) {
		link    = in_link;
		handler = in_handler;

		telem = new LinkedBlockingQueue<String>();
//		link.add_monitor(telem);
	}
}
