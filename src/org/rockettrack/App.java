package org.rockettrack;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class App extends Application {

	private final static String APP_NAME = "RocketTrack";
	
	/**
	 * Android shared preferences
	 */
	private SharedPreferences preferences;

	/**
	 * application directory
	 */
	private String appDir;

	/**
	 * is external storage writable
	 */
	private boolean externalStorageWriteable = false;

	/**
	 * is external storage available, ex: SD card
	 */
	private boolean externalStorageAvailable = false;


	public boolean getExternalStorageAvailable() {
		return externalStorageAvailable;
	}

	public boolean getExternalStorageWriteable() {
		return externalStorageWriteable;
	}

	public SharedPreferences getPreferences() {
		return preferences;
	}

	public String getAppDir() {
		return appDir;
	}

	@Override
	public void onCreate() {

		super.onCreate();

		// accessing preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		// set application external storage folder
		appDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APP_NAME;

		setExternalStorageState();

		// create all folders required by the application on external storage
		if (getExternalStorageAvailable() && getExternalStorageWriteable()) {
			createFolderStructure();
		} else {
			Toast.makeText(this, R.string.memory_card_not_available,
					Toast.LENGTH_SHORT).show();
		}

	}
	
	/**
	 * Checking if external storage is available and writable
	 */
	private void setExternalStorageState() {

		// checking access to SD card
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}

	}

	/**
	 * Create application folders
	 */
	private void createFolderStructure() {
		Utils.createFolder(getAppDir());
	}

}
