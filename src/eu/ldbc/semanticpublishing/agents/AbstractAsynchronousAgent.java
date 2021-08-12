package eu.ldbc.semanticpublishing.agents;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class for extending Aggregation and Editorial agents.
 *
 */
public abstract class AbstractAsynchronousAgent extends Thread {

	private final AtomicBoolean runFlag;
	
	public AbstractAsynchronousAgent( AtomicBoolean runFlag ) {
		this.runFlag = runFlag;
	}

	@Override
	public void run() {
		while(runFlag.get()) {
			if(! executeLoop() ) {
				break;
			}
		}
		executeFinalize();
	}
	
	/**
	 * This method will be called repeatedly until either runFlag is set to false
	 * or this method returns false 
	 * @return true to be called again, false otherwise
	 */
	public abstract boolean executeLoop();
	
	/**
	 * This method will be called after breaking (or finishing) the execute loop, 
	 * and is be used to close endpoint connections or execute other finalization 
	 * procedures.
	 */
	public abstract void executeFinalize();
}
