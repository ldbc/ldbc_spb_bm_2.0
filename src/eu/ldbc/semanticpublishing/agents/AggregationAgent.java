package eu.ldbc.semanticpublishing.agents;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParametersManager;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SPARQLResultStatementsCounter;
//import eu.ldbc.semanticpublishing.resultanalyzers.sesame.TurtleResultStatementsCounter;
import eu.ldbc.semanticpublishing.resultanalyzers.sesame.RDFXMLResultStatementsCounter;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.statistics.querypool.Pool;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.templates.aggregation.*;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.StringUtil;

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
//	private TurtleResultStatementsCounter turtleResultStatementsCounter;
	private RDFXMLResultStatementsCounter rdfXmlResultStatementsCounter;
	private SPARQLResultStatementsCounter sparqlResultStatementsCounter;
	private final boolean saveDetailedQueryLogs;
	
	private final static Logger DETAILED_LOGGER = LoggerFactory.getLogger(AggregationAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
//	private final static int MAX_DRILL_DOWN_ITERATIONS = 5;
	
	public AggregationAgent(AtomicBoolean benchmarkingState, SparqlQueryExecuteManager queryExecuteManager, RandomUtil ru, AtomicBoolean runFlag, HashMap<String, String> queryTamplates, Configuration configuration, Definitions definitions, SubstitutionQueryParametersManager substitutionQueryParametersMngr, long benchmarkByQueryMixRuns) {
		super(runFlag);
		this.queryExecuteManager = queryExecuteManager;
		this.ru = ru;
		this.benchmarkingState = benchmarkingState;
		this.queryTemplates = queryTamplates;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
		this.definitions = definitions;
		this.substitutionQueryParametersMngr = substitutionQueryParametersMngr;
//		this.turtleResultStatementsCounter = new TurtleResultStatementsCounter();
		this.rdfXmlResultStatementsCounter = new RDFXMLResultStatementsCounter();
		this.sparqlResultStatementsCounter = new SPARQLResultStatementsCounter();
		this.queryMixPool = new Pool(definitions.getString(Definitions.QUERY_POOLS), Statistics.totalStartedQueryMixRuns, Statistics.totalCompletedQueryMixRuns);
		this.benchmarkByQueryMixRuns = benchmarkByQueryMixRuns;
		this.saveDetailedQueryLogs = configuration.getBoolean(Configuration.SAVE_DETAILED_QUERY_LOGS);
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
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		InputStream inputStreamResult = null;

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
			
			inputStreamResult = queryExecuteManager.executeQueryWithInputStreamResult(connection, aggregateQuery.getTemplateFileName(), queryString, aggregateQuery.getTemplateQueryType(), true, false);			
			
			updateQueryStatistics(true, startedDuringBenchmarkPhase, aggregateQuery.getTemplateQueryType(), aggregateQuery.getTemplateFileName(), queryString, inputStreamResult, saveDetailedQueryLogs, queryId, System.currentTimeMillis() - executionTimeMs, timeStamp);

		} catch (Throwable t) {
			String msg = "WARNING : AggregationAgent [" + Thread.currentThread().getName() +"] reports: " + t.getMessage() + "\n" + "\tfor query : \n" + queryString + "\n...closing current connection and creating a new one..." + "\n----------------------------------------------------------------------------------------------\n";
			
			System.out.println(msg);
			
			DETAILED_LOGGER.warn(msg);
			
			try {
                
                updateQueryStatistics(false, startedDuringBenchmarkPhase, aggregateQuery.getTemplateQueryType(), aggregateQuery.getTemplateFileName(), queryString, inputStreamResult, saveDetailedQueryLogs, queryId, 0, timeStamp);

				msg = StringUtil.iostreamToString(inputStreamResult);
                               
                System.out.println("===============================");
                System.out.println("Dump of InputStream:");
                System.out.println(msg);
                System.out.println("===============================");					

			} catch (Throwable t1) {
				t1.printStackTrace();
			}
			
			connection.disconnect();
			connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
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
	
	private void updateQueryStatistics(boolean reportSuccess, boolean startedDuringBenchmarkPhase, QueryType queryType, String queryName, String queryString, InputStream inputStreamQueryResult, boolean useStringQueryResultOrInputStreamResult, long id, long queryExecutionTimeMs, String timeStamp) {
		//skip update of statistics for conformance queries
		if (queryName.startsWith("#")) {
			return;
		}
		
		int queryNumber = getQueryNumber(queryName);
		String queryNameId = constructQueryNameId(queryName, id);
		
		//count results (statements)
		long resultsCount = 0;
		
		String queryResultString = "";
		
		try {
			if (useStringQueryResultOrInputStreamResult) {
				//might increase memory footprint of the driver, as each query result will be stored into a String
				queryResultString = StringUtil.iostreamToString(inputStreamQueryResult);
				
				//reconvert the queryStringResult to an InputStream again, as the first one will be exhausted and not usable any more
				inputStreamQueryResult = StringUtil.stringToIostream(queryResultString);				
			}		
            
            if (reportSuccess) {
                if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
                  resultsCount = rdfXmlResultStatementsCounter.getStatementsCount(inputStreamQueryResult);
                  Statistics.timeCorrectionsMS.addAndGet(rdfXmlResultStatementsCounter.getParseTime());
                } else {
                  resultsCount = sparqlResultStatementsCounter.getStatementsCount(inputStreamQueryResult);
                  Statistics.timeCorrectionsMS.addAndGet(sparqlResultStatementsCounter.getParseTime());
                }	
            }
	        
			if (startedDuringBenchmarkPhase) {
				if (reportSuccess) {
					Statistics.aggregateQueriesArray[queryNumber - 1].reportSuccess(queryExecutionTimeMs);
					Statistics.totalAggregateQueryStatistics.reportSuccess(queryExecutionTimeMs);
					logBrief(timeStamp, queryNameId, queryType, "", queryExecutionTimeMs, resultsCount);
				} else {				
					Statistics.aggregateQueriesArray[queryNumber - 1].reportFailure();
					Statistics.totalAggregateQueryStatistics.reportFailure();
					logBrief(timeStamp, queryNameId, queryType, ", query error!", queryExecutionTimeMs, resultsCount);
				}        
			} else {
				if (queryExecutionTimeMs > 0) {
					DETAILED_LOGGER.info("\tQuery : " + queryName + ", time : " + timeStamp + " (" + queryExecutionTimeMs + " ms), " + "queryResult.length : " + queryResultString.length() + ", results : " + resultsCount + ", has been started during the warmup phase, it will be ignored in the benchmark result!");
					logBrief(timeStamp, queryNameId, queryType, ", has been started during the warmup phase, it will be ignored in the benchmark result!", queryExecutionTimeMs, resultsCount);
				} else {
					DETAILED_LOGGER.warn("\tQuery : " + queryName + ", time : " + timeStamp + " (" + queryExecutionTimeMs + " ms), " + "queryResult.length : " + queryResultString.length() + ", results : " + resultsCount + ", has failed to execute... possibly query timeout has been reached!");
					logBrief(timeStamp, queryNameId, queryType, ", has failed to execute... possibly query timeout has been reached!", queryExecutionTimeMs, resultsCount);
				}
			}
			
			DETAILED_LOGGER.info("\n*** Query [" + queryNameId + "], execution time : " + timeStamp + " (" + queryExecutionTimeMs + " ms), results : " + resultsCount + "\n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryNameId + "]" + " : \n" + (queryResultString.isEmpty() ? "Query results are not saved, to enable, set 'saveDetailedQueryLogs=true' in test.properties file." : ("Length : " + queryResultString.length() + "\n" + queryResultString)) + "\n\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void logBrief(String timeStamp, String queryId, QueryType queryType, String appendString, long queryExecutionTimeMs, long resultStatementsCount) {
		StringBuilder reportSb = new StringBuilder();
		reportSb.append(String.format("\t%s:\t[%s, %s] Query executed, execution time : %d ms, results : %d %s", timeStamp, queryId, Thread.currentThread().getName(), queryExecutionTimeMs, resultStatementsCount, appendString));
		
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
