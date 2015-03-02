package eu.ldbc.semanticpublishing.templates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.ldbc.semanticpublishing.util.FileUtils;

/**
 * A holder of all query template files (mustache or plain text) found in folders :
 *   Configuration.QUERIES_PATH/aggregation and Configuration.QUERIES_PATH/editorial. 
 *   Each of the queries is stored in a HashMap with key : fileName and value : query string
 * Class is instantiated once during the initialization of the benchmark driver
 */
public class MustacheTemplatesHolder {
	public static final String AGGREGATION = "aggregation";
	public static final String EDITORIAL = "editorial";
	public static final String SYSTEM = "system";
	public static final String VALIDATION = "validation";

	//declaring the HashMap final makes it immutable for concurrent reading
	private final HashMap<String, String> aggregationQueryTemplates = new HashMap<String, String>();
	private final HashMap<String, String> editorialQueryTemplates = new HashMap<String, String>();
	private final HashMap<String, String> systemQueryTemplates = new HashMap<String, String>();
	private final HashMap<String, String> validationQueryTemplates = new HashMap<String, String>();

	private String queryPath;
	
	public void loadFrom(String queryPath) throws IOException {
		this.queryPath = queryPath;
		initializeQueries(AGGREGATION);
		initializeQueries(EDITORIAL);
		initializeQueries(SYSTEM);
		initializeQueries(VALIDATION);
	}
		
	/**
	 * Initialize queryTemplates the HashMaps with aggregation and editorial queries
	 */
	private void initializeQueries(String type) throws IOException {
		StringBuilder pathSb = new StringBuilder();
		pathSb.append(queryPath);
		
		if (!queryPath.endsWith(File.separator)) {
			pathSb.append(File.separator);
		}
		
		pathSb.append(type);

		List<String> queryFiles = new ArrayList<String>();
		FileUtils.collectFilesList(pathSb.toString(), queryFiles, "txt", false);
		
		for (String filePath : queryFiles) {	
			StringBuilder sb = new StringBuilder();
			String[] queryString = FileUtils.readTextFile(filePath);
			
			for (int i = 0; i < queryString.length; i++) {
				sb.append(queryString[i]);
				sb.append("\n");
			}
			
			getQueryTemplates(type).put(filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.length()), sb.toString());		
		}
	}
	
	public HashMap<String, String> getQueryTemplates(String type) {
		if (type.equals(AGGREGATION)) {
			return aggregationQueryTemplates;
		} else if (type.equals(EDITORIAL)) {
			return editorialQueryTemplates;
		} else if (type.equals(SYSTEM)) {
			return systemQueryTemplates;
		} else if (type.equals(VALIDATION)) {
			return validationQueryTemplates;
		} else {
			return null;
		}
	}
}
