package eu.ldbc.semanticpublishing.resultanalyzers.history;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import org.eclipse.rdf4j.model.Model;

public class SavedAsModelOriginalResults extends OriginalQueryData {

	private Model savedModel;

	public SavedAsModelOriginalResults(String timeStamp, String originalQueryString, Model originalModel,
									   SparqlQueryConnection.QueryType originalQueryType, String originalQueryName) {
		this.timeStamp = timeStamp;
		this.originalQueryString = originalQueryString;
		this.originalQueryType = originalQueryType;
		this.originalQueryName = originalQueryName;
		this.savedModel = originalModel;
	}

	public Model getSavedModel() {
		return savedModel;
	}
}
