package eu.ldbc.semanticpublishing.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A class for holding individual statistics for each query type.
 */
public class QueryStatistics {
	private String queryName;
	private AtomicLong queryId;
	private AtomicLong runsCount;
	private AtomicLong failuresCount;
	
	private long minExecutionTimeMs = 0;
	private long maxExecutionTimeMs = 0;
	private long avgExecutionTimeMs = 0;

	public QueryStatistics(String queryName) {
		this.queryName = queryName;
		queryId = new AtomicLong(0);
		runsCount = new AtomicLong(0);
		failuresCount = new AtomicLong(0);
	}
	
	public synchronized void reportSuccess(long currentExecutionTimeMs) {
		runsCount.incrementAndGet();
		minExecutionTimeMs = (minExecutionTimeMs == 0) ? currentExecutionTimeMs : (minExecutionTimeMs > currentExecutionTimeMs ? currentExecutionTimeMs : minExecutionTimeMs);
		maxExecutionTimeMs = (currentExecutionTimeMs > maxExecutionTimeMs) ? currentExecutionTimeMs : maxExecutionTimeMs;
		avgExecutionTimeMs += currentExecutionTimeMs;
	}
	
	public void reportFailure() {
		failuresCount.incrementAndGet();
	}
	
	public String getQueryName() {
		return queryName;
	}
	
	public long getRunsCount() {
		return runsCount.get();
	}
	
	public long getFailuresCount() {
		return failuresCount.get();
	}	
	
	public long getMinExecutionTimeMs() {
		return minExecutionTimeMs;
	}
	
	public long getMaxExecutionTimeMs() {
		return maxExecutionTimeMs;
	}
	
	public long getAvgExecutionTimeMs() {
		if (runsCount.get() == 0) {
			return 0;
		}
		return avgExecutionTimeMs / runsCount.get();
	}
	
	public long getNewQueryId() {
		return queryId.getAndIncrement();
	}
	
	public AtomicLong getRunsCountAtomicLong() {
		return runsCount;
	}
}
