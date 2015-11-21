package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.InputStream;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXQuery21TemplateTransformer;

/**
 * A class used to extract title, year, month, day from a query21.txt's result.
 */
public class Query21Analyzer {
	public ArrayList<String> collectDatesList(InputStream inputStreamResult) {
		if (inputStreamResult == null) {	
			return null;
		}
		SAXQuery21TemplateTransformer transformer = new SAXQuery21TemplateTransformer();
		transformer.transform(inputStreamResult);
		return transformer.getDatesList();
	}
}