package eu.ldbc.semanticpublishing.templates.aggregation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * A class extending the MustacheTemplate, used to generate a query string
 * corresponding to file Configuration.QUERIES_PATH/aggregation/query7.txt
 */
public class Query7Template extends MustacheTemplate implements SubstitutionParametersGenerator {
	//must match with corresponding file name of the mustache template file
	private static final String templateFileName = "query7.txt";
	
	private final RandomUtil ru;
	private int creativeWorkType;
	private int year;
	private int month;
	private int day;
	private int maxDayOfMonth;
	private int hour;
	private int minute;
	private int deviation;
	private int iteration;
	private Calendar calendar;
	
	public Query7Template(RandomUtil ru, HashMap<String, String> queryTemplates, Definitions definitions, String[] substitutionParameters) {
		super(queryTemplates, substitutionParameters);
		this.ru = ru;
		this.calendar = Calendar.getInstance();
		preInitialize();
	}
	
	private void preInitialize() {
		creativeWorkType = Definitions.creativeWorkTypesAllocation.getAllocation();
		
		//Initializing year with a value that is certain to be used. see RandomUtil.YEARS_OFFSET
		calendar.setTime(ru.randomDateTime());
		year = calendar.get(Calendar.YEAR);
		month = ru.nextInt(1, 12 + 1);
		calendar.set(year, month - 1, 1);		
		day = ru.nextInt(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
		maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		hour = ru.nextInt(0, 23 + 1);
		minute = ru.nextInt(0, 59 + 1);
		deviation = 0;
		iteration = 0;
	}
	
/*
 *  Used for drill-down
 *  	
	public void initialize(String dateTimeString, int deviation) {
		//example of dateTime literal : 2012-08-22T18:22:38.240+03:00
		if (dateTimeString.indexOf("T") > 0) {
			String dateString = dateTimeString.substring(0, dateTimeString.indexOf("T"));
			String[] tokens = dateString.split("-");
			if (tokens.length == 3) {
//				int year = Integer.parseInt(dateTokens[0]);
				//TODO : if line below is uncommented, maxDayOfMonth should beupdated too								
//				this.month = Integer.parseInt(dateTokens[1]);
				this.day = Integer.parseInt(tokens[2]);
			}
			
			String timeString;
			if (dateTimeString.indexOf(".") > 0) {
				timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 1, dateTimeString.indexOf("."));
			} else {
				timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 1, dateTimeString.indexOf("T") + 9);
			}	
			tokens = timeString.split(":");			
			if (tokens.length == 3) {
				this.hour = Integer.parseInt(tokens[0]);
				this.minute = Integer.parseInt(tokens[1]);
//				this.seconds = Integer.parseInt(tokens[2]);
			}
		}
		
		this.deviation = deviation;
		
		iteration++;
	}
*/	
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
	 * A method for replacing mustache template : {{{liveCoverage}}}
	 */		
/*	 
	public String liveCoverage() {
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}
	
		return ru.nextBoolean() ? "\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean>" : "\"false\"^^<http://www.w3.org/2001/XMLSchema#boolean>"; 
	}
*/	
	
	/**
	 * A method for replacing mustache template : {{{cwAudience}}}
	 */	
/*	 
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
*/
	/**
	 * A method for replacing mustache template : {{{cwFilterDateModifiedCondition}}}
	 * with a FILTER constraint evaluating time range conditions
	 */		
	public String cwFilterDateModifiedCondition() {	
		if (substitutionParameters != null) {
			return substitutionParameters[parameterIndex++];
		}		
		
		if (iteration % 4 == 0) {
			//first iteration starts with a filter constraint for the whole month
			if (iteration > 0) {
				//5th++ iterations will start with a new randomly selected month
				month = ru.nextInt(1, 12 + 1);
				calendar.set(year, month - 1, 1);		
				maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
			return generateFilterDateString("dateModif", year, month, -1, -1, -1, -1, -1, -1);
		} else if (iteration % 4 == 1) {
			//further limiting to days range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, -1, -1, -1, -1);
		} else if (iteration % 4 == 2) {
			//further limiting to hours range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, hour, (hour + deviation) < 24 ? (hour + deviation) : hour, -1, -1);
		} else if (iteration % 4 == 3) {
			//further limiting to minutes range
			return generateFilterDateString("dateModif", year, month, day, (day + deviation) <= maxDayOfMonth ? (day + deviation) : day, hour, (hour + deviation) < 24 ? (hour + deviation) : hour, minute, (minute + deviation) < 60 ? (minute + deviation) : minute);
		}
		return generateFilterDateString("dateModif", year, month, -1, -1, -1, -1, -1, -1);
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
	
	/**
	 * Input parameters year and month are compulsory
	 * @param variableName - name of the variable used to hold the date in the SPARQL query
	 * @param year - the year used in the constraint
	 * @param month - the month used in the constraint, IF < 0 it is not used in the constraint
	 * @param day1 - start of the period, if < 0 it is not used
	 * @param day2 - end period, if < 0 it is not used
	 * @param hour1 - start hour, if < 0 it is not used
	 * @param hour2 - end hour, if < 0 it is not used
	 * @param minute1 - start minute, if < 0 it is not used
	 * @param minute2 - end minute, if < 0 it is not used
	 * @return SPARQL formatted string with FILTER constraint 
	 */
	private String generateFilterDateString(String variableName,int year, int month, int day1, int day2, int hour1, int hour2, int minute1, int minute2) {
		StringBuilder sb = new StringBuilder();
		StringBuilder sbStartRange = new StringBuilder();
		StringBuilder sbEndRange = new StringBuilder();
		
		Calendar calendar = Calendar.getInstance();
		
		calendar.set(year, month - 1, 1);
		
		sbStartRange.append("\"");
		sbEndRange.append("\"");
		
		sbStartRange.append(year);
		sbEndRange.append(year);
		
		sbStartRange.append("-");
		sbEndRange.append("-");
		
		if (month > 0) {
			sbStartRange.append(String.format("%02d", month));
			sbEndRange.append(String.format("%02d", month));
		} else {
			sbStartRange.append("01");
			sbEndRange.append("12");
		}
		sbStartRange.append("-");
		sbEndRange.append("-");
				
		if (day1 > 0) { 
			sbStartRange.append(String.format("%02d", day1));			
		} else {
			sbStartRange.append("01");
		}

		if (day2 > 0) {
			sbEndRange.append(String.format("%02d", day2));
		} else {
			sbEndRange.append(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		}	
		
		sbStartRange.append("T");
		sbEndRange.append("T");
		
		if (hour1 > 0) {
			sbStartRange.append(String.format("%02d", hour1));
		} else {
			sbStartRange.append("00");
		}
		sbStartRange.append(":");
		
		if (hour2 > 0) {
			sbEndRange.append(String.format("%02d", hour2));
		} else {
			sbEndRange.append("23");
		}
		sbEndRange.append(":");
		
		if (minute1 > 0) {
			sbStartRange.append(String.format("%02d", minute1));
		} else {
			sbStartRange.append("00");
		}
		sbStartRange.append(":");
		
		if (minute2 > 0) {
			sbEndRange.append(String.format("%02d", minute2));
		} else {
			sbEndRange.append("59");
		}
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
			sb.append(cwType());
			sb.append(SubstitutionParametersGenerator.PARAMS_DELIMITER);
			sb.append(cwFilterDateModifiedCondition());
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
