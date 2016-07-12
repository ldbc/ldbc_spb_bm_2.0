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
		BufferedReader in;
		
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(fullPathName), "UTF-8"));
			while((s = in.readLine()) != null) {
				entitiesList.add(s);
			}
			in.close();
		} catch (IOException e) {
			System.out.println("\t" + e.getMessage());
		}	
		
		return entitiesList;		
	}
	
	public void persistToFile(ArrayList<String> list, String fullPathName) throws IOException {
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fullPathName), "UTF-8"));
		
		try {
			for (String s : list) {
				out.write(s);
				out.write(String.format("%n"));
			}
		} finally {
			out.flush();
			out.close();
		}
	}
}
