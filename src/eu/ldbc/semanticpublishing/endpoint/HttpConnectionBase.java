package eu.ldbc.semanticpublishing.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Base class for all HTTP operations.
 */
public abstract class HttpConnectionBase {
	protected String endpointUrl;
	protected String endpointUpdateUrl;
	protected String contentTypeForGraphQuery;	
	protected int timeoutMilliseconds;
	protected HttpURLConnection httpUrlConnection;
	protected boolean verbose;
	
	public HttpConnectionBase(String endpointUrl, String endpointUpdateUrl, String contentTypeForGraphQuery, int timeoutMilliseconds, boolean verbose) {
		this.endpointUrl = endpointUrl;
		this.endpointUpdateUrl = endpointUpdateUrl;
		this.contentTypeForGraphQuery = contentTypeForGraphQuery;
		this.timeoutMilliseconds = timeoutMilliseconds;
		this.verbose = verbose;
	}
	
	private void connect() throws IOException {
		httpUrlConnection.connect();
	}
	
	private InputStream getResponse() throws IOException {
		
		int code = httpUrlConnection.getResponseCode();
		if ((code < 200 || code >= 300) && verbose) {
			System.out.println("HttpConnectionBase : received error code : " + code + " from server. Error message : " + httpUrlConnection.getResponseMessage());
		}
		
		return httpUrlConnection.getInputStream();
			
	}	
	
	public InputStream execute() throws IOException {
		connect();		
		InputStream returnedStream = getResponse();
		
		return returnedStream;
	}	
	
	public void disconnect() {
		httpUrlConnection.disconnect();
		httpUrlConnection = null;
	}
	
	/**
	 * Must provide implementation for that method, and execute it before starting a query
	 */
	public abstract void prepareConnection(boolean setQueryToStream);
}
