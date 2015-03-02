package eu.ldbc.semanticpublishing.resultanalyzers.sax;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import eu.ldbc.semanticpublishing.refdataset.model.Entity;

//sample RDF-XML being parsed here :
/*
<?xml version='1.0' encoding='UTF-8'?>
<sparql xmlns='http://www.w3.org/2005/sparql-results#'>
	<head>
		<variable name='cwork'/>
		<variable name='geonamesId'/>
		<variable name='lat'/>
		<variable name='long'/>
	</head>
	<results>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/3333173/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.75</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/598#id</uri>
			</binding>
			<binding name='lat'>
				<literal>52.08333</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7292978/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.60855</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/3614#id</uri>
			</binding>
			<binding name='lat'>
				<literal>51.90273</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7294338/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.61922</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/2024#id</uri>
			</binding>
			<binding name='lat'>
				<literal>51.98746</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7295591/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.74088</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/2696#id</uri>
			</binding>
			<binding name='lat'>
				<literal>52.04557</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7295592/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.73606</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/3002#id</uri>
			</binding>
			<binding name='lat'>
				<literal>52.02429</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7296589/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.67368</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/3209#id</uri>
			</binding>
			<binding name='lat'>
				<literal>51.98207</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7298699/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.67269</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/2752#id</uri>
			</binding>
			<binding name='lat'>
				<literal>51.8512</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7299258/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.67593</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/2551#id</uri>
			</binding>
			<binding name='lat'>
				<literal>52.1349</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7299791/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.82868</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/623#id</uri>
			</binding>
			<binding name='lat'>
				<literal>51.99785</literal>
			</binding>
		</result>
		<result>
			<binding name='geonamesId'>
				<uri>http://sws.geonames.org/7301829/</uri>
			</binding>
			<binding name='long'>
				<literal>-0.65577</literal>
			</binding>
			<binding name='cwork'>
				<uri>http://www.bbc.co.uk/things/2040#id</uri>
			</binding>
			<binding name='lat'>
				<literal>52.01595</literal>
			</binding>
		</result>
	</results>
</sparql>
*/

/**
 * SAX Parser for transforming an RDF-XML result stream into a list
 */
public class SAXQuery17TemplateTransformer extends DefaultHandler implements SAXResultTransformer {
	private boolean entityConstructionBegin = false;
	private boolean resultElementBeginFlag = false;
	private boolean uriElementBeginFlag = false;
	private boolean literalElementBeginFlag = false;
	
	private static final String RESULT_XML_ELEMENT = "result";	
	private static final String URI_XML_ELEMENT = "uri";
	private static final String LITERAL_XML_ELEMENT = "literal";
	
	private static final String GEONAMESID_BINDING_ELEMENT_ATTIRBUTE = "geonamesId";
	private static final String LAT_BINDING_ELEMENT_ATTRIBUTE = "lat";
	private static final String LONG_BINDING_ELEMENT_ATTRIBUTE = "long";
	private static final String CWORK_BINDING_ELEMENT_ATTRIBUTE = "cwork";
	
	private static final String BINDING = "binding";
	private static final String NAME = "name";	
	
	private String currentBindingName;
	
	private StringBuilder locationsIdSb;
	private StringBuilder latSb;
	private StringBuilder longSb;
	private StringBuilder cworkSb;
	
	private Entity entity;
	private ArrayList<Entity> entitiesList; 

	@Override
	public void startDocument() throws SAXException {
		entitiesList = new ArrayList<Entity>();
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		updateFlagsState(localName, true);
		
		if (resultElementBeginFlag) {
			String attributeName = atts.getValue(NAME);
			if (attributeName != null && localName.equals(BINDING)) {
				currentBindingName = attributeName;
			}
			
			if (entityConstructionBegin == false) {
				entity = new Entity();
				locationsIdSb = new StringBuilder();
				latSb = new StringBuilder();				
				longSb = new StringBuilder();
				cworkSb = new StringBuilder();
				
				currentBindingName = "";
				entityConstructionBegin = true;
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		boolean wasInResultElement = false;
		
		if (resultElementBeginFlag && localName.equals(RESULT_XML_ELEMENT)) {
			wasInResultElement = true;
		}
			
		updateFlagsState(localName, false);
		
		if(wasInResultElement) {
			entityConstructionBegin = false;
			currentBindingName = "";
			
			if (cworkSb.length() > 0) {
				cworkSb.insert(0, "<");
				cworkSb.append(">");
			}
			
			if (locationsIdSb.length() > 0) {
				locationsIdSb.insert(0, "<");
				locationsIdSb.append(">");
			}
			
			//each entity will be identified with a Creative Work ID. Each of the properties will follow query 26 predicate names
			entity.setURI(cworkSb.toString());
			entity.addTriple(cworkSb.toString(), "geo:lat", latSb.toString());
			entity.addTriple(cworkSb.toString(), "geo:long", longSb.toString());
			entity.addTriple(cworkSb.toString(), "cwork:mentions", locationsIdSb.toString());
			entitiesList.add(entity);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {		
		if (length > 0 && entityConstructionBegin) {
			String str = new String(ch, start, length);
			
			if (uriElementBeginFlag) {
				if (currentBindingName.equals(GEONAMESID_BINDING_ELEMENT_ATTIRBUTE)) {
					locationsIdSb.append(str);
				} else if (currentBindingName.equals(CWORK_BINDING_ELEMENT_ATTRIBUTE)) {
					cworkSb.append(str);
				}
			}
			
			if (literalElementBeginFlag) {
				if (currentBindingName.equals(LAT_BINDING_ELEMENT_ATTRIBUTE)) {									
					latSb.append(str);	
				} else if (currentBindingName.equals(LONG_BINDING_ELEMENT_ATTRIBUTE)) {
					longSb.append(str);
				}
			}
		}
	}
	
	/**
	 * Updates boolean flags about which element from parsing is currently visited.
	 */
	private void updateFlagsState(String element, boolean state) {
		if (element.equals(RESULT_XML_ELEMENT)) {
			resultElementBeginFlag = state;
			
		} else if (element.equals(URI_XML_ELEMENT)) {
			uriElementBeginFlag = state;
			
		} else if (element.equals(LITERAL_XML_ELEMENT)) {
			literalElementBeginFlag = state;
		}
	}
	
	public ArrayList<Entity> getEntitiesList() {
		return entitiesList;
	}
	
	@Override
	public void transform(InputStream is) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			SAXParser saxParser;
			saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(this);
			xmlReader.parse(new InputSource(is));
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}				
	}
}
