package eu.ldbc.semanticpublishing.tools.resultsvalidation;

public class DetailedResultModel implements Comparable <DetailedResultModel> {
	private long benchmarkRunMs;
	private String agent;
	private String thread;
	private String queryName;
	private long id;
	private String timeStamp;
	private String query;	
	private long executionTimeMs;
	private long results;
	private long reultStrLength;
	private String queryResultText;
	private String status;
	
	public DetailedResultModel() {
	}
	
	public DetailedResultModel(long benchmarkRunMs, String agent, String thread, String queryName, 
			long id, String timeStamp, long executionTimeMs, long results, long reultStrLength, String query, String queryResult, String status) {
		this.benchmarkRunMs = benchmarkRunMs;
		this.agent = agent;
		this.thread = thread;
		this.queryName = queryName;
		this.id = id;
		this.timeStamp = timeStamp;
		this.executionTimeMs = executionTimeMs;
		this.results = results;
		this.reultStrLength = reultStrLength;
		this.query = query;
		this.queryResultText = queryResult;
		this.status = status;
	}

	public long getBenchmarkRunMs() {
		return benchmarkRunMs;
	}

	public void setBenchmarkRunMs(long benchmarkRunMs) {
		this.benchmarkRunMs = benchmarkRunMs;
	}

	public String getAgent() {
		return agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public String getThread() {
		return thread;
	}

	public void setThread(String thread) {
		this.thread = thread;
	}

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public long getResults() {
		return results;
	}

	public void setResults(long results) {
		this.results = results;
	}

	public long getReultStrLength() {
		return reultStrLength;
	}

	public void setResultStrLength(long reultStrLength) {
		this.reultStrLength = reultStrLength;
	}

	public String getQueryResultText() {
		return queryResultText;
	}

	public void setQueryResultText(String queryResult) {
		this.queryResultText = queryResult;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "QueryResultModel [benchmarkRunMs=" + benchmarkRunMs + ", agent=" + agent + ", thread=" + thread
				+ ", queryName=" + queryName + ", id=" + id + ", timeStamp=" + timeStamp + ", query=" + query
				+ ", executionTimeMs=" + executionTimeMs + ", results=" + results + ", reultStrLength=" + reultStrLength
				+ ", queryResult=" + queryResultText + ", status=" + status + "]";
	}
	
	public String toShortString() {
		return "id: " + id + ", results: " + results;
	}

	@Override
	public int compareTo(DetailedResultModel o) {
		
		DetailedResultModel queryModel = (DetailedResultModel) o;
		
		if (this.getQuery().compareTo(queryModel.getQuery()) < 0 ||
				this.getResults() < queryModel.getResults()) {
			return -1;
		} else if (this.getQuery().compareTo(queryModel.getQuery()) > 0 ||
				this.getResults() > queryModel.getResults()) {
			return 1;
		}
		
		return 0;
	}
}
