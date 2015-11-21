package eu.ldbc.semanticpublishing.substitutionparameters;

import java.io.File;
import java.io.IOException;

import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.util.FileUtils;

/**
 * A class which will manage initialization and consumption of generated
 * substitution query parameters
 */
public class SubstitutionQueryParametersManager {
	private static final SubstitutionQueryParameters[] aggregateSubstitutionParameters;
	private static final SubstitutionQueryParameters[] editorialSubstitutionParameters;

	public static final String AGGREGATE_QUERY_NAME = "query";
	public static final String INSERT_QUERY_NAME = "insert";
	public static final String SUBST_PARAMETERS_FILE_SUFFIX = "SubstParameters.txt";
	
	private static final String AGGREGATE_STRING_FORMAT = "%s%d%s";
	private static final String EDITORIAL_STRING_FORMAT = "%s%s";
	
	public static enum QueryType {AGGREGATE, EDITORIAL};
	
	static {
		aggregateSubstitutionParameters = new SubstitutionQueryParameters[Statistics.AGGREGATE_QUERIES_COUNT];
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			aggregateSubstitutionParameters[i] = new SubstitutionQueryParameters(String.format(AGGREGATE_STRING_FORMAT, AGGREGATE_QUERY_NAME, (i + 1), SUBST_PARAMETERS_FILE_SUFFIX));
		}
		
		editorialSubstitutionParameters = new SubstitutionQueryParameters[Statistics.EDITORIAL_QUERIES_COUNT];
		
		editorialSubstitutionParameters[0] = new SubstitutionQueryParameters(String.format(EDITORIAL_STRING_FORMAT, "insert", SUBST_PARAMETERS_FILE_SUFFIX));
	}

	public void intiSubstitutionParameters(String location, boolean suppressErrorMessagesForAggregate, boolean suppressErrorMessagesForEditorial) throws IOException,	InterruptedException {
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			aggregateSubstitutionParameters[i].initFromFile(buildFilePath(location, aggregateSubstitutionParameters[i].getQueryName()), suppressErrorMessagesForAggregate, true);
		}
		
		//editorial - initalizing insert substitution parameters only
		editorialSubstitutionParameters[0].initFromFile(buildFilePath(location, editorialSubstitutionParameters[0].getQueryName()), suppressErrorMessagesForEditorial, true);
	}

	private String buildFilePath(String location, String queryName) {
		StringBuilder sb = new StringBuilder();
		sb.append(FileUtils.normalizePath(location));
		sb.append(File.separator);
		sb.append(queryName);
		return sb.toString();
	}

	/**
	 * @param queryIndex
	 *            - Notice - queryIndex is zero based, while substitution query
	 *            parameters are NOT
	 */
	public SubstitutionQueryParameters getSubstitutionParametersFor(QueryType queryType, int queryIndex) {
		if (queryType == QueryType.AGGREGATE) {
			return aggregateSubstitutionParameters[queryIndex];
		} else if (queryType == QueryType.EDITORIAL) {
			return editorialSubstitutionParameters[0];
		}
		return null;
	}
	
	public boolean querySubstitutionParametersInitializedSuccessfully() {
		if (aggregateSubstitutionParameters[0].getParametersCount() == 0) {
			return false;
		}
		
		return true;
	}
}