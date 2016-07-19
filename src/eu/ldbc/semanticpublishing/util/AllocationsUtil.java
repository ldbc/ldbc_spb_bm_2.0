package eu.ldbc.semanticpublishing.util;

import java.util.Random;

/**
 * Randomly allocate instances to specific bands of probability
 */
public class AllocationsUtil {
	// Don't need to store allocations, but might be useful for debugging
	private final double[] limits;

	private Random random;

	/**
	 * Construct an allocator. For example, to randomly allocate 10% of objects
	 * to band 0, 35% to band 1 and the rest to band 2, initialise with these
	 * values: allocations = { 0.1, 0.35, 0.55 }
	 * 
	 * @param allocations
	 *            A range of probability values, where each value is 0 <= v <=
	 *            1.0 and the sum of all values must be exactly 1.0
	 */
	public AllocationsUtil(double[] allocations, Random random) {
		this.limits = new double[allocations.length];
		this.random = random;
		calculateLimitsFromAllocations(allocations);
	}

	private void calculateLimitsFromAllocations(double[] allocations) {
		double limitsRange = 0.0;
		double allocationsSum = 0.0;
		for (int i = 0; i < allocations.length; i++) {
			limitsRange += allocations[i];
			limits[i] = limitsRange;

			allocationsSum += allocations[i];
		}
		if (Math.abs(1.0 - allocationsSum) > 0.001) {
			throw new IllegalArgumentException(
					String.format("WARNING : Sum of all allocation probabilities is not equal to 1.0, but equals: %.2f, check file definitions.properties", allocationsSum));
		}
	}

	/**
	 * Get the next allocation band, randomly selected, but distributed (on
	 * average) with the allocation weights.
	 * 
	 * @return The allocation band
	 */
	public int getAllocation() {
		double d = random.nextDouble();
		for (int i = 0; i < limits.length; i++) {
			if (d < limits[i]) {
				return i;
			}
		}
		return limits.length;
	}
	
	public void setRandom(Random random) {
		this.random = random;
	}
	
	public double[] getAllocationsArray() {
		return this.limits;
	}
}
