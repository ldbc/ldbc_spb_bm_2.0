package eu.ldbc.semanticpublishing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Exponential decay generator, produces values in the exponential decay form of y = C*exp(-k*t), k > 0
 * Where :
 *    C is decayLimit, or maximum number decay starts from
 *    k is decayRate - values from [0.01..1.0] used to define the slope of the decay
 *    t - variable which in this case is the time
 *    
 * When no more elements can be generated, or threshold is reached a NoSuchElementException exception is thrown
 */
public class ExponentialDecayNumberGeneratorUtil {
	private long step = 0;
	private Long next = null;
	
	private long decayLimit = 1;
	private double decayRate = 0.1;
	private double decayThresholdPercent = 0.05;
	
	public ExponentialDecayNumberGeneratorUtil(long decayLimit, double decayRate, double decayThresholdPercent) {
		this.next = decayLimit;
		this.decayLimit = decayLimit;
		this.decayRate = decayRate;
		this.decayThresholdPercent = decayThresholdPercent;
	}
	
	public long generateNext() throws NoSuchElementException {
		long result = (long) (decayLimit * Math.exp(-decayRate*step));
		next = result;
		step++;
		
		if (thresholdReached()) {
			next = null;
			throw new NoSuchElementException("No more values to generate (or the threshold of " + decayLimit * decayThresholdPercent + " was reached)");
		}
		
		return result;		
	}

	public long getIterationStep() {
		return step;
	}
	
	public boolean hasNext() {
		return next != null;
	}
	
	private double calculateThreshold() {
		return ((double)next / (double)decayLimit);
	}
	
	public boolean thresholdReached() {
		return calculateThreshold() <= decayThresholdPercent;
	}
	
	public List<Long> produceIterationStepsList() {
		List<Long> iterationStepsList = new ArrayList<Long>();
		
		long currentIterationSize = generateNext();
		
		try {
			while (hasNext()) {
				iterationStepsList.add(currentIterationSize);
				currentIterationSize = generateNext();
			}
		} catch (NoSuchElementException nse) {
			//expecting generateNext() to throw it
		} 
		
		return iterationStepsList;
	}
	
	public long calculateTotal() {
		int step = 0;
		long result = 0;
		long next = 0;

		next = (long) (decayLimit * Math.exp(-decayRate*step));
		result += next;						
		step++;		
		
		double c = (double)next / (double)decayLimit;
		
		while (c > decayThresholdPercent) {			
			
			next = (long) (decayLimit * Math.exp(-decayRate*step));
			c = (double)next / (double)decayLimit;
			
			if (c <= decayThresholdPercent) {
				return result;
			}
			result += next;
			step++;
		}
		
		return result;
	}	
}
