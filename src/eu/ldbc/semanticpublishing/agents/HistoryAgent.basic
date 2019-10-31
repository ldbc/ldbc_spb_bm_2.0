package eu.ldbc.semanticpublishing.agents;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.resultanalyzers.history.OriginalQueryData;
import eu.ldbc.semanticpublishing.resultanalyzers.history.QueryResultsConverterUtil;
import eu.ldbc.semanticpublishing.resultanalyzers.history.SavedAsBindingSetListOriginalResults;
import eu.ldbc.semanticpublishing.resultanalyzers.history.SavedAsModelOriginalResults;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HistoryAgent extends AbstractAsynchronousAgent {

	private BlockingQueue<OriginalQueryData> playedQueriesCopy;
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;
	public static boolean historyValidationStarted;

	private final static Logger DETAILED_LOGGER = LoggerFactory.getLogger(AggregationAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());

	public HistoryAgent(AtomicBoolean runFlag, BlockingQueue<OriginalQueryData> playedQueriesQueue,
						SparqlQueryExecuteManager queryExecuteManager) {
		super(runFlag);
		this.playedQueriesCopy = playedQueriesQueue;
		this.queryExecuteManager = queryExecuteManager;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
	}

	@Override
	public boolean executeLoop() {
		return validateHistoryPlugin();
	}

	@Override
	public void executeFinalize() {
		connection.disconnect();
	}

	private boolean validateHistoryPlugin() {
		if (playedQueriesCopy.size() > 1000) {
			historyValidationStarted = true;
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			OriginalQueryData headQuery = playedQueriesCopy.poll();
			if (headQuery != null) {
				try {
					long resultsCount = 0;
					String timeStamp = format.format(Calendar.getInstance().getTime());
					String historyQuery = applyHistoryGraphToQuery(headQuery.getOriginalQueryString(), format.parse(headQuery.getTimeStamp()).getTime());
					long start = System.currentTimeMillis();
					InputStream inputStreamResult = queryExecuteManager.executeQueryWithInputStreamResult(connection, headQuery.getOriginalQueryName(),
							historyQuery, headQuery.getOriginalQueryType(), false, false);
					long queryExecutionTimeMs = System.currentTimeMillis() - start;
					Statistics.historyTimeCorrectionsMS.addAndGet(queryExecutionTimeMs);
					boolean reportSuccess = false;
					if (headQuery.getOriginalQueryType() == SparqlQueryConnection.QueryType.SELECT) {
						List<BindingSet> bindingSetList = QueryResultsConverterUtil.getBindingSetsList(inputStreamResult);
						if (((SavedAsBindingSetListOriginalResults) headQuery).getSavedBindingSets()
								.equals(bindingSetList)) {
							resultsCount = bindingSetList.size();
							reportSuccess = true;
						}
					} else {
						Model resultAsModel = QueryResultsConverterUtil.getReturnedResultAsModel(inputStreamResult);
						if (((SavedAsModelOriginalResults) headQuery).getSavedModel()
								.equals(resultAsModel)) {
							resultsCount = resultAsModel.size();
							reportSuccess = true;
						}
					}

					int queryNumber = getQueryNumber(headQuery.getOriginalQueryName());
					if (reportSuccess) {
						Statistics.historyQueriesArray[mapQueryNumber(queryNumber)].reportSuccess(queryExecutionTimeMs);
						Statistics.historyAggregateQueryStatistics.reportSuccess(queryExecutionTimeMs);
						BRIEF_LOGGER.info(String.format("\t%s:\t[%s, %s] Query executed from history plugin, execution time : %d ms, results : %d",
								timeStamp, queryNumber, Thread.currentThread().getName(), queryExecutionTimeMs, resultsCount));
						DETAILED_LOGGER.info("\n*** Query [" + headQuery.getOriginalQueryName() + "], execution time : " + timeStamp + " (" + queryExecutionTimeMs + " ms), results : " + resultsCount + "\n" + historyQuery + "\n");
					} else {
						Statistics.historyQueriesArray[mapQueryNumber(queryNumber)].reportFailure();
						Statistics.historyAggregateQueryStatistics.reportFailure();
						BRIEF_LOGGER.info(String.format("\t%s:\t[%s, %s] Query executed from history plugin, execution time : %d ms, results : %d %s", timeStamp, queryNumber, Thread.currentThread().getName(), queryExecutionTimeMs, resultsCount, ", query error!"));
					}
				} catch (IOException e) {
					BRIEF_LOGGER.error("Exception occurred during query execution\n" + e.getMessage());
					DETAILED_LOGGER.error("Exception occurred during query execution\n" + e.getMessage());
				} catch (ParseException e) {
					BRIEF_LOGGER.error("Couldn't parse properly timestamp");
					DETAILED_LOGGER.error("Couldn't parse properly timestamp");
				}
			}
		}

		return true;
	}

	private String applyHistoryGraphToQuery(String queryString, long timestampAsLong) {
		String dis = "ot:disable-sameAs";
		int index = queryString.indexOf(dis) + dis.length();
		StringBuilder sb = new StringBuilder(queryString);
		sb.insert(index, " FROM <http://www.ontotext.com/at/" + convertTimestamp(timestampAsLong) + ">");
		return sb.toString();
	}

	private String convertTimestamp(long timeStamp) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.systemDefault()).format(dtf);
	}

	private int getQueryNumber(String queryName) {
		return Integer.parseInt(queryName.substring(queryName.indexOf(Statistics.AGGREGATE_QUERY_NAME) + Statistics.AGGREGATE_QUERY_NAME.length(), queryName.indexOf(".")));
	}

	private int mapQueryNumber(int queryNumber) {
		switch (queryNumber) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
				return queryNumber - 1;
			case 7:
				return 5;
			case 9:
				return 6;
			case 11:
				return 7;
				default:
					throw new IllegalArgumentException("Query with " + queryNumber + " should not be added for history validation");
		}
	}
}
