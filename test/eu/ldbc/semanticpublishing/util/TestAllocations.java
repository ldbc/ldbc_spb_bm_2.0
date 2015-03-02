package eu.ldbc.semanticpublishing.util;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class TestAllocations {

	@Test
	public void testSimple() {
		checkAllocations( new double[] { 0.1, 0.2, 0.3, 0.4 });
	}

	@Test
	public void testOneBand() {
		checkAllocations( new double[] { 1.0 });
	}

	@Test
	public void testVariedBands() {
		checkAllocations( new double[] { 0.0001, 0.001, 0.01, 0.1, 0.8889 });
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoBands() {
		checkAllocations( new double[] {});
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddUpTooSmall() {
		checkAllocations( new double[] {0.5, 0.45});
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAddUpTooBig() {
		checkAllocations( new double[] {0.5, 0.55});
	}

	private void checkAllocations(double[] allocations ) {
		AllocationsUtil d = new AllocationsUtil(allocations, new Random(0));
		int counts[] = new int[allocations.length];
		
		final int LOOP = 100000;
		
		for( int i = 0; i < LOOP; ++i ) {
			int band = d.getAllocation();
			assertTrue( band >= 0);
			assertTrue( band < allocations.length);
			
			++counts[band];
		}
		
		for(int b = 0; b < allocations.length; ++b ) {
			double rate = (1.0 * counts[b]) / LOOP;
			assertTrue( Math.abs(rate - allocations[b]) < 0.01 );
		}
	}
}
