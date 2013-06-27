package org.rockettrack.util;

/**
 * 
 * @author Martin Preishuber
 *
 */
public class ColorGradientHelper 
{
	/**
	 * 
	 */
	public ColorGradientHelper()
	{
	}

	/**
	 * 
	 * @param min
	 * @param max
	 * @param value
	 * @return
	 */
	public static int getRedGreenGradient(float min, float max, float value)
	{
		int blue = 0x0;
		int red;
		int green;
		
		// Make sure that min < max
		if (min > max)
		{
			value = (max - value) + min;
	
			final float temp = min;
			min = max;
			max = temp;
		}
		
		// Make sure that value is in (min,max) range
		value = (value < min) ? min : value;
		value = (value > max) ? max : value;
			
		final float difference = max - min; 
		final float middleValue = (difference / 2) + min;
		
		if (value <= middleValue)
		{
			// Range from 0xFF0000 [red] to 0xFFA500 [orange]
			red = 0xFF;
			green = Math.round(((0xA5 - 0x00) / (difference / 2)) * (value - min));
		}
		else
		{
			// Range from 0xFFA500 [orange] to 0x00FF00 [green]
			red = 0xFF - Math.round(((0xFF - 0x00) / (difference / 2)) * (value - middleValue));
			green = 0xA5 + Math.round(((0xFF - 0xA5) / (difference / 2)) * (value - middleValue));
		}

		// System.out.println("r: " + Integer.toHexString(red) + " g: " + Integer.toHexString(green) + " b: " + Integer.toHexString(blue));
		
		final int color = (red << 16) + (green << 8) + blue;

		return color;
	}
	
	/**
	 * 
	 * @param min
	 * @param max
	 * @param value
	 * @param alpha
	 * @return
	 */
	public static int getRedGreenGradient(float min, float max, float value, int alpha)
	{
		final int color = (alpha << 24) + ColorGradientHelper.getRedGreenGradient(min, max, value);

		return color;
	}

	/**
	 * 
	 * @param min
	 * @param max
	 * @param value
	 * @return
	 */
	public static String getRedGreenGradientHtml(float min, float max, float value)
	{
		final int color = ColorGradientHelper.getRedGreenGradient(min, max, value);
		String colorString = Integer.toHexString(color);
		while (colorString.length() < 6)
		{
			colorString = "0" + colorString;
		}
		
		return "#" + colorString;
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// min, max, value, alpha
		System.out.println(Integer.toHexString(ColorGradientHelper.getRedGreenGradient(6, 66, 6, 0xff)));
		System.out.println(Integer.toHexString(ColorGradientHelper.getRedGreenGradient(6, 66, 18, 0xff)));
		System.out.println(Integer.toHexString(ColorGradientHelper.getRedGreenGradient(6, 66, 36, 0x55)));
		System.out.println(Integer.toHexString(ColorGradientHelper.getRedGreenGradient(6, 66, 54, 0xff)));
		System.out.println(Integer.toHexString(ColorGradientHelper.getRedGreenGradient(6, 66, 66, 0x80)));
		
		System.out.println(ColorGradientHelper.getRedGreenGradientHtml(6, 66, 6));
		System.out.println(ColorGradientHelper.getRedGreenGradientHtml(6, 66, 18));
		System.out.println(ColorGradientHelper.getRedGreenGradientHtml(6, 66, 36));
		System.out.println(ColorGradientHelper.getRedGreenGradientHtml(6, 66, 54));
		System.out.println(ColorGradientHelper.getRedGreenGradientHtml(6, 66, 66));
	}

}
