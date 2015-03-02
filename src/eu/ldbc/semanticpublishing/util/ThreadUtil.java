package eu.ldbc.semanticpublishing.util;

/**
 * A utility class for thread related actions. 
 */
public class ThreadUtil {
	public static void sleepMilliseconds(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			
		}
	}
	
	public static void sleepSeconds(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
		}
	}
	
	public static void join( Thread thread ) {
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
	}
}
