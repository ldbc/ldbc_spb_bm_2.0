package eu.ldbc.semanticpublishing.util;

import eu.ldbc.semanticpublishing.util.sesame.CroppingRDFHandler;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFHandlerException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for configuring the POST http request with different serialization content types.
 */
public class RdfUtils {
	
	private static final int READ_BUFFER_SIZE_BYTES = 128 * 1024;
	
	public static final String CONTENT_TYPE_RDFXML = "application/rdf+xml";
	public static final String CONTENT_TYPE_NQUADS = "application/n-quads";
	public static final String CONTENT_TYPE_SESAME_NQUADS = "text/x-nquads";
	public static final String CONTENT_TYPE_TRIG = "application/x-trig";
	public static final String CONTENT_TYPE_TRIG_STAR = "application/x-trigstar";
	public static final String CONTENT_TYPE_TURTLE = "application/x-turtle";
	public static final String CONTENT_TYPE_TURTLE_STAR = "application/x-turtlestar";
	
	private static String[] NAMESPACES = {"cwork:", "<http://www.bbc.co.uk/ontologies/creativework/>",
										  "bbc:"  , "<http://www.bbc.co.uk/ontologies/bbc/>"};

	public static void postStatements(String endpoint, String contentType, InputStream input) throws IOException {
		
		URL url = new URL(endpoint);
		HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();
		httpUrlConnection.setDefaultUseCaches(false);
		httpUrlConnection.setUseCaches(false);
		httpUrlConnection.setDoOutput(input != null);

		httpUrlConnection.setRequestMethod("POST");
		httpUrlConnection.setRequestProperty("Content-Type", contentType);
//		httpUrlConnection.setRequestProperty("Accept", "*/*");

		if(input != null) {
			OutputStream outStream = httpUrlConnection.getOutputStream();
			
			try {
				int b; 
				byte[] buffer = new byte[READ_BUFFER_SIZE_BYTES];
				while((b = input.read(buffer)) >= 0) {
					outStream.write(buffer, 0, b);
				}
				outStream.flush();
			}
			finally {
				input.close();
				outStream.close();
			}
		}
		
		int code = httpUrlConnection.getResponseCode();
		if (code < 200 || code >= 300) {
			throw new IOException("Posting statements received error code : " + code + " from server.");
		}
		
		httpUrlConnection.getInputStream().close();
	}
	
	/**
	 * The method can be used to "crop" entities for a dataset file, for example depending on the size of generated data
	 * @param datasetFile - full path name
	 * @param croppedDatasetFile - full path name
	 * @param entitiesLimit - maximum number of entities to keep in cropped file
	 * @throws RDFHandlerException 
	 * @throws RDFParseException 
	 */
	public static void cropDatasetFile(String datasetFile, String croppedDatasetFile, long entitiesLimit, String entityTypeUri) throws IOException {
		InputStream inputStream = new FileInputStream(datasetFile);
		try {
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			CroppingRDFHandler croppingRdfHandler = new CroppingRDFHandler(croppedDatasetFile, entitiesLimit, entityTypeUri);
			rdfParser.setRDFHandler(croppingRdfHandler);			
			rdfParser.parse(inputStream, datasetFile);
		} catch (RDFParseException pe) {
			System.out.println("RdfUtils : RDFParseException : " + pe.getMessage());
			pe.printStackTrace();
		} catch (RDFHandlerException he) {
			System.out.println("RdfUtils : RDFHandlerException : " + he.getMessage());
			he.printStackTrace();
		} finally {		
			inputStream.close();
		}
	}
	
	/**
	 * Expands name-space prefix if found in the input string to a URI
	 * 
	 * @param str - the string that will be expanded
	 * @return - full URI, or input string
	 */
	public static String expandNamepsacePrefix(String str) {
		for (int i = 0; i < NAMESPACES.length; i++) {
			if (i % 2 != 0) {
				continue;
			}
			String prefix = NAMESPACES[i];
			String URI = "";
			if (NAMESPACES[i+1].endsWith(">")) {
				URI = NAMESPACES[i+1].substring(0, NAMESPACES[i+1].length() - 1);
			}

			if (str.contains(prefix)) {
				StringBuilder sb = new StringBuilder();
				sb.append(str.replace(prefix, URI));
				sb.append(">");
				return sb.toString();
			}
		}
		return str;
	}	
}
