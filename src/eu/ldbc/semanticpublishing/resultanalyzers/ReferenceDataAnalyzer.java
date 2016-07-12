package eu.ldbc.semanticpublishing.resultanalyzers;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
	public static final String SEPARATOR = "\\|\\|\\|";
	
	private SparqlQueryExecuteManager sparqlQeuryManager;
	private MustacheTemplatesHolder queryTemplatesHolder;
	
	public ReferenceDataAnalyzer(SparqlQueryExecuteManager sparqlQueryExecuteManager, MustacheTemplatesHolder queryTemplatesHolder) {
		this.sparqlQeuryManager = sparqlQueryExecuteManager;
		this.queryTemplatesHolder = queryTemplatesHolder;
	}
	
	public ArrayList<Entity> initFromDatabase() throws IOException {
		StringBuilder query = new StringBuilder(); 
		query.append(queryTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.SYSTEM).get("analyzereferencedata.txt"));

		SAXReferenceDataEntityTransformer refDataBuilder = new SAXReferenceDataEntityTransformer();
		sparqlQeuryManager.executeSystemQuery(refDataBuilder, query.toString(), QueryType.SELECT);
		return refDataBuilder.getEntitiesList();
	}
	
	public ArrayList<Entity> initFromFile(String fullPathName) throws IOException {
		String s;
		BufferedReader in = null;
		ArrayList<Entity> entitiesList = new ArrayList<Entity>();
		
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(fullPathName), "UTF-8"));
			long lines = 0;
			while((s = in.readLine()) != null) {
				lines++;
				Entity entity = new Entity();
				if (s.trim().isEmpty()) {
					System.out.println("WARNING: empty line found at line: " + lines);
					continue;
				}
				String[] tokens = s.split(SEPARATOR);
				entity.setURI(tokens[0]);
				entity.setLabel(tokens[1]);
				entity.setCategory(tokens[2]);
				entity.setRank(tokens[3]);
				entitiesList.add(entity);
			}
		    in.close();
		} catch (IOException e) {
			System.out.println("\t" + e.getMessage());
		}		
		return entitiesList;		
	}
	
	public void persistToFile(ArrayList<Entity> list, String fullPathName) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPathName), "UTF-8"));		
		try {
			for (Entity e : list) {
				out.write(e.toString());
				out.write(String.format("%n"));
			}
		} finally {
			out.flush();
			out.close();
		}
	}
}
