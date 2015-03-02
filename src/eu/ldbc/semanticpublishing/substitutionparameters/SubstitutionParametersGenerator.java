package eu.ldbc.semanticpublishing.substitutionparameters;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Interface for producers of query substitution parameter values. 
 *
 */
public interface SubstitutionParametersGenerator {
	public static final String PARAMS_DELIMITER = ";";
	
	/**
	 * @param bw - BufferedWriter used to persist parameter values. It is responsibility of the BufferedWriter producer to close it.
	 * @param amount - number of query runs that are expected to supplied with parameter values
	 * @return - String containing generated parameter values. It is advisable to return null when return value is not needed. Use with caution,
	 * e.g. if amount = 1000000, then returned string will take a lot of memory!
	 * @throws IOException
	 */
	public String generateSubstitutionParameters(BufferedWriter bw, int amount) throws IOException;
}
