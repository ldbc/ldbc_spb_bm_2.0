package eu.ldbc.semanticpublishing.agents;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.resultanalyzers.history.*;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HistoryAgent extends AbstractAsynchronousAgent {

	public static final int MAX_NUMBER_OF_STORED_HISTORY_QUERIES = 1000;

	private BlockingQueue<OriginalQueryData> playedQueriesOriginal;
	private BlockingQueue<OriginalQueryData> playedQueriesCopy = new LinkedBlockingQueue<>();
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;
	public static boolean historyValidationStarted;
	private final boolean logFailedQueriesResults;

	private final static Logger DETAILED_LOGGER = LoggerFactory.getLogger(AggregationAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	public final static Logger FAILED_HISTORY_QUERIES_LOGGER = LoggerFactory.getLogger(HistoryAgent.class.getName());

	public HistoryAgent(AtomicBoolean runFlag, BlockingQueue<OriginalQueryData> playedQueriesQueue,
						SparqlQueryExecuteManager queryExecuteManager, boolean logFailedQueries) {
		super(runFlag);
		this.playedQueriesOriginal = playedQueriesQueue;
		this.queryExecuteManager = queryExecuteManager;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
		this.logFailedQueriesResults = logFailedQueries;
	}

	@Override
	public boolean executeLoop() {
		if (playedQueriesCopy.size() > 0) {
			historyValidationStarted = true;
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			OriginalQueryData headQuery = playedQueriesCopy.poll();
			if (headQuery != null) {
				long start = System.currentTimeMillis();
				String timeStamp = "";
				String historyQuery = "";
				InputStream inputStreamResult = null;
				try {
					timeStamp = format.format(Calendar.getInstance().getTime());
					historyQuery = applyHistoryGraphToQuery(headQuery.getOriginalQueryString(), format.parse(headQuery.getTimeStamp()).getTime());
					inputStreamResult = queryExecuteManager.executeQueryWithInputStreamResult(connection, headQuery.getOriginalQueryName(),
							historyQuery, headQuery.getOriginalQueryType(), false, false);
					updateHistoryStatistics(true, headQuery, inputStreamResult, timeStamp, historyQuery, System.currentTimeMillis() - start);
				} catch (Throwable t) {
					String msg = "WARNING : HistoryAgent [" + Thread.currentThread().getName() + "] reports: " + t.getMessage() + "\n" + "\tfor query : \n" + historyQuery + "\n...closing current connection and creating a new one..." + "\n----------------------------------------------------------------------------------------------\n";

					System.out.println(msg);

					DETAILED_LOGGER.warn(msg);
					updateHistoryStatistics(false, headQuery, inputStreamResult, timeStamp, historyQuery, System.currentTimeMillis() - start);

					connection.disconnect();
					connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
				}
			}
		}

		if (playedQueriesCopy.size() == 0 && playedQueriesOriginal.size() == MAX_NUMBER_OF_STORED_HISTORY_QUERIES) {
			playedQueriesOriginal.drainTo(playedQueriesCopy);
		}

		return true;
	}

	@Override
	public void executeFinalize() {
		connection.disconnect();
	}

	private void updateHistoryStatistics(boolean reportSuccess, OriginalQueryData headQuery,
										 InputStream inputStreamResult, String timeStamp,
										 String historyQuery, long queryExecutionTimeMs) {
		Model resultAsModel = null;
		List<BindingSet> bindingSetList = null;
		int resultsCount = 0;
		int resultsOrigQueryCount = headQuery.getOriginalQueryType() == SparqlQueryConnection.QueryType.SELECT ?
				((SavedAsBindingSetListOriginalResults) headQuery).getSavedBindingSets().size() :
				((SavedAsModelOriginalResults) headQuery).getSavedModel().size();
		boolean areResultsEqual = false;

		if (reportSuccess) {
			if (headQuery.getOriginalQueryType() == SparqlQueryConnection.QueryType.SELECT) {
				bindingSetList = QueryResultsConverterUtil.getBindingSetsList(inputStreamResult);
				assert headQuery instanceof SavedAsBindingSetListOriginalResults;
				List<BindingSet> origBindingSetList = ((SavedAsBindingSetListOriginalResults) headQuery).getSavedBindingSets();
				if (CollectionUtils.isEqualCollection(origBindingSetList, bindingSetList)) {
					areResultsEqual = true;
				}
				resultsCount = bindingSetList.size();
			} else {
				resultAsModel = QueryResultsConverterUtil.getReturnedResultAsModel(inputStreamResult);
				assert headQuery instanceof SavedAsModelOriginalResults;
				Model origResultAsModel = ((SavedAsModelOriginalResults) headQuery).getSavedModel();
				if (origResultAsModel.equals(resultAsModel)) {
					areResultsEqual = true;
				}
				resultsCount = resultAsModel.size();
			}

			Statistics.historyTimeCorrectionsMS.addAndGet(queryExecutionTimeMs);
		}

		int queryNumber = getQueryNumber(headQuery.getOriginalQueryName());
		if (reportSuccess && areResultsEqual) {
			Statistics.historyQueriesArray[HistoryQueriesUtils.getIndexFromQueryNum(queryNumber)].reportSuccess(queryExecutionTimeMs);
			Statistics.historyAggregateQueryStatistics.reportSuccess(queryExecutionTimeMs);
			BRIEF_LOGGER.info(String.format("\t%s:\t[%s, %s] Query executed from history plugin, execution time : %d ms, results : %d",
					timeStamp, queryNumber, Thread.currentThread().getName(), queryExecutionTimeMs, resultsCount));
		} else {
			Statistics.historyQueriesArray[HistoryQueriesUtils.getIndexFromQueryNum(queryNumber)].reportFailure();
			Statistics.historyAggregateQueryStatistics.reportFailure();
			BRIEF_LOGGER.info(String.format("\t%s:\t[%s, %s] Query executed from history plugin, execution time : %d ms, previous results: %d, results from history plugin: %d %s",
					timeStamp, queryNumber, Thread.currentThread().getName(), queryExecutionTimeMs, resultsOrigQueryCount, resultsCount, ", query error!"));
			// ReportSuccess check is needed because if it false there won't be results from history plugin
			if (reportSuccess && logFailedQueriesResults) {
				FAILED_HISTORY_QUERIES_LOGGER.info(String.format("Query %s results from first run", headQuery.getOriginalQueryName()));
				if (headQuery.getOriginalQueryType() == SparqlQueryConnection.QueryType.SELECT) {
					QueryResultsConverterUtil.writeBindingSetToLogger(((SavedAsBindingSetListOriginalResults) headQuery).getSavedBindingSets());
					FAILED_HISTORY_QUERIES_LOGGER.info(String.format("Query %s results from history plugin", headQuery.getOriginalQueryName()));
					QueryResultsConverterUtil.writeBindingSetToLogger(bindingSetList);
				} else {
					QueryResultsConverterUtil.writeModelResultsToLogger(((SavedAsModelOriginalResults) headQuery).getSavedModel());
					FAILED_HISTORY_QUERIES_LOGGER.info(String.format("Query %s results from history plugin", headQuery.getOriginalQueryName()));
					QueryResultsConverterUtil.writeModelResultsToLogger(resultAsModel);
				}
			}
		}

		DETAILED_LOGGER.info("\n*** Query [" + headQuery.getOriginalQueryName() + "], execution time : " + timeStamp + " (" + queryExecutionTimeMs + " ms), results : " + resultsCount + "\n" + historyQuery + "\n");
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
}
