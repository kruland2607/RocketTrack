package org.rockettrack.service;

public final class BroadcastIntents {

	public static String LOCATION_UPDATE = "location_update";
	public static String ROCKET_LOCATION_UPDATE = "rocket_location_update";
	public static String RAW_ROCKET_TELEMETRY = "raw_rocket_telemetry";
	public static String BLUETOOTH_STATE_CHANGE = "bluetooth_state_change";
	
	// bundle keys for BLUETOOTH_STATE_CHANGE messages:
	public static String DEVICE = "device";
	public static String STATE = "state";
}
