package eu.ldbc.semanticpublishing.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXResultTransformer;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.StringUtil;

/**
 * The class for handling a SPARQL query execution.
 */
public class SparqlQueryExecuteManager {
	private String endpointUrl;
	private String endpointUpdateUrl;
	private int queryTimeoutMilliseconds;
	private int systemQueryTimeoutMilliseconds;
	private boolean verbose;
	
	public SparqlQueryExecuteManager(AtomicBoolean benchmarkState, String endpointUrl, String endpointUpdateUrl, int queryTimeoutMilliseconds, int systemQueryTimeoutMilliseconds, boolean verbose) {
		this.endpointUrl = endpointUrl;
		this.endpointUpdateUrl = endpointUpdateUrl;
		this.queryTimeoutMilliseconds = queryTimeoutMilliseconds;
		this.systemQueryTimeoutMilliseconds = systemQueryTimeoutMilliseconds;
		this.verbose = verbose;
	}
	
	/**
	 * Executes the query by creating a new connection and disconnects afterwards. Typically not used for the benchmark run. 
	 * @param queryString - the query string
	 * @param queryType - query type
	 * @return count of bytes from the returned result 
	 * @throws IOException
	 */
	public String executeQueryWithStringResult(String queryName, String queryString, QueryType queryType, String contentTypeForGraphQuery) throws IOException {
		return executeQueryWithStringResult(new SparqlQueryConnection(endpointUrl, endpointUpdateUrl, contentTypeForGraphQuery, queryTimeoutMilliseconds, verbose), queryName, queryString, queryType, false, true);
	}

	public String executeQueryWithStringResult(SparqlQueryConnection connection, String queryName, String queryString, QueryType queryType, boolean useInStatistics, boolean disconnect) throws IOException {
		return executeQueryWithStringResult(connection, queryName, queryString, queryType, useInStatistics, disconnect, false);
	}
	
	/**
	 * Executes a query by using an existing connection, requires an explicit disconnect.
	 * @param
	 * @return query result as string, or empty string controlled by returnQueryResultAsString (if set to false will reduce memory footprint of the driver)
	 * @throws IOException
	 */
	public String executeQueryWithStringResult(SparqlQueryConnection connection, String queryName, String queryString, QueryType queryType, boolean useInStatistics, boolean disconnect, boolean useTrigStarResult) throws IOException {
		
		connection.setQueryString(queryString);
		connection.setQueryType(queryType);
		if (useTrigStarResult) {
			connection.setResultType(SparqlQueryConnection.SPARQL_STAR_RESULT_TYPE);
		}
		connection.prepareConnection(true);
		
		InputStream is = connection.execute();
		
		String queryResult = StringUtil.iostreamToString(is);		
		
		if (disconnect) {
			connection.disconnect();
		}
		
		return queryResult;		
	}
	
	public String executeQueryWithInputStreamResult(String queryName, String queryString, QueryType queryType, String contentTypeForGraphQuery) throws IOException {
		return executeQueryWithStringResult(new SparqlQueryConnection(endpointUrl, endpointUpdateUrl, contentTypeForGraphQuery, queryTimeoutMilliseconds, verbose), queryName, queryString, queryType, false, true);
	}		
	
	public InputStream executeQueryWithInputStreamResult(SparqlQueryConnection connection, String queryName, String queryString, QueryType queryType, boolean useInStatistics, boolean disconnect) throws IOException {
		
		connection.setQueryString(queryString);
		connection.setQueryType(queryType);
		connection.prepareConnection(true);
		
		InputStream is = connection.execute();		
		
		if (disconnect) {
			connection.disconnect();
		}
		
		return is;		
	}
	
	/**
	 * A service method for executing queries not related to the benchmark run.
	 * Always executed in a new connection, used for execution of queries during ontologies and reference datasets loading only.
	 * For system queries using systemQueryTimeoutMilliseconds instead of queryTimeoutMilliseconds
	 * @param transformer
	 * @param queryString
	 * @param queryType
	 * @throws IOException
	 */
	public void executeSystemQuery(SAXResultTransformer transformer, String queryString, QueryType queryType) throws IOException {
		
		SparqlQueryConnection sparqlQuery = new SparqlQueryConnection(endpointUrl, endpointUpdateUrl, RdfUtils.CONTENT_TYPE_RDFXML, queryString, queryType, systemQueryTimeoutMilliseconds, verbose);		
		InputStream is = sparqlQuery.execute();
		if (is == null) {
			System.out.println("Unable to execute query : \n" + queryString);
		} else {
			transformer.transform(is);
		}
		
		sparqlQuery.disconnect();
	}

/*	
	private int countResultBytes(InputStream is) throws IOException {
		int length = 0;
		int bytesCount = 0;
		byte[] buffer = new byte[10000];

		while((length = is.read(buffer)) != -1) {
			bytesCount += length;
		}
		
		return bytesCount;
	}
*/	
	
	public String getEndpointUrl() {
		return this.endpointUrl;
	}
	
	public String getEndpointUpdateUrl() {
		return this.endpointUpdateUrl;
	}
	
	public int getTimeoutMilliseconds() {
		return this.queryTimeoutMilliseconds;
	}
	
	public void setTimeoutsMilliseconds(int milliseconds) {
		this.queryTimeoutMilliseconds = milliseconds;
		this.systemQueryTimeoutMilliseconds = milliseconds;
	}
}
