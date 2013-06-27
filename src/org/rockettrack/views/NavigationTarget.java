package org.rockettrack.views;

import java.util.Date;

import android.location.Location;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class NavigationTarget extends Location
{
	private static final int NAME_MAX_CHARS = 16;

	private String mName = "";
	private Date mDateCreated;
	private Date mDateLastAccess;
	private Long mId = Long.MIN_VALUE;

	/**
	 * 
	 */
	public NavigationTarget()
	{
		this("Dummy");
	}
	
	/**
	 * 
	 * @param provider
	 */
	public NavigationTarget(String provider) 
	{
		super(provider);
	}

	/**
	 * 
	 * @return
	 */
	public long getId() 
	{
		return this.mId;
	}

	/**
	 * 
	 * @param id
	 */
	public void setId(long id) 
	{
		this.mId = id;
	}
		  
	/**
	 * 
	 * @param name
	 */
	public void setName(String name)
	{
    	if (name.length() > NAME_MAX_CHARS)
    	{
    		name = name.substring(0, NAME_MAX_CHARS);
    		int intIndexOfBlank = name.lastIndexOf(" ");
    		if (intIndexOfBlank != -1)
    			name = name.substring(0, intIndexOfBlank);
    	}
		this.mName = name;
	}

	/**
	 * 
	 * @return
	 */
	public String getName()
	{
		return this.mName;
	}

	/**
	 * 
	 * @return
	 */
	public Date getDateCreated()
	{
		return this.mDateCreated;
	}
	
	/**
	 * 
	 * @param date
	 */
	public void setDateCreated(Date date)
	{
		this.mDateCreated = date;
	}

	/**
	 * 
	 * @return
	 */
	public Date getDateLastAccess()
	{
		return this.mDateLastAccess;
	}
	
	/**
	 * 
	 * @param date
	 */
	public void setDateLastAccess(Date date)
	{
		this.mDateLastAccess = date;
	}
	
	/**
	 * 
	 * @param latitude
	 */
	public void setLatitude(Object latitude)
	{
		if (latitude instanceof Double)
			super.setLatitude((Double) latitude);
		else if (latitude instanceof Float)
			super.setLatitude(((Float) latitude).doubleValue());
	}
		
	/**
	 * 
	 * @param longitude
	 */
	public void setLongitude(Object longitude)
	{
		if (longitude instanceof Double)
			super.setLongitude((Double) longitude);	
		else if (longitude instanceof Float)
			super.setLongitude(((Float) longitude).doubleValue());	
	}
}
