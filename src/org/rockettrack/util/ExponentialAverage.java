package org.rockettrack.util;

public class ExponentialAverage {
	private float alpha;
	private boolean isPrimed;
	private float[] current;

	public ExponentialAverage(int dimensions, float alpha) {
		this.alpha = alpha;
		this.isPrimed = false;
		current = new float[dimensions];
	}

	public float[] average(float[] input) {

		if ( isPrimed ) {
			for( int i=0; i< input.length; i++ ) {
				current[i] = current[i] + alpha* (input[i] - current[i]);
			}
		}
		else {
			isPrimed = true;
			for( int i=0; i<current.length; i++ ) {
				current[i] = input[i];
			}
		}

		return current;
	}
}	
