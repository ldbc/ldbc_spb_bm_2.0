package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXQuery22TemplateTransformer;

/**
 * A class used to extract title, year, month, day from a query21.txt's result.
 */
public class Query22Analyzer {
	public ArrayList<String> collectDatesList(InputStream inputStreamResult) throws UnsupportedEncodingException {
		if (inputStreamResult == null) {
			return null;
		}
		SAXQuery22TemplateTransformer transformer = new SAXQuery22TemplateTransformer();
		transformer.transform(inputStreamResult);
		return transformer.getDatesList();
	}
}
