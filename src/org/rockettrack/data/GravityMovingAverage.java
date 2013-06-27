package org.rockettrack.data;

import java.util.Arrays;
import java.util.LinkedList;

public class GravityMovingAverage 
{
	private LinkedList<float[]> mValues = new LinkedList<float[]>();
	private int mWindowSize = 5;
	private boolean mUseWeightedAverage = true;

	/**
	 * 
	 */
	public GravityMovingAverage()
	{
	}
	
	/**
	 * 
	 * @param values
	 * @return
	 */
	public float[] add(float[] values)
	{
		// Limit the queue to X values
		if (this.mValues.size() == this.mWindowSize)
			this.mValues.removeFirst();

		this.mValues.addLast(values);
		final int valuesSize = this.mValues.size();

		if (valuesSize == 1)
		{
			return values;
		}
		else
		{
			final int valueLength = values.length;
			float[] result = new float[valueLength];
	
			float sumWeight = 0;
			int counter = 1;
			for (float[] value : this.mValues)
			{
				float weight = 1;
				if (this.mUseWeightedAverage)
					weight = (float) counter / valuesSize;
				for (int i = 0; i<valueLength; i++)
				{
					result[i] = result[i] + value[i] * weight;
				}
				sumWeight += weight;
				counter++;
			}
			
			for (int i = 0; i < valueLength; i++)
			{
				result[i] = result[i] / sumWeight;
			}
	
			return result;
		}
	}

	/**
	 * 
	 */
	public void clear()
	{
		this.mValues.clear();
	}
	
	/**
	 * 
	 * @param windowSize
	 */
	public void setWindowSize(int windowSize)
	{
		this.mWindowSize = windowSize;
	}
	
	/**
	 * 
	 * @param useWeightedAverage
	 */
	public void setUseWeightedAverage(boolean useWeightedAverage)
	{
		this.mUseWeightedAverage = useWeightedAverage;
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{
		final boolean useWeightedAverage = false;
		final int windowSize = 3;
		
		final GravityMovingAverage ma = new GravityMovingAverage();
		ma.setWindowSize(windowSize);
		ma.setUseWeightedAverage(useWeightedAverage);
		
		System.out.println("Window size: " + windowSize);
		System.out.println("Use weighted average: " + useWeightedAverage);

		float[] in1 = { 1, 1, 340 }; 
		System.out.println(Arrays.toString(in1) + " : " + Arrays.toString(ma.add(in1)));
		float[] in2 = { 2, 2, 357 }; 
		System.out.println(Arrays.toString(in2) + " : " + Arrays.toString(ma.add(in2)));
		float[] in3 = { 2, 3, 355 }; 
		System.out.println(Arrays.toString(in3) + " : " + Arrays.toString(ma.add(in3)));
		float[] in4 = { 2, 4, 359 }; 
		System.out.println(Arrays.toString(in4) + " : " + Arrays.toString(ma.add(in4)));
	}
}
