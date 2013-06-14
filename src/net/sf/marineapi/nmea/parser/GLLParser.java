/* 
 * GLLParser.java
 * Copyright (C) 2010 Kimmo Tuukkanen
 * 
 * This file is part of Java Marine API.
 * <http://ktuukkan.github.io/marine-api/>
 * 
 * Java Marine API is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Java Marine API is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java Marine API. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.marineapi.nmea.parser;

import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.TalkerId;
import net.sf.marineapi.nmea.util.CompassPoint;
import net.sf.marineapi.nmea.util.DataStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;

/**
 * GLL Sentence parser.
 * 
 * @author Kimmo Tuukkanen
 */
class GLLParser extends PositionParser implements GLLSentence {

	// field indices
	private static final int LATITUDE = 0;
	private static final int LAT_HEMISPHERE = 1;
	private static final int LONGITUDE = 2;
	private static final int LON_HEMISPHERE = 3;
	private static final int UTC_TIME = 4;
	private static final int DATA_STATUS = 5;

	/**
	 * Creates a new instance of GLLParser.
	 * 
	 * @param nmea GLL sentence String.
	 * @throws IllegalArgumentException If the given sentence is invalid or does
	 *             not contain GLL sentence.
	 */
	public GLLParser(String nmea) {
		super(nmea, SentenceId.GLL);
	}

	/**
	 * Creates GSA parser with empty sentence.
	 * 
	 * @param talker TalkerId to set
	 */
	public GLLParser(TalkerId talker) {
		super(talker, SentenceId.GLL, 6);
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.marineapi.nmea.sentence.PositionSentence#getPosition()
	 */
	public Position getPosition() {
		double lat = parseLatitude(LATITUDE);
		double lon = parseLongitude(LONGITUDE);
		CompassPoint lath = parseHemisphereLat(LAT_HEMISPHERE);
		CompassPoint lonh = parseHemisphereLon(LON_HEMISPHERE);
		return new Position(lat, lath, lon, lonh);
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.marineapi.nmea.sentence.GLLSentence#getDataStatus()
	 */
	public DataStatus getStatus() {
		return DataStatus.valueOf(getCharValue(DATA_STATUS));
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.marineapi.nmea.sentence.TimeSentence#getTime()
	 */
	public Time getTime() {
		String str = getStringValue(UTC_TIME);
		int h = Integer.parseInt(str.substring(0, 2));
		int m = Integer.parseInt(str.substring(2, 4));
		double s = Double.parseDouble(str.substring(4, 6));
		return new Time(h, m, s);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * net.sf.marineapi.nmea.sentence.PositionSentence#setPosition(net.sf.marineapi
	 * .nmea.util.Position)
	 */
	public void setPosition(Position pos) {
		setLatitude(LATITUDE, pos.getLatitude());
		setLongitude(LONGITUDE, pos.getLongitude());
		setLatHemisphere(LAT_HEMISPHERE, pos.getLatHemisphere());
		setLonHemisphere(LON_HEMISPHERE, pos.getLonHemisphere());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * net.sf.marineapi.nmea.sentence.GLLSentence#setDataStatus(net.sf.marineapi
	 * .nmea.util.DataStatus)
	 */
	public void setStatus(DataStatus status) {
		setCharValue(DATA_STATUS, status.toChar());
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * net.sf.marineapi.nmea.sentence.TimeSentence#setTime(net.sf.marineapi.
	 * nmea.util.Time)
	 */
	public void setTime(Time t) {
		int h = t.getHour();
		int m = t.getMinutes();
		int s = (int) Math.floor(t.getSeconds());
		String time = String.format("%02d%02d%02d", h, m, s);
		setStringValue(UTC_TIME, time);
	}
}
