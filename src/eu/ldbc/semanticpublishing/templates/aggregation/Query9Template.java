package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query9.txt
 */
public class Query9Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query9.txt";
	
	private final RandomUtil ru;
	private long cwNextId;
	
	public Query9Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		preInitialize();
	}
	
	private void preInitialize() {
		this.cwNextId = ru.nextInt((int)DataManager.creativeWorksNextId.get());
	}
	
	/**
	 * A method for replacing mustache template : {{{cwUri}}}
	 */			
	public String cwUri() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return ru.numberURI("things", cwNextId, true, true);
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwUri());			
			sb.append("\n");
			bw.write(sb.toString());
		}
		return null;
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
