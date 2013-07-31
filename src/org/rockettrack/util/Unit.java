package org.rockettrack.util;

/**
 * 
 * @author Martin Preishuber
 *
 */
public enum Unit 
{
	meter,
	feet;

	public static Unit getUnitForString(final String unitSystem)
	{
		Unit unit;
		if (unitSystem.equalsIgnoreCase("metric"))
			unit = Unit.meter;
		else
			unit = Unit.feet;
		return unit;
	}


}
