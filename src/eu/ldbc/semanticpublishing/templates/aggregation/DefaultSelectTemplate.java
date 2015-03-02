package eu.ldbc.semanticpublishing.templates.aggregation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;

/**
 * A 'default' class extending the MustacheTemplate, used for building 
 * templates where no template parameters are set.
 */
public class DefaultSelectTemplate extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query-without-template.txt"; 
	
	public DefaultSelectTemplate(HashMap<String, String> queryTemplates) {
		super(queryTemplates, null);
	}

	@Override
	public String getTemplateFileName() {
		return templateFileName;
	}
	
	@Override
	public QueryType getTemplateQueryType() {
		return QueryType.SELECT;
	}
}
