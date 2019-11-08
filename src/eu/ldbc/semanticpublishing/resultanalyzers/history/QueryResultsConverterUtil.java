package eu.ldbc.semanticpublishing.resultanalyzers.history;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.ldbc.semanticpublishing.agents.HistoryAgent;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResultsConverterUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryResultsConverterUtil.class);
	private static final String BASE_URI_STRING = "http://www.ldbc.eu";
	private static final OutputStream output = new LogOutputStream(HistoryAgent.FAILED_HISTORY_QUERIES_LOGGER);

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

	public static void writeModelResultsToLogger(Model model) {
		Rio.write(model, output, RDFFormat.TURTLE);
	}

	public static void writeBindingSetToLogger(List<BindingSet> list) {

		final TupleQueryResultWriter writer = new SPARQLResultsCSVWriter(output);

		for (BindingSet bindingSet : list) {
			Set<String> bindingNames = bindingSet.getBindingNames();
			writer.startQueryResult(new ArrayList<>(bindingNames));
			writer.handleSolution(bindingSet);
			writer.endQueryResult();
		}
	}
}
