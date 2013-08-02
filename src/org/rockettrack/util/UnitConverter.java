package org.rockettrack.util;

import java.text.DecimalFormat;
import java.util.Hashtable;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class UnitConverter 
{
	
	private static UnitConverter instance = new UnitConverter();
	
	private Hashtable<String, Double> mUnitConversionFactors;

	/**
	 * 
	 */
	private UnitConverter()
	{
		this.InitializeFactors();
	}
	
	/**
	 * 
	 */
	private void InitializeFactors()
	{
		this.mUnitConversionFactors = new Hashtable<String, Double>();
		// Add some conversion factors
		this.addConversionFactor(Unit.meter, Unit.meter, 1.0);
		this.addConversionFactor(Unit.feet, Unit.feet, 1.0);
		this.addConversionFactor(Unit.meter, Unit.feet, 3.2808);
		this.addConversionFactor(Unit.feet, Unit.meter, 1.0/3.2808);
	}
	
	/**
	 * 
	 * @param unitSource
	 * @param unitTarget
	 * @return
	 */
	private String getFactorName(Unit unitSource, Unit unitTarget)
	{
		return String.format("%s_%s", unitSource.toString(), unitTarget.toString());
	}
	
	/**
	 * 
	 * @param unitSource
	 * @param unitTarget
	 * @param factor
	 */
	private void addConversionFactor(Unit unitSource, Unit unitTarget, Double factor)
	{
		final String strFactorName = this.getFactorName(unitSource, unitTarget);
		this.mUnitConversionFactors.put(strFactorName, factor);
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	private Double getConversionFactor(Unit source, Unit target)
	{
		final String strFactorName = this.getFactorName(source, target);
		return this.mUnitConversionFactors.get(strFactorName);
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param value
	 * @return
	 */
	public static double convert(Unit source, Unit target, double value)
	{	
		final double dblFactor = instance.getConversionFactor(source, target);
		return (value * dblFactor);
	}
	
	public static String convertWithUnit(Unit source, Unit target, double value ) {
		double convertedValue = convert(source,target,value);
		String valueString = String.valueOf(convertedValue) + target.abbreviation;
		return valueString;
	}
	
	public static String convertWithUnit(Unit source, Unit target, double value, String formatPattern ) {
		double convertedValue = convert(source,target,value);
		DecimalFormat formatter = new DecimalFormat(formatPattern);
		String valueString = formatter.format(convertedValue) + target.abbreviation;
		return valueString;
	}
	/**
	 * 
	 * @param source
	 * @param target
	 * @param value
	 * @return
	 */
	public static float convert(Unit source, Unit target, float value)
	{	
		final float fltFactor = instance.getConversionFactor(source, target).floatValue();
		return (value * fltFactor);
	}

}
