package eu.ldbc.semanticpublishing.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestRandomUtil {

	@Test
	public void testNextInt_IntInt() {
		RandomUtil rg = new RandomUtil("./data/WordsDictionary.txt", 0, 2000, 1);
		
		checkNextInt_IntInt(rg, 0, 2);
		checkNextInt_IntInt(rg, -10, 10);
		checkNextInt_IntInt(rg, 2000, 2013);
		checkNextInt_IntInt(rg, Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
		checkNextInt_IntInt(rg, -Integer.MIN_VALUE, Integer.MIN_VALUE + 10);
		checkNextInt_IntInt(rg, 0, 1);
	}

	private void checkNextInt_IntInt(RandomUtil rg, int min, int max) {
		for(int i = 0; i < 10000; ++i) {
			int value = rg.nextInt(min, max);
			assertTrue(value >= min);
			assertTrue(value < max);
		}
	}

	@Test
	public void testNextLong() {
		RandomUtil rg = new RandomUtil("./data/WordsDictionary.txt", 0, 2000, 1);

		checkNextLong(rg, 1);
		checkNextLong(rg, 2);
		checkNextLong(rg, 3);
		checkNextLong(rg, Long.MAX_VALUE/2);
		checkNextLong(rg, Long.MAX_VALUE);
	}

	private void checkNextLong(RandomUtil rg, long max) {
		for(int i = 0; i < 10000; ++i) {
			long value = rg.nextLong(max);
			assertTrue(value >= 0);
			assertTrue(value < max);
		}
	}

}
