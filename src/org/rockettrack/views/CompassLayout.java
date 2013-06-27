package org.rockettrack.views;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CompassLayout 
{
	private int mDisplayWidthInPixel;
	// private int mDisplayHeightInPixel;
	private float mDisplayScaledDensity;
	
	// Position of main blocks
	private int mTargetInformationY;
	private int mCompassY;
	
	public int FirstColumnX;
	
	// Top two rows (target, latitude, longitude, distance, bearing) 
	public int TargetInformationTargetHeaderY;
	public int TargetInformationTargetValueY;
	public int TargetInformationBearingHeaderY;
	public int TargetInformationBearingValueY;

	// Compass metrics (x,y position, width)
	public int CompassCenterX;
	public int CompassCenterY;
	public int CompassRadius;
	public int CompassRadiusTwoThirds;
	public int CompassRadiusOneThird;
	public int CompassTriangleWidth;
	
	// 

	/**
	 * 
	 * @param displayDensity
	 */
	public CompassLayout(final int displayWidthInPixel, final int displayHeightInPixel, final float displayScaledDensity)
	{
		this.mDisplayWidthInPixel = displayWidthInPixel;
		// this.mDisplayHeightInPixel = displayHeightInPixel;
		this.mDisplayScaledDensity = displayScaledDensity;
		
		this.defineMainPositions();
		
		this.defineTargetInformationPositions();
		this.defineCompassMetrics();
	}
	
	/**
	 * Define main position of target, compass and GPS information
	 */
	private void defineMainPositions()
	{
		this.mTargetInformationY = (int)(15 * this.mDisplayScaledDensity);
		this.mCompassY = (int)(40 * this.mDisplayScaledDensity);
		
		this.FirstColumnX = (int)(10 * this.mDisplayScaledDensity);
	}

	/**
	 * Define positions of labels on top (Target, Lat, Long, Distance, Bearing)
	 */
	private void defineTargetInformationPositions()
	{
		// Y Position of Target, Latitude, Longitude
		this.TargetInformationTargetHeaderY = this.mTargetInformationY;
		this.TargetInformationTargetValueY = this.mTargetInformationY + (int)(16 * this.mDisplayScaledDensity);
		this.TargetInformationBearingHeaderY = this.mTargetInformationY + (int)(34 * this.mDisplayScaledDensity);
		this.TargetInformationBearingValueY = this.mTargetInformationY + (int)(50 * this.mDisplayScaledDensity);		
	}
	
	/**
	 * 
	 */
	private void defineCompassMetrics()
	{
		this.CompassCenterX = this.mDisplayWidthInPixel / 2;
		this.CompassCenterY = this.mDisplayWidthInPixel / 2 + this.mCompassY;
		
		this.CompassRadius = this.CompassCenterX - (int)(30 * this.mDisplayScaledDensity);
		this.CompassRadiusTwoThirds = this.CompassRadius * 2 / 3;
		this.CompassRadiusOneThird = this.CompassRadius * 1 / 3;
		
		this.CompassTriangleWidth = (int)(7 * this.mDisplayScaledDensity);
	}
}
