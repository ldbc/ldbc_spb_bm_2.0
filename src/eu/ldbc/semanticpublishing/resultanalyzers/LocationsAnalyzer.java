package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.IOException;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXLocationsTransformer;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;

/**
 * A class used to extract locations ids from provided reference dataset for Great Britain.
 * Executes a sparql query and parses the result using an implementation of the SAXResultTransformer interface.
 */
public class LocationsAnalyzer {
	private SparqlQueryExecuteManager sparqlQeuryManager;
	private MustacheTemplatesHolder queryTemplatesHolder;
	
	public LocationsAnalyzer(SparqlQueryExecuteManager sparqlQueryExecuteManager, MustacheTemplatesHolder queryTemplatesHolder) {
		this.sparqlQeuryManager = sparqlQueryExecuteManager;
		this.queryTemplatesHolder = queryTemplatesHolder;
	}
	
	public ArrayList<String> collectLocationsIds(String systemQueryFileNameExt) throws IOException {
		StringBuilder query = new StringBuilder(); 
		query.append(queryTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.SYSTEM).get(systemQueryFileNameExt));

		SAXLocationsTransformer geonamesTransformer = new SAXLocationsTransformer();
		sparqlQeuryManager.executeSystemQuery(geonamesTransformer, query.toString(), QueryType.SELECT);
		return geonamesTransformer.getLocationsIds();
	}	
}
