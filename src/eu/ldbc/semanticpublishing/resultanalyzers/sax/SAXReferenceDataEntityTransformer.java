package eu.ldbc.semanticpublishing.resultanalyzers.sax;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
		<variable name='uri'/>
		<variable name='label'/>
		<variable name='category'/>
	</head>
	<results>
	<results>
		<result>
			<binding name='label'>
				<literal datatype='http://www.w3.org/2001/XMLSchema#string'>Oxford United</literal>
			</binding>
			<binding name='category'>
				<literal>SportsTeams</literal>
			</binding>
			<binding name='uri'>
				<uri>http://www.bbc.co.uk/things/03e20e1e-61e9-3749-ae53-0bbaa18e9ed9#id</uri>
			</binding>
		</result>
		<result>
			<binding name='label'>
				<literal datatype='http://www.w3.org/2001/XMLSchema#string'>Blackpool</literal>
			</binding>
			<binding name='category'>
				<literal>SportsTeams</literal>
			</binding>
			<binding name='uri'>
				<uri>http://www.bbc.co.uk/things/05251295-c015-2e42-839a-efe200441c9d#id</uri>
			</binding>
		</result>
		<result>
			<binding name='label'>
				<literal datatype='http://www.w3.org/2001/XMLSchema#string'>Reading</literal>
			</binding>
			<binding name='category'>
				<literal>SportsTeams</literal>
			</binding>
			<binding name='uri'>
				<uri>http://www.bbc.co.uk/things/0838f428-327a-7f41-9bf4-7758710b572b#id</uri>
			</binding>
		</result>
		...
	</results>
</sparql>	
*/

/**
 * SAX Parser for transforming an RDF-XML result stream into entities list
 */
public class SAXReferenceDataEntityTransformer extends DefaultHandler implements SAXResultTransformer {
	private ArrayList<Entity> entitiesList;
	private Set<String> uniqueSet;
	
	//<result>
	private boolean resultElementBeginFlag = false;
	//</result>
	
	//<uri>
	private boolean uriElementBeginFlag = false;
	//</uri>
	
	//<literal>
	private boolean literalElementBeginFlag = false;
	//</literal>
	
	private static final String RESULT_XML_ELEMENT = "result";
	private static final String URI_XML_ELEMENT = "uri";
	private static final String LITERAL_XML_ELEMENT = "literal";
	
	private static final String URI_BINDING_ELEMENT_ATTIRBUTE = "uri";
	private static final String CATEGORY_BINDING_ELEMENT_ATTRIBUTE = "category";
	private static final String LABEL_BINDING_ELEMENT_ATTRIBUTE = "label";
	private static final String RANK_BINDING_ELEMENT_ATTRIBUTE = "rank";
	
	private static final String BINDING = "binding";
	private static final String NAME = "name";
	
	private Entity entity;
	private boolean entityConstructionBegin = false;
	
	private StringBuilder uriSb;
	private StringBuilder categorySb;
	private StringBuilder labelSb;
	private StringBuilder rankSb;
	
	private String currentBindingName;
	
	@Override
	public void startDocument() throws SAXException {
		entitiesList = new ArrayList<Entity>();
		uniqueSet = new HashSet<String>();
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
				uriSb = new StringBuilder();
				labelSb = new StringBuilder();				
				categorySb = new StringBuilder();
				rankSb = new StringBuilder();
				
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
			
			//uriSb always contains result
			uriSb.insert(0, "<");
			uriSb.append(">");
						
			entity.setURI(uriSb.toString());
			entity.setLabel(labelSb.toString());
			entity.setCategory(categorySb.toString());
			entity.setRank(rankSb.toString());
			
			if (!uniqueSet.contains(uriSb.toString())) {
				entitiesList.add(entity);
				uniqueSet.add(uriSb.toString());
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {		
		if (length > 0 && entityConstructionBegin) {
			String str = new String(ch, start, length);
			
			if (uriElementBeginFlag) {
				if (currentBindingName.equals(URI_BINDING_ELEMENT_ATTIRBUTE)) {
					uriSb.append(str);
				}
			}
			
			if (literalElementBeginFlag) {
				if (currentBindingName.equals(LABEL_BINDING_ELEMENT_ATTRIBUTE)) {									
					labelSb.append(str);	
				} else if (currentBindingName.equals(CATEGORY_BINDING_ELEMENT_ATTRIBUTE)) {
					categorySb.append(str);
				} else if (currentBindingName.equals(RANK_BINDING_ELEMENT_ATTRIBUTE)) {
					rankSb.append(str);
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
