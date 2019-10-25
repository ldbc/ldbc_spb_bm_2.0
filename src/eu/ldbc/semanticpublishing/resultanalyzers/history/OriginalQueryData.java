package eu.ldbc.semanticpublishing.resultanalyzers.history;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;

public abstract class OriginalQueryData {
	String timeStamp;
	String originalQueryString;
	SparqlQueryConnection.QueryType originalQueryType;
	String originalQueryName;

	public String getOriginalQueryString() {
		return originalQueryString;
	}

	public SparqlQueryConnection.QueryType getOriginalQueryType() {
		return originalQueryType;
	}

	public String getOriginalQueryName() {
		return originalQueryName;
	}

	public String getTimeStamp() {
		return timeStamp;
	}
}
