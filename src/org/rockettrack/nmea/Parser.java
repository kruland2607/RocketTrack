package org.rockettrack.nmea;

import android.location.Location;

public class Parser {

	public static Location parse( String sentence ) {
		if ( sentence.startsWith("$GPGGA") ) {
			Location l = new Location("RocketTrack");

			String[] p = sentence.split(",");

			// Example:
			
			String timeString = p[1];
			String latString = p[2];
			String latHemString = p[3];
			String lonString = p[4];
			String lonHemString = p[5];
			String fixString = p[6];
			String satString = p[7];
			String hdopString = p[8];
			String altString = p[9];
			String altUnitString = p[10];
			String geoidHeightString = p[11];
			String geoidHeightUnitString = p[12];

			{
				Double d = toDoubleMeters( altString, altUnitString );
				if ( d != null ) {
					l.setAltitude( d );
				}
			}
			{
				Double d = toDecimalDegrees(latString, latHemString);
				if( d != null ) {
					l.setLatitude(d);
				}
			}
			{
				Double d = toDecimalDegrees(lonString, lonHemString);
				if( d != null ) {
					l.setLongitude(d);
				}
			}
			{
				// Set accuracy to 30m because it's a number.
				l.setAccuracy(30f);
				try {
					Float d = Float.parseFloat( hdopString );
					l.setAccuracy(d * 10f);
					// Read: http://www.edu-observatory.org/gps/gps_accuracy.html 
				} catch ( NumberFormatException ex ) {
				}
			}
			
			return l;
		} else {
			return null;
		}
	}

	private static Double toDoubleMeters( String dimension, String unit ) {
		try {
			double value = Double.parseDouble( dimension ) ;
			if ( "M".equals(unit ) ) {
				return value;
			} else {
				throw new RuntimeException("Need to parse unit: " + unit);
			}
		} catch (NumberFormatException ex ) {
			return null;
		}
	}

	private static Double toDecimalDegrees( String ddmm, String dir ) {

		try {

			double value = Double.parseDouble(ddmm);

			double deg = Math.floor( value / 100 );

			double min = value - deg *100;

			double decimalDegrees = deg + min/60.0;

			if( "W".equals(dir) || "S".equals(dir) ) {
				decimalDegrees *= -1.0;
			}

			return decimalDegrees;
		} catch ( NumberFormatException ex ) {
			return null;
		}

	}
}
