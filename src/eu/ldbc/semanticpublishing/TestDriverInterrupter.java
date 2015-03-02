package eu.ldbc.semanticpublishing;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.ldbc.semanticpublishing.util.FileUtils;

/**
 * This class is used to watch and control the TestDriver in cases when benchmark process needs to be interrupted. 
 * Such cases are:
 *   - receiving an interrupt signal from a second benchmark test driver
 */
public class TestDriverInterrupter extends Thread {
	protected final static String BENCHMARK_INTERRUPT_SIGNAL = "benchmark_run_completed";
	private final AtomicBoolean benchmarkState;
	private Thread parentThread;
	private String interruptSignalFilePath;
	
	public TestDriverInterrupter(Thread parentThread, AtomicBoolean benchmarkState, String interruptSignalFilePath) {
		this.parentThread = parentThread;
		this.benchmarkState = benchmarkState;
		this.interruptSignalFilePath = interruptSignalFilePath;
		
		//clear traces from previous runs
		FileUtils.deleteFile(TestDriverInterrupter.BENCHMARK_INTERRUPT_SIGNAL);		
	}
	
	@Override
	public void run() {
		try {
			while (benchmarkState.get()) {
				Thread.sleep(1000);
				
				//check for interrupt signal from another test driver
				if (!interruptSignalFilePath.trim().isEmpty()) {
					if (FileUtils.fileExists(interruptSignalFilePath + File.separator + BENCHMARK_INTERRUPT_SIGNAL)) {
						benchmarkState.set(false);
						System.out.println("*** Interrupt signal has been received (" + interruptSignalFilePath + "), stopping the benchmark run...");
						parentThread.interrupt();
						return;
					}
				}		
			}
		} catch (Throwable t) {
			System.out.println("TestDriverInterrupter :: encountered a problem : " + t.getMessage());
			t.printStackTrace();
		}		
	}
}
