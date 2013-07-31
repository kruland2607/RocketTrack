package org.rockettrack.util;

import java.util.Hashtable;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class UnitConverter 
{
	
	private Hashtable<String, Double> mUnitConversionFactors;

	/**
	 * 
	 */
	public UnitConverter()
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
	public Double convert(Unit source, Unit target, Double value)
	{	
		final Double dblFactor = this.getConversionFactor(source, target);
		return (value * dblFactor);
	}

	/**
	 * 
	 * @param source
	 * @param target
	 * @param value
	 * @return
	 */
	public Float convert(Unit source, Unit target, Float value)
	{	
		final Float fltFactor = this.getConversionFactor(source, target).floatValue();
		return (value * fltFactor);
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{
		UnitConverter uc = new UnitConverter();
		System.out.println(uc.convert(Unit.meter, Unit.feet, 7.0));
		System.out.println(uc.convert(Unit.feet, Unit.meter, 7.0));
	}

}
