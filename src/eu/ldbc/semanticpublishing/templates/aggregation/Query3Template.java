package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query3.txt
 */
public class Query3Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query3.txt"; 
	
	private final RandomUtil ru;
	
	public Query3Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAboutUri}}}
	 */		
	public String cwAboutUri() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		//use a popular or regular entity for about/mentions uri 
		boolean usePopularEntity = Definitions.usePopularEntities.getAllocation() == 0;
		
		Entity e;
		
		if (usePopularEntity) {
			e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
		} else {
			e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
		}
		
		return e.getURI();
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */		
	public String cwAudience() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.nextBoolean() ? "cwork:NationalAudience" : "cwork:InternationalAudience";
	}
	
	/**
	 * A method for replacing mustache template : {{{randomLimit}}}
	 */			
	public String randomLimit() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "" + ru.nextInt(5, 20 + 1);
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			sb.setLength(0);
			sb.append(cwAboutUri());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwAudience());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(randomLimit());			
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
		return QueryType.DESCRIBE;
	}
}
