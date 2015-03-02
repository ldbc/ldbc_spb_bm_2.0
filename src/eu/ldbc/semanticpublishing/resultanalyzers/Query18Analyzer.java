package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXQuery18TemplateTransformer;

/**
 * A class used to extract cwork uris, geoLocationsids, lat and long properties from a query18.txt result.
 */
public class Query18Analyzer {
	public ArrayList<Entity> collectEntitiesList(String result) throws UnsupportedEncodingException {
		if (result.trim().isEmpty()) {	
			return null;
		}
		SAXQuery18TemplateTransformer transformer = new SAXQuery18TemplateTransformer();
		transformer.transform(new ByteArrayInputStream(result.getBytes("UTF-8")));
		return transformer.getEntitiesList();
	}
}
