package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXQuery22TemplateTransformer;

/**
 * A class used to extract title, year, month, day from a query21.txt's result.
 */
public class Query22Analyzer {
	public ArrayList<String> collectDatesList(String result) throws UnsupportedEncodingException {
		if (result.trim().isEmpty()) {
			return null;
		}
		SAXQuery22TemplateTransformer transformer = new SAXQuery22TemplateTransformer();
		transformer.transform(new ByteArrayInputStream(result.getBytes("UTF-8")));
		return transformer.getDatesList();
	}
}
