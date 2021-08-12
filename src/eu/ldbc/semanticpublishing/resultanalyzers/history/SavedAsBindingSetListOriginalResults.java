package eu.ldbc.semanticpublishing.resultanalyzers.history;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.List;

public class SavedAsBindingSetListOriginalResults extends OriginalQueryData {

	private List<BindingSet> savedBindingSets;

	public SavedAsBindingSetListOriginalResults(String timeStamp, String originalQueryString, List<BindingSet> originalBindingSets,
												SparqlQueryConnection.QueryType originalQueryType, String originalQueryName) {
		this.timeStamp = timeStamp;
		this.originalQueryString = originalQueryString;
		this.originalQueryType = originalQueryType;
		this.originalQueryName = originalQueryName;
		this.savedBindingSets = originalBindingSets;
	}

	public List<BindingSet> getSavedBindingSets() {
		return savedBindingSets;
	}
}
