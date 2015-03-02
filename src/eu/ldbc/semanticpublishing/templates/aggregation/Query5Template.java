package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query5.txt
 */
public class Query5Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query5.txt";	

	private static final int TIME_UNIT = Calendar.HOUR;
	private static final int TIME_INTERVAL = 1;
	
	private Date initialDate;
	private int creativeWorkType;
	private final RandomUtil ru;	
	
	public Query5Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		preInitialize();
	}
	
	private void preInitialize() {
		this.initialDate = ru.randomDateTime();
		this.creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();		
	}
	
	/**
	 * A method for replacing mustache template : {{{cwType}}}
	 */	
	public String cwType() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		switch (creativeWorkType) {
		case 0 :
			return "cwork:BlogPost";
		case 1 :
			return "cwork:NewsItem";
		case 2 :
			return "cwork:Programme";
		}
		return "cwork:BlogPost";
	}
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
	public String cwAudience() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		switch (creativeWorkType) {
		//cwork:BlogPost
		case 0 :
			return "cwork:InternationalAudience";
		//cwork:NewsItem
		case 1 :
			return "cwork:NationalAudience";
		//cwork:Programme
		case 2 :
			return "cwork:InternationalAudience";
		}
		return "cwork:InternationalAudience";		
	}
	
	/**
	 * A method for replacing mustache template : {{{cwStartDateTime}}}
	 */
	public String cwStartDateTime() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return ru.dateTimeString(initialDate);
	}
	
	/**
	 * A method for replacing mustache template : {{{cwEndDateTime}}}
	 */
	public String cwEndDateTime() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(initialDate);
		calendar.add(TIME_UNIT, TIME_INTERVAL);
		return ru.dateTimeString(calendar.getTime());
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(cwType());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwAudience());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwStartDateTime());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwEndDateTime());			
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
