package eu.ldbc.semanticpublishing.util;

/**
 * A class used to calculate the average of series of double values.
 */
public class AveragesUtil {
	private long counter = 0;
	private double avgValue = 0.0;
	
	public synchronized void addValue(double value) {
		avgValue += value;
		counter++;
	}
	
	public synchronized double getAvgValue() {
		if (counter == 0) {
			return 0.0;
		}
		return (avgValue / (double)counter);
	}
	
	public synchronized void reset() {
		avgValue = 0.0;
		counter = 0;
	}
}
