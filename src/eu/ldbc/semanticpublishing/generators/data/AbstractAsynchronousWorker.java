package eu.ldbc.semanticpublishing.generators.data;

/**
 * Abstract class for extending Workers for the DataGenerator.
 */
public abstract class AbstractAsynchronousWorker extends Thread {
	
	protected static final String FILENAME_FORMAT = "%s%sgeneratedCreativeWorks-%06d.";
	
	@Override
	public void run() {
		try {
			execute();
		} catch (Exception e) {
			System.out.println("Exception caught by : " + Thread.currentThread().getName() + " : " + e.getMessage());
			e.printStackTrace();
		}
	}	
	
	/**
	 * This method will be called for execution of a concrete task
	 */
	public abstract void execute() throws Exception;
}
