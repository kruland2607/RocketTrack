/* 
 * Direction.java
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
package net.sf.marineapi.nmea.util;

/**
 * Defines the relative directions, e.g. "left" and "right".
 * 
 * @author Kimmo Tuukkanen
 */
public enum Direction {

	/** Left */
	LEFT('L'),

	/** Right */
	RIGHT('R');

	private char ch;

	private Direction(char c) {
		ch = c;
	}

	/**
	 * Returns the corresponding char constant.
	 * 
	 * @return Char indicator for Direction
	 */
	public char toChar() {
		return ch;
	}

	/**
	 * Get the enum corresponding to specified char.
	 * 
	 * @param c Char indicator for Direction
	 * @return Direction
	 */
	public static Direction valueOf(char c) {
		for (Direction d : values()) {
			if (d.toChar() == c) {
				return d;
			}
		}
		return valueOf(String.valueOf(c));
	}
}
