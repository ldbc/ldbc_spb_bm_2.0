package eu.ldbc.semanticpublishing.endpoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXResultTransformer;

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
	public String executeQuery(String queryName, String queryString, QueryType queryType) throws IOException {
		return executeQuery(new SparqlQueryConnection(endpointUrl, endpointUpdateUrl, queryTimeoutMilliseconds, verbose), queryName, queryString, queryType, false, true);
	}
	
	/**
	 * Executes a query by using an existing connection, requires an explicit disconnect.
	 * @param connection - a prepared connection
	 * @return count of bytes from returned result
	 * @throws IOException
	 */
	public String executeQuery(SparqlQueryConnection connection, String queryName, String queryString, QueryType queryType, boolean useInStatistics, boolean disconnect) throws IOException {
		
		connection.setQueryString(queryString);
		connection.setQueryType(queryType);
		connection.prepareConnection(true);
		
		InputStream is = connection.execute();
		
		String queryResult = readResultString2(is);
		
		if (disconnect) {
			connection.disconnect();
		}
		
		return queryResult;		
	}


	public InputStream executeQueryStream(SparqlQueryConnection connection, String queryName, String queryString, QueryType queryType, boolean useInStatistics, boolean disconnect) throws IOException {

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
		
		SparqlQueryConnection sparqlQuery = new SparqlQueryConnection(endpointUrl, endpointUpdateUrl, queryString, queryType, systemQueryTimeoutMilliseconds, verbose);		
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
	
/*	
	//Non UTF-8 compatible !!!
	private String readResultString(InputStream is) throws IOException {
		int length = 0;
		byte[] buffer = new byte[10000];
		StringBuilder sb = new StringBuilder();
		while((length = is.read(buffer)) != -1) {
			String s = new String(buffer, 0, length);
			sb.append(s);
		}
		return sb.toString();		
	}
*/
	
	private String readResultString2(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder sb = new StringBuilder();
	    String str;
	    while (null != (str = br.readLine())) {
	        sb.append(str).append("\r\n"); 
	    }
	    br.close();	    
	    return sb.toString();
	}
	
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
