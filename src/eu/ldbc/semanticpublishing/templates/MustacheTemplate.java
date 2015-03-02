package eu.ldbc.semanticpublishing.templates;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;

/**
 * An abstract class forcing implementors to provide a mustache model for compiling a mustache template,
 * corresponding to a template file saved in Configuration.QUERIES_PATH/aggregation and 
 * editorial folders. Model consists of class members and methods that conform to the Mustache Java model.
 */
public abstract class MustacheTemplate {
	public static final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
	
	/**
	 * A HashMap of query templates, key=query file name, value=query template
	 */
	protected final HashMap<String, String> queryTemplates;
	
	protected int parameterIndex;
	protected String[] substitutionParameters;	
	
	public MustacheTemplate(HashMap<String, String> queryTemplates, String[] substitutionParameters) {
		this.queryTemplates = queryTemplates;
		this.substitutionParameters = substitutionParameters;
		this.parameterIndex = 0;		
	}
	
	/**
	 * Method will return a compiled query string, ready for execution
	 */
	public String compileMustacheTemplate() {
		StringReader reader = new StringReader(queryTemplates.get(getTemplateFileName()));
		Mustache mustache = mustacheFactory.compile(reader, getTemplateFileName());
		StringWriter writer = new StringWriter();
		mustache.execute(writer, this);
		writer.flush();
		return writer.toString();		
	}
	
	/**
	 * Returns the file name of the mustache template, for which the model will be built.
	 * Kindly forces implementors to store template file name in an instance member.
	 */
	public abstract String getTemplateFileName();
	
	public abstract QueryType getTemplateQueryType();
}
