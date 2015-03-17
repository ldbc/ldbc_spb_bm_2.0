package eu.ldbc.semanticpublishing.resultanalyzers;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXReferenceDataEntityTransformer;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;

/**
 * A class used to extract various properties from reference data stored in the repository.
 * Executes a sparql query and parses the result using an implementation of the SAXResultTransformer interface.
 */
public class ReferenceDataAnalyzer {
	private SparqlQueryExecuteManager sparqlQeuryManager;
	private MustacheTemplatesHolder queryTemplatesHolder;
	
	public ReferenceDataAnalyzer(SparqlQueryExecuteManager sparqlQueryExecuteManager, MustacheTemplatesHolder queryTemplatesHolder) {
		this.sparqlQeuryManager = sparqlQueryExecuteManager;
		this.queryTemplatesHolder = queryTemplatesHolder;
	}
	
	public ArrayList<Entity> analyzeEntities() throws IOException {
		StringBuilder query = new StringBuilder(); 
		query.append(queryTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.SYSTEM).get("analyzereferencedata.txt"));

		SAXReferenceDataEntityTransformer refDataBuilder = new SAXReferenceDataEntityTransformer();
		sparqlQeuryManager.executeSystemQuery(refDataBuilder, query.toString(), QueryType.SELECT);
		return refDataBuilder.getEntitiesList();
	}
	
	public ArrayList<Entity> initFromFile(String fullPathName) throws IOException {
		String s;
		ArrayList<Entity> entitiesList = new ArrayList<Entity>();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fullPathName), "UTF-8"));
		
		try {
			while((s = in.readLine()) != null) {
				Entity entity = new Entity();
				String[] tokens = s.split(";");
				entity.setURI(tokens[0]);
				entity.setLabel(tokens[1]);
				entity.setCategory(tokens[2]);
				entity.setRank(tokens[3]);
				entitiesList.add(entity);
			}
		} finally {
		    in.close();
		}		
		return entitiesList;		
	}
}
