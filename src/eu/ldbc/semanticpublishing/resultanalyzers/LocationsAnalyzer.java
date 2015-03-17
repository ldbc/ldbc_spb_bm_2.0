package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	public ArrayList<String> initFromFile(String fullPathName) throws IOException {
		String s;
		ArrayList<String> entitiesList = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fullPathName), "UTF-8"));
		
		try {
			while((s = in.readLine()) != null) {
				entitiesList.add(s);
			}
		} finally {
		    in.close();
		}		
		return entitiesList;		
	}
}
