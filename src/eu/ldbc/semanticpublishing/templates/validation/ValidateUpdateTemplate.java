package eu.ldbc.semanticpublishing.templates.validation;

import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

public class ValidateUpdateTemplate extends MustacheTemplate {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "validateUpdate.txt";
	
	private String[] substitutionParameters;
	private int parameterIndex = 0;
	
	public ValidateUpdateTemplate(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.substitutionParameters = substitutionParameters;
	}
	
	/**
	 * A method for replacing mustache template : {{{contextURI}}}
	 */
	public String contextURI() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		return "";
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
