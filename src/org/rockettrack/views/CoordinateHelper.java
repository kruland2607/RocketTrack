package org.rockettrack.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class CoordinateHelper 
{

	class DM
	{
		int degrees;
		double minutes;
		
		DM(double value)
		{
	        final double dblAbsValue = Math.abs(value * 3600);
	        this.degrees = (int) Math.floor(dblAbsValue / 3600);
	        this.minutes = (dblAbsValue - (degrees * 3600)) / 60;
		}
	}

	private final static String DEGREE_CHAR = "\u00B0";
	private final static String COORDS_REGEXP = "([NnSs]) ?(\\d{1,2}).{0,1} ?(\\d{1,2}\\.\\d{1,4}).?\\s{0,25}([EeWw]) ?(\\d{1,3}).{0,1} ?(\\d{1,2}\\.\\d{1,4})"; 

	private double mdblLatitude;
	private double mdblLongitude;

	/**
	 * 
	 */
	public CoordinateHelper()
	{
	}
	
	/**
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public CoordinateHelper(double latitude, double longitude)
	{
		this.mdblLatitude = latitude;
		this.mdblLongitude = longitude;
	}
	
    /**
     * 
     * @param text
     * @return
     */
    public boolean ParseFromText(String text)
    {
        boolean blnResult = false;
        Pattern regex = Pattern.compile(COORDS_REGEXP, Pattern.DOTALL);
        Matcher matcher = regex.matcher(text);
        if (matcher.find())
        {
        	String strHemisphereLatitude = matcher.group(1).toUpperCase();
            int latDegrees = Integer.parseInt(matcher.group(2));
            double latMinutes = Double.parseDouble(matcher.group(3));
            this.mdblLatitude = latDegrees + latMinutes / 60;
            if (strHemisphereLatitude.equals("S"))
            	this.mdblLatitude *= -1;

            String strHemisphereLongitude = matcher.group(4).toUpperCase();
            int longDegrees = Integer.parseInt(matcher.group(5));
            double longMinutes = Double.parseDouble(matcher.group(6));
            this.mdblLongitude = longDegrees + longMinutes / 60;
            if (strHemisphereLongitude.equals("W"))
            	this.mdblLongitude *= -1;
            
            blnResult = true;
        }
        return blnResult;
    }

    /**
     * 
     * @return
     */
    public double getLatitude()
    {
    	return this.mdblLatitude;
    }
    
	/**
	 * 
	 * @return
	 */
	public String getLatitudeString()
	{
		final String strHemisphereLatitude = (this.mdblLatitude < 0) ? "S" : "N";
		final DM dmLatitude = new DM(this.mdblLatitude);
		final String strLatitude = String.format("%s %02d%s %5.3f\'", strHemisphereLatitude, dmLatitude.degrees, DEGREE_CHAR, dmLatitude.minutes); 
		return strLatitude.replace(",", "."); 
	}

	/**
	 * 
	 * @return
	 */
	public double getLongitude()
	{
		return this.mdblLongitude;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLongitudeString()
	{
		final String strHemisphereLongitude = (this.mdblLongitude < 0) ? "W" : "E";
		final DM dmLongitude = new DM(this.mdblLongitude);
		final String strLongitude = String.format("%s %03d%s %5.3f\'", strHemisphereLongitude, dmLongitude.degrees, DEGREE_CHAR, dmLongitude.minutes);
		return strLongitude.replace(",", ".");
	}
	
	/**
	 * 
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public String toString()
	{
		return String.format("%s %s", this.getLatitudeString(), this.getLongitudeString());
	}
}
