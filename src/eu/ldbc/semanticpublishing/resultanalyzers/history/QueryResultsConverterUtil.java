package eu.ldbc.semanticpublishing.resultanalyzers.history;

import eu.ldbc.semanticpublishing.util.StringUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class QueryResultsConverterUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryResultsConverterUtil.class);
	private static final String BASE_URI_STRING = "http://www.ldbc.eu";

	public static List<BindingSet> getBindingSetsList(InputStream is) {
		QueryResultCollector handler = new QueryResultCollector();
		TupleQueryResultParser parser = new SPARQLResultsXMLParser();
		parser.setQueryResultHandler(handler);
		try {
			parser.parseQueryResult(is);
		} catch (IOException ex) {
			LOGGER.error("Couldn't parse returned sparql+xml result properly\n", ex);
		}
		return handler.getBindingSets();
	}


	public static Model getReturnedResultAsModel(InputStream is) {
		Model parsedModel = null;
		try {
			parsedModel = Rio.parse(is, BASE_URI_STRING, RDFFormat.RDFXML);
		} catch (RDFParseException | RDFHandlerException | IOException ex) {
			LOGGER.error("Couldn't parse returned RDFXML result properly\n", ex);
		}
		return parsedModel;
	}
}
