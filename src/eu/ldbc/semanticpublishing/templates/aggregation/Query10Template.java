package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query10.txt
 */
public class Query10Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query10.txt";
	
	private final RandomUtil ru;
	private Calendar calendar;
	private String geonamesId;
	private int year;
	private int month;
	private int day;
//	private int hour;
//	private int minute;
	

	public Query10Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		this.calendar = Calendar.getInstance();
		preInitialize();
	}
	
	private void preInitialize() {
		geonamesId = DataManager.locationsIdsList.get(ru.nextInt(DataManager.locationsIdsList.size()));
		//Initializing year with a value that is certain to be used. see RandomUtil.YEARS_OFFSET
		calendar.setTime(ru.randomDateTime());
		year = calendar.get(Calendar.YEAR);
		month = ru.nextInt(1, 12 + 1);
		calendar.set(year, month - 1, 1);		
		day = ru.nextInt(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
//		hour = ru.nextInt(0, 23 + 1);
//		minute = ru.nextInt(0, 59 + 1);		
	}
	
	/**
	 * A method for replacing mustache template : {{{geonamesFeatureURI}}}
	 */	
	public String geonamesFeatureURI() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return geonamesId;		
	}
	
	/**
	 * A method for replacing mustache template : {{{dateTimeRangeFilter}}}
	 * with a FILTER constraint evaluating time range conditions set to generate a 10 days time range
	 */	
	public String dateTimeRangeFilter() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		return generateFilterDateString2("dateModified", year, month, day, 5);
	}

	private String generateFilterDateString2(String variableName,int startYear, int startMonth, int startDay, int daysOffset) {
		StringBuilder sb = new StringBuilder();
		StringBuilder sbStartRange = new StringBuilder();
		StringBuilder sbEndRange = new StringBuilder();

		calendar.set(startYear, startMonth - 1, startDay, 0, 0, 0);
		calendar.add(Calendar.DATE, daysOffset);
		
		sbStartRange.append("\"");
		sbEndRange.append("\"");
		
		sbStartRange.append(startYear);
		sbEndRange.append(calendar.get(Calendar.YEAR));
		
		sbStartRange.append("-");
		sbEndRange.append("-");

		sbStartRange.append(String.format("%02d", startMonth));
		sbEndRange.append(String.format("%02d", calendar.get(Calendar.MONTH) + 1));

		sbStartRange.append("-");
		sbEndRange.append("-");
		
		sbStartRange.append(String.format("%02d", startDay));
		sbEndRange.append(String.format("%02d", calendar.get(Calendar.DATE)));
		
		sbStartRange.append("T");
		sbEndRange.append("T");
		
		sbStartRange.append("00");
		sbEndRange.append("23");
		
		sbStartRange.append(":");
		sbEndRange.append(":");
		
		sbStartRange.append("00");
		sbEndRange.append("59");
		
		sbStartRange.append(":");
		sbEndRange.append(":");		
		
		sbStartRange.append("00.000");
		sbEndRange.append("59.999");
		
		sbStartRange.append("\"");
		sbEndRange.append("\"");
		
		sbStartRange.append("^^<http://www.w3.org/2001/XMLSchema#dateTime>");
		sbEndRange.append("^^<http://www.w3.org/2001/XMLSchema#dateTime>");
		
		sb.append("FILTER(");
		sb.append("?");
		sb.append(variableName);
		sb.append(" >= ");
		sb.append(sbStartRange);
		sb.append(" && ");
		sb.append("?");
		sb.append(variableName);
		sb.append(" < ");
		sb.append(sbEndRange);
		sb.append(") . ");		
		
		return sb.toString();
	}
	
	@Override
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < amount; i++) {
			preInitialize();
			sb.setLength(0);			
			sb.append(geonamesFeatureURI());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(dateTimeRangeFilter());
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
