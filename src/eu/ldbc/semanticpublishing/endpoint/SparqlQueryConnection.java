package eu.ldbc.semanticpublishing.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Used to prepare an HttpUrlConnection for executing a SPARQL query against a remote endpoint
 */
public class SparqlQueryConnection extends HttpConnectionBase {
	public static enum QueryType {
		SELECT, CONSTRUCT, DESCRIBE, INSERT, UPDATE, DELETE
	}
	
	private String queryString;
	private QueryType queryType;

	/**
	 * Constructs a SparqlQueryConnection using a new HttpUrlConnection
	 */
	public SparqlQueryConnection(String endpointUrl, String endpointUpdateUrl, String queryString, QueryType queryType, int timeoutMilliseconds, boolean verbose) {
		super(endpointUrl, endpointUpdateUrl, timeoutMilliseconds, verbose);
		this.queryString = queryString;
		this.queryType = queryType;
		prepareConnection(true);
	}
	
	/**
	 * Constructs a SparqlQueryConnection which allows to be re-used.
	*/
	public SparqlQueryConnection(String endpointUrl, String endpointUpdateUrl, int timeoutMilliseconds, boolean verbose) {
		super(endpointUrl, endpointUpdateUrl, timeoutMilliseconds, verbose);
		this.queryString = "";
		this.queryType = QueryType.CONSTRUCT;
		prepareConnection(false);
	}
	
	private String prepareEncodedUrlQueryString() throws UnsupportedEncodingException {
		String urlString = "";

		if (queryType == QueryType.INSERT || queryType == QueryType.UPDATE || queryType == QueryType.DELETE) {
			urlString = endpointUpdateUrl;
		} else {
			urlString = endpointUrl; // + "?" + "query=" + URLEncoder.encode(queryString, "UTF-8");
		}
		
		return urlString;
	}	

	@Override
	public void prepareConnection(boolean flushQueryContentsToStream) {

		try {
			URL url = new URL(prepareEncodedUrlQueryString());
			httpUrlConnection = (HttpURLConnection)url.openConnection();
			httpUrlConnection.setDoOutput(true);
			httpUrlConnection.setDefaultUseCaches(false);
			httpUrlConnection.setUseCaches(false);
			httpUrlConnection.setReadTimeout(timeoutMilliseconds);
			httpUrlConnection.setConnectTimeout(timeoutMilliseconds);

			boolean sparqlUpdate = queryType == QueryType.INSERT || queryType == QueryType.UPDATE || queryType == QueryType.DELETE;
			boolean graphQuery = queryType == QueryType.DESCRIBE || queryType == QueryType.CONSTRUCT;
			if (sparqlUpdate) {
				httpUrlConnection.setRequestMethod("POST");
				httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
//				httpUrlConnection.setRequestProperty("Content-Type", "application/sparql-update");
				httpUrlConnection.setRequestProperty("Accept", "*/*");
			} else {
//				httpUrlConnection.setRequestMethod("GET");
				httpUrlConnection.setRequestMethod("POST");
				httpUrlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
//				httpUrlConnection.setRequestProperty("Content-Type", "application/sparql-query");
				if (graphQuery) {
					httpUrlConnection.setRequestProperty("Accept", "application/rdf+xml");
//					httpUrlConnection.setRequestProperty("Accept", "application/x-turtle");
				} else {
					httpUrlConnection.setRequestProperty("Accept", "application/sparql-results+xml");
				}
			}
			if (flushQueryContentsToStream && !queryString.isEmpty()) {
				setOutputStream();
			}
		} catch (UnsupportedEncodingException uee) {
			System.out.println("SparqlQueryConnection : UnsupportedEncodingException : " + uee.getMessage());
			uee.printStackTrace();
		} catch (IOException ioe) {
			//http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
			System.out.println("SparqlQueryConnection : IOException : " + ioe.getMessage());
			try {
				//int responseCode = httpUrlConnection.getResponseCode();
				InputStream es = httpUrlConnection.getErrorStream();
	
				byte[] buffer = new byte[10000];
				// read the response body			
				while ((es.read(buffer)) > 0) {
					//consume ErrorStream's contents
				}
				es.close();
			} catch (IOException e) {
				//sink the exception, not interested if error stream produces it.
				e.printStackTrace();
			}
		}
	}	
	
	private void setOutputStream() throws IOException {
		boolean sparqlUpdate = queryType == QueryType.INSERT || queryType == QueryType.UPDATE || queryType == QueryType.DELETE;

		OutputStream outStream = httpUrlConnection.getOutputStream();
		if (sparqlUpdate) {
			outStream.write("update=".getBytes());
		}
		else {
			outStream.write("query=".getBytes());
		}
		outStream.write(URLEncoder.encode(queryString, "UTF-8").getBytes());
		outStream.flush();
		outStream.close();
	}
	
	public String getQueryString() {
		return this.queryString;
	}
	
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	
	public QueryType getQueryType() {
		return this.queryType;
	}
	
	public void setQueryType(QueryType queryType) {
		this.queryType = queryType;
	}
}
