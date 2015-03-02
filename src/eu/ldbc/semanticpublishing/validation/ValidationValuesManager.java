package eu.ldbc.semanticpublishing.validation;

import java.io.File;
import java.io.IOException;

import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.util.StringUtil;

public class ValidationValuesManager {
	private static final ValidationValuesModel[] validationValuesArray;
	
	public static final String VALIDATION_PREFIX = "query";
	public static final String VALIDATION_SUFFIX = "Validation.txt";
	
	static {
		validationValuesArray = new ValidationValuesModel[Statistics.AGGREGATE_QUERIES_COUNT];
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			validationValuesArray[i] = new ValidationValuesModel(String.format("%s%d%s", VALIDATION_PREFIX, (i + 1), VALIDATION_SUFFIX));
		}
	}
	
	public void initValidationValues(String location, boolean suppressErrorMessages) throws IOException, InterruptedException {
		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			validationValuesArray[i].initFromFile(buildFilePath(location, validationValuesArray[i].getQueryName()), suppressErrorMessages);
		}
	}
	
	private String buildFilePath(String location, String fileName) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtil.normalizePath(location));
		sb.append(File.separator);
		sb.append(fileName);
		return sb.toString();
	}
	
	/**
	 * @param index
	 *            - Notice - index is zero based, while validation files are NOT
	 */
	public ValidationValuesModel getValidationValuesFor(int index) {
		return validationValuesArray[index];
	}	
}
