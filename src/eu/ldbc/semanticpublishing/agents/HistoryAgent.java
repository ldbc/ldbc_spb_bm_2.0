package eu.ldbc.semanticpublishing.agents;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.resultanalyzers.history.OriginalQueryData;
import eu.ldbc.semanticpublishing.resultanalyzers.history.QueryResultsConverterUtil;
import eu.ldbc.semanticpublishing.resultanalyzers.history.SavedAsBindingSetListOriginalResults;
import eu.ldbc.semanticpublishing.resultanalyzers.history.SavedAsModelOriginalResults;
import eu.ldbc.semanticpublishing.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HistoryAgent extends AbstractAsynchronousAgent {

	private BlockingQueue<OriginalQueryData> playedQueriesQueue;
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;

	public HistoryAgent(AtomicBoolean runFlag, BlockingQueue<OriginalQueryData> playedQueriesQueue,
						SparqlQueryExecuteManager queryExecuteManager) {
		super(runFlag);
		this.playedQueriesQueue = playedQueriesQueue;
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
		if (playedQueriesQueue.size() > 1000) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			OriginalQueryData headQuery = playedQueriesQueue.poll();
			if (headQuery != null) {
				try {
					long timeStamp = format.parse(headQuery.getTimeStamp()).getTime();
					String result = applyHistoryGraphToQuery(headQuery.getOriginalQueryString(), timeStamp);
					long start = System.currentTimeMillis();
					InputStream inputStreamResult = queryExecuteManager.executeQueryWithInputStreamResult(connection, headQuery.getOriginalQueryName(),
							result, headQuery.getOriginalQueryType(), false, false);
					long executionTime = System.currentTimeMillis() - start;
					if (headQuery.getOriginalQueryType() == SparqlQueryConnection.QueryType.SELECT) {
						if (!((SavedAsBindingSetListOriginalResults) headQuery).getSavedBindingSets()
								.equals(QueryResultsConverterUtil.getBindingSetsList(inputStreamResult))) {
							System.err.println(generateErrorMsg(headQuery));
						} else {
							System.out.println("Executed query " + headQuery.getOriginalQueryName() + " for " + executionTime + "ms");
						}
					} else {
						if (!((SavedAsModelOriginalResults) headQuery).getSavedModel()
								.equals(QueryResultsConverterUtil.getReturnedResultAsModel(inputStreamResult))) {
							System.err.println(generateErrorMsg(headQuery));
						} else {
							System.out.println("Executed query " + headQuery.getOriginalQueryName() + " for " + executionTime + "ms");
						}
					}
				} catch (IOException e) {
					System.err.println("Exception occurred during query execution\n" + e.getMessage());
				} catch (ParseException e) {
					System.err.println("Couldn't parse properly timestamp");
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

	private String generateErrorMsg(OriginalQueryData headQuery) {
		return "Found difference in returned results from history plugin for " +
				headQuery.getOriginalQueryName() + " queryType " + headQuery.getOriginalQueryType().toString();
	}
}
