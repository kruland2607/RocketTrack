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
			String latumString = p[3];
			String lonString = p[4];
			String lonumString = p[5];
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
				Double d = toDecimalDegrees(latString, latumString);
				if( d != null ) {
					l.setLatitude(d);
				}
			}
			{
				Double d = toDecimalDegrees(lonString, lonumString);
				if( d != null ) {
					l.setLongitude(d);
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
