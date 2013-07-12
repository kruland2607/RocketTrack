package org.rockettrack.util;

public class ExponentialAverage {
	private float alpha;

	public ExponentialAverage(float alpha) {
		this.alpha = alpha;
	}

	public float[] average(float[] input, float[] previous) {

		for( int i=0; i< input.length; i++ ) {
			previous[i] = previous[i] + alpha* (input[i] - previous[i]);
		}

		return previous;
	}
}	
