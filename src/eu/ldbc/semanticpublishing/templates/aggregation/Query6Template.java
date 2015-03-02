package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query6.txt
 */
public class Query6Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query6.txt";
	
	//south boundary
	private double minLat = 50.45;	
	//north boundary
	private double maxLat = 53.25;
	//west boundary
	private double minLong = -2.15;
	//east boundary
	private double maxLong = 0.25;
	
	private final RandomUtil ru;	
	
	private double referenceLat = 0.0;
	private double referenceLong = 0.0;
	//deviation value, sets the range by adding/subtracting it from referenceLat and referenceLong
	private double deviationValue = 0.2;
	
	private Definitions definitions;
	
	public Query6Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		this.definitions = definitions;
		preInitialize();
	}
	
	private void preInitialize() {
		minLat = definitions.getDouble(Definitions.GEO_MIN_LAT);
		maxLat = definitions.getDouble(Definitions.GEO_MAX_LAT);
		minLong = definitions.getDouble(Definitions.GEO_MIN_LONG);
		maxLong = definitions.getDouble(Definitions.GEO_MAX_LONG);
		referenceLat = ru.nextDouble(minLat, maxLat);
		referenceLong = ru.nextDouble(minLong, maxLong);
		deviationValue = ru.nextDouble(0.20, 0.25);
	}
	
	public void initialize(double latitude, double longtitude, double deviationDecrease) {
		referenceLat = latitude;
		referenceLong = longtitude;
		deviationValue = ((deviationValue - deviationDecrease) > 0.0) ? (deviationValue - deviationDecrease) : 0.0 ;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLatitude}}}
	 */
	public String refLatitude() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return "" + referenceLat;
	}
	
	/**
	 * A method for replacing mustache template : {{{refLongtitude}}}
	 */
	public String refLongtitude() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
		
		return "" + referenceLong;
	}
	
	/**
	 * A method for replacing mustache template : {{{refDeviation}}}
	 */
	public String refDeviation() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "" + deviationValue;
	}	
	
	/**
	 * A method for replacing mustache template : {{{orderBy}}}
	 */			
	public String orderBy() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "";
	}	
	
	/**
	 * A method for replacing mustache template : {{{randomLimit}}}
	 */			
	public String randomLimit() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return "100";
	}	
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);
			sb.append(refLatitude());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(refLongtitude());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(refDeviation());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(orderBy());			
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
		return QueryType.SELECT;
	}	
}
