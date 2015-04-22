package eu.ldbc.semanticpublishing.agents;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParametersManager;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SPARQLResultStatementsCounter;
import eu.ldbc.semanticpublishing.resultanalyzers.sesame.TurtleResultStatementsCounter;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.statistics.querypool.Pool;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.templates.aggregation.*;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class that represents an aggregation agent. It executes aggregation queries 
 * in a loop, updates query execution statistics.
 *
 * WARNING : after making changes to this class, make sure you've made a copy of it with corresponding .basic / .advanced extension before building, 
 *           otherwise you will lose your changes! 
 */
public class AggregationAgent extends AbstractAsynchronousAgent {
	private final SparqlQueryExecuteManager queryExecuteManager;
	private final RandomUtil ru;
	private final AtomicBoolean benchmarkingState;
	private final HashMap<String, String> queryTemplates;
	private final Pool queryMixPool;
	private final long benchmarkByQueryMixRuns;
	private SparqlQueryConnection connection;
	private Definitions definitions;
	private SubstitutionQueryParametersManager substitutionQueryParametersMngr;
	private TurtleResultStatementsCounter turtleResultStatementsCounter;
	private SPARQLResultStatementsCounter sparqlResultStatementsCounter;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(AggregationAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
//	private final static int MAX_DRILL_DOWN_ITERATIONS = 5;
	
	public AggregationAgent(AtomicBoolean benchmarkingState, SparqlQueryExecuteManager queryExecuteManager, RandomUtil ru, AtomicBoolean runFlag, HashMap<String, String> queryTamplates, Definitions definitions, SubstitutionQueryParametersManager substitutionQueryParametersMngr, long benchmarkByQueryMixRuns) {
		super(runFlag);
		this.queryExecuteManager = queryExecuteManager;
		this.ru = ru;
		this.benchmarkingState = benchmarkingState;
		this.queryTemplates = queryTamplates;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), queryExecuteManager.getTimeoutMilliseconds(), true);
		this.definitions = definitions;
		this.substitutionQueryParametersMngr = substitutionQueryParametersMngr;
		this.turtleResultStatementsCounter = new TurtleResultStatementsCounter();
		this.sparqlResultStatementsCounter = new SPARQLResultStatementsCounter();
		this.queryMixPool = new Pool(definitions.getString(Definitions.QUERY_POOLS), Statistics.totalStartedQueryMixRuns, Statistics.totalCompletedQueryMixRuns);
		this.benchmarkByQueryMixRuns = benchmarkByQueryMixRuns;
	}
	
	@Override
	public boolean executeLoop() {
		//remember if query was executed before benchmark phase start to skip it later when updating query statistics. No need to do that for Editorial Agents.
		boolean startedDuringBenchmarkPhase = benchmarkingState.get();

		//retrieve next query to be executed from the aggregation query mix
		int aggregateQueryIndex = Definitions.aggregationOperationsAllocation.getAllocation();
		
		if (startedDuringBenchmarkPhase && queryMixPool.getItemsCount() > 0) {
		    if (benchmarkByQueryMixRuns > 0 && !queryMixPool.getInProgress() && Statistics.totalStartedQueryMixRuns.get() >= benchmarkByQueryMixRuns) {
		        return true;
		    }
		
		    //aggregateQueryIndex is ZERO based, while query ids in definitions.properties (parameter queryPools) are not
		    if (!queryMixPool.checkAndSetItemUnavailable(aggregateQueryIndex + 1)) {
		        return true;
		    }		
		}
		
		long queryId = 0;
		MustacheTemplate aggregateQuery = null;
		String queryString = "";
		String queryResult = "";

		try {
//			boolean drillDownQuery = false;
			
			//important : queryDistribution is zero-based, while QueryNTemplate is not!
			queryId = Statistics.aggregateQueriesArray[aggregateQueryIndex].getNewQueryId();
			
			String[] querySubstParameters;
			switch (aggregateQueryIndex) {
				case 0 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query1Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 1 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query2Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 2 : 
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query3Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 3 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query4Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 4 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query5Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 5 : 
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query6Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 6 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query7Template(ru, queryTemplates, definitions, querySubstParameters);
					break;			
				case 7 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query8Template(ru, queryTemplates, definitions, querySubstParameters);
					break;	
				case 8 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query9Template(ru, queryTemplates, definitions, querySubstParameters);
					break;
				case 9 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query10Template(ru, queryTemplates, definitions, querySubstParameters);
					break;		
				case 10 :
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query11Template(ru, queryTemplates, definitions, querySubstParameters);
					break;		
				case 11 : 
					querySubstParameters = substitutionQueryParametersMngr.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.AGGREGATE, aggregateQueryIndex).get(queryId);
					aggregateQuery = new Query12Template(ru, queryTemplates, definitions, querySubstParameters);
					break;	
			}
			
			queryString = aggregateQuery.compileMustacheTemplate();
			
			long executionTimeMs = System.currentTimeMillis();
			
			queryResult = queryExecuteManager.executeQuery(connection, aggregateQuery.getTemplateFileName(), queryString, aggregateQuery.getTemplateQueryType(), true, false);			
			
			updateQueryStatistics(true, startedDuringBenchmarkPhase, aggregateQuery.getTemplateQueryType(), aggregateQuery.getTemplateFileName(), queryString, queryResult, queryId, System.currentTimeMillis() - executionTimeMs);

		} catch (IOException ioe) {
			String msg = "Warning : AggregationAgent : IOException caught : " + ioe.getMessage() + ", attempting a new connection" + "\n" + "\tfor query : \n" + queryString;
			
			System.out.println(msg);
			
			LOGGER.warn(msg);
			
			updateQueryStatistics(false, startedDuringBenchmarkPhase, aggregateQuery.getTemplateQueryType(), aggregateQuery.getTemplateFileName(), queryString, queryResult, queryId, 0);
			
			connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), queryExecuteManager.getTimeoutMilliseconds(), true);
			
		}

		if (startedDuringBenchmarkPhase) {
        	queryMixPool.releaseUnavailableItem(aggregateQueryIndex + 1);
        }
		
		return true;
	}

	@Override
	public void executeFinalize() {				
		connection.disconnect();
	}
	
	private void updateQueryStatistics(boolean reportSuccess, boolean startedDuringBenchmarkPhase, QueryType queryType, String queryName, String queryString, String queryResult, long id, long queryExecutionTimeMs) {
		//skip update of statistics for conformance queries
		if (queryName.startsWith("#")) {
			return;
		}
		
		int queryNumber = getQueryNumber(queryName);
		String queryNameId = constructQueryNameId(queryName, id);
		
		//count results (statements)
		long resultsCount = 0;
		InputStream iStream = null;
				
		try {			
			if ((!queryResult.trim().isEmpty())) {			
            iStream = new ByteArrayInputStream(queryResult.getBytes("UTF-8"));
		        if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
		          resultsCount = turtleResultStatementsCounter.getStatementsCount(iStream);
		          Statistics.timeCorrectionsMS.addAndGet(turtleResultStatementsCounter.getParseTime());
		        } else {
		          resultsCount = sparqlResultStatementsCounter.getStatementsCount(iStream);
		          Statistics.timeCorrectionsMS.addAndGet(sparqlResultStatementsCounter.getParseTime());
		        }
			}
			
			if (queryResult.length() >= 0 && benchmarkingState.get()) {
				if (startedDuringBenchmarkPhase) {
					if (reportSuccess) {
						Statistics.aggregateQueriesArray[queryNumber - 1].reportSuccess(queryExecutionTimeMs);
						Statistics.totalAggregateQueryStatistics.reportSuccess(queryExecutionTimeMs);
						logBrief(queryNameId, queryType, queryResult, "", queryExecutionTimeMs, resultsCount);
					} else {				
						Statistics.aggregateQueriesArray[queryNumber - 1].reportFailure();
						Statistics.totalAggregateQueryStatistics.reportFailure();
						logBrief(queryNameId, queryType, queryResult, ", query error!", queryExecutionTimeMs, resultsCount);
					}
				} else {
					if (queryExecutionTimeMs > 0) {
						LOGGER.info("\tQuery : " + queryName + ", time : " + queryExecutionTimeMs + " ms, queryResult.length : " + queryResult.length() + ", results : " + resultsCount + ", has been started during the warmup phase, it will be ignored in the benchmark result!");
						logBrief(queryNameId, queryType, queryResult, ", has been started during the warmup phase, it will be ignored in the benchmark result!", queryExecutionTimeMs, resultsCount);
					} else {
						LOGGER.warn("\tQuery : " + queryName + ", time : " + queryExecutionTimeMs + " ms, queryResult.length : " + queryResult.length() + ", results : " + resultsCount + ", has failed to execute... possibly query timeout has been reached!");					
						logBrief(queryNameId, queryType, queryResult, ", has failed to execute... possibly query timeout has been reached!", queryExecutionTimeMs, resultsCount);
					}
				}
			}
			
			LOGGER.info("\n*** Query [" + queryNameId + "], execution time : " + queryExecutionTimeMs + " ms, results : " + resultsCount + "\n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryNameId + "]" + " : \n" + "Length : " + queryResult.length() + "\n" + queryResult + "\n\n");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private void logBrief(String queryId, QueryType queryType, String queryResult, String appendString, long queryExecutionTimeMs, long resultStatementsCount) {
		StringBuilder reportSb = new StringBuilder();
		reportSb.append(String.format("\t[%s, %s] Query executed, execution time : %d ms, results : %d %s", queryId, Thread.currentThread().getName(), queryExecutionTimeMs, resultStatementsCount, appendString));
//		if (queryType == QueryType.SELECT || queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
//			reportSb.append(", characters returned : " + queryResult.length());
//		}
		
		BRIEF_LOGGER.info(reportSb.toString());		
	}
	
	private int getQueryNumber(String queryName) {
		return Integer.parseInt(queryName.substring(queryName.indexOf(Statistics.AGGREGATE_QUERY_NAME) + Statistics.AGGREGATE_QUERY_NAME.length(), queryName.indexOf(".")));
	}
	
	private String constructQueryNameId(String queryName, long id) {
		StringBuilder queryId = new StringBuilder();
		queryId.append(queryName);
		queryId.append(", id:");
		queryId.append("" + id);
		return queryId.toString();
	}
}
