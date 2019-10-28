package eu.ldbc.semanticpublishing.statistics;

import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
	private static final String INSERT_QUERIES_STATISTICS = "INSERT";
	private static final String UPDATE_QUERIES_STATISTICS = "UPDATE";
	private static final String DELETE_QUERIES_STATISTICS = "DELETE";
	private static final String AGGREGATE_QUERIES_STATISTICS = "AGGREGATE";
	private static final String HISTORY_QUERIES_STATISTICS = "HISTORY_AGGREGATE";

	public static final int AGGREGATE_QUERIES_COUNT = 12;
	public static final int EDITORIAL_QUERIES_COUNT = 3;
	public static final int HISTORY_QUERIES_COUNT = 8;

	public static final String AGGREGATE_QUERY_NAME = "query";
	public static final QueryStatistics[] aggregateQueriesArray;
	public static final QueryStatistics[] historyQueriesArray;

	public static final AtomicLong timeCorrectionsMS = new AtomicLong(0);
	public static final AtomicLong historyTimeCorrectionsMS = new AtomicLong(0);
	public static final AtomicLong totalStartedQueryMixRuns = new AtomicLong(0);
	public static final AtomicLong totalCompletedQueryMixRuns = new AtomicLong(0);

	static {
		aggregateQueriesArray = new QueryStatistics[AGGREGATE_QUERIES_COUNT];
		historyQueriesArray = new QueryStatistics[HISTORY_QUERIES_COUNT];

		for (int i = 0; i < AGGREGATE_QUERIES_COUNT; i++) {
			aggregateQueriesArray[i] = new QueryStatistics(AGGREGATE_QUERIES_STATISTICS + "_" + (i + 1));
		}
		for (int i = 0; i < HISTORY_QUERIES_COUNT - 3; i++) {
			historyQueriesArray[i] = new QueryStatistics(HISTORY_QUERIES_STATISTICS + "_" + (i + 1));
		}
		// Not all queries could be executed against history plugin
		historyQueriesArray[5] = new QueryStatistics(HISTORY_QUERIES_STATISTICS + "_" + 7);
		historyQueriesArray[6] = new QueryStatistics(HISTORY_QUERIES_STATISTICS + "_" + 9);
		historyQueriesArray[7] = new QueryStatistics(HISTORY_QUERIES_STATISTICS + "_" + 11);
	}

	//section for keeping statistics for each executed query type
	public static final QueryStatistics insertCreativeWorksQueryStatistics = new QueryStatistics(INSERT_QUERIES_STATISTICS);
	public static final QueryStatistics updateCreativeWorksQueryStatistics = new QueryStatistics(UPDATE_QUERIES_STATISTICS);
	public static final QueryStatistics deleteCreativeWorksQueryStatistics = new QueryStatistics(DELETE_QUERIES_STATISTICS);
	public static final QueryStatistics totalAggregateQueryStatistics = new QueryStatistics(AGGREGATE_QUERIES_STATISTICS);
	public static final QueryStatistics historyAggregateQueryStatistics = new QueryStatistics(HISTORY_QUERIES_STATISTICS);
}
