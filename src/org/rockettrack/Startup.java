package org.rockettrack;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * This activity is strictly to ensure that GPS and BT have been enabled.  It then
 * starts the Main activity.
 * 
 * @author kruland
 *
 */
public class Startup extends Activity {

	private static final int REQUEST_ENABLE_BT      = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onStart() {
		super.onStart();

		// Do step 1.
		checkForBluetooth();

	}

	private void checkForBluetooth() {
		// Get local Bluetooth adapter
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			
			// Step 2.
			checkForGPS();
		}
	}

	private void checkForGPS() {
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);

		boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if( !enabled ) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int id) {
					dialog.cancel();
					Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int id) {
					dialog.cancel();
					Utils.displayAbortDialog(Startup.this, R.string.gps_not_enabled);
				}
			});
			final AlertDialog alert = builder.create();
			alert.show();
		} else {
			
			// Step 3
			startServices();
		}
	}

	private void startServices() {
		
		// Finish this activity and start main.
		
		Intent i = new Intent(this, Main.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Do step 2
				checkForGPS();
			} else {
				// User did not enable Bluetooth or an error occured
				Utils.displayAbortDialog(this, R.string.bt_not_enabled);
			}
			break;
		}
	}

}
