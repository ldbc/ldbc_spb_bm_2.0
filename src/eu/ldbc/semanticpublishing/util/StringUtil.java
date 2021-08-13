package eu.ldbc.semanticpublishing.util;

import eu.ldbc.semanticpublishing.TestDriver;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * A utility class for performing various operations on strings. 
 */
public class StringUtil {

	private static String lineSeparator = System.getProperty("line.separator");
	/**
	 * Prepares a string from an input stream, used when needed to log a detailed query result
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String iostreamToString(InputStream is) throws IOException {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
	    String str;
	    try {
			br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			while (null != (str = br.readLine())) {
				sb.append(str).append(lineSeparator);
			}
		} finally {
	    	if (br != null) {
				br.close();
			}
		}

	    return sb.toString();
	}	
	
	/**
	 * Returns an InputStream from a given string using UTF-8 encoding
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static InputStream stringToIostream(String str) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(str.getBytes("UTF-8"));
	}
	
	/*	
	//Non UTF-8 compatible !!!
	private String readResultString(InputStream is) throws IOException {
		int length = 0;
		byte[] buffer = new byte[10000];
		StringBuilder sb = new StringBuilder();
		while((length = is.read(buffer)) != -1) {
			String s = new String(buffer, 0, length);
			sb.append(s);
		}
		return sb.toString();		
	}
	*/

	/**
	 *  Generates triple string representation for the CreativeWork,
	 *  to which all other statements are appended as metadata
	 * @param URI
	 * @return
	 */
	public static String generateEmbeddedTripleFromURI(String URI) {
		if (TestDriver.generatedCreativeWorksFormat == RDFFormat.TRIGSTAR) {
			return "<<" + URI + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.bbc.co.uk/ontologies/creativework/CreativeWork>>>";
		}
		return URI;
	}
}
