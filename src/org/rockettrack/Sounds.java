/*
 * Copyright (C) 2013 Fran�ois Girard
 * 
 * This file is part of Rocket Finder.
 *
 * Rocket Finder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Rocket Finder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Rocket Finder. If not, see <http://www.gnu.org/licenses/>.*/
 
package org.rockettrack;

import android.media.AudioManager;
import android.media.MediaPlayer;

public class Sounds {
	public static MediaPlayer gps_connected;
	public static MediaPlayer gps_disconnected;
	public static MediaPlayer pulse;
	public static MediaPlayer radar_beep;
	
	public static void init(Main mainActivity){
		mainActivity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		AudioManager audioManager = (AudioManager) mainActivity.getSystemService(Main.AUDIO_SERVICE);
		
//		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
		
		gps_connected = MediaPlayer.create(mainActivity, R.raw.gps_connected);
		gps_disconnected = MediaPlayer.create(mainActivity, R.raw.gps_disconnected);   
		pulse = MediaPlayer.create(mainActivity, R.raw.pulse);   
		radar_beep = MediaPlayer.create(mainActivity, R.raw.radar_beep);
		radar_beep.setVolume(0.2f, 0.2f);
		
	}
	
	public static void release(){
		gps_connected.release();
		gps_connected = null;
	
		gps_disconnected.release();
		gps_disconnected = null;
		
		pulse.release();
		pulse = null;

		radar_beep.release();
		radar_beep = null;
	}
}
