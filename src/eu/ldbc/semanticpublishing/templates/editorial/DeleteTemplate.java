package eu.ldbc.semanticpublishing.templates.editorial;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/editorial/delete.txt
 */
public class DeleteTemplate extends MustacheTemplate implements SubstitutionParametersGenerator {

	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "delete.txt"; 
	
	private final RandomUtil ru;
	
	public DeleteTemplate(RandomUtil ru, HashMap<String, String> queryTemplates) {
		super(queryTemplates, null);
		this.ru = ru;
	}
	
	public DeleteTemplate(RandomUtil ru, HashMap<String, String> queryTemplates, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwGraphUri}}}
	 */	
	public String cwGraphUri() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		long cwNextId = ru.nextInt((int)DataManager.creativeWorksNextId.get());
		return ru.numberURI("context", cwNextId, true, true);
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			sb.setLength(0);
			sb.append(cwGraphUri());
			sb.append("\n");
			if (bw != null) {
				bw.write(sb.toString());
			}
		}
		return sb.toString();
	}

	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}

	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.DELETE;
	}
}
