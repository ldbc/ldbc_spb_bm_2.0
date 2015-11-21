package eu.ldbc.semanticpublishing.resultanalyzers;

import java.io.InputStream;
import java.util.ArrayList;

import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SAXQuery17TemplateTransformer;

/**
 * A class used to extract cwork uris, geoLocationsids, lat and long properties from a query17.txt result.
 */
public class Query17Analyzer {
	public ArrayList<Entity> collectEntitiesList(InputStream inputStreamResult) {
		if (inputStreamResult == null) {		
			return null;
		}		
		SAXQuery17TemplateTransformer transformer = new SAXQuery17TemplateTransformer();
		transformer.transform(inputStreamResult);
		return transformer.getEntitiesList();
	}
}
