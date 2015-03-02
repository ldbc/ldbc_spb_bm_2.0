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

/**
 * SAX Parser for transforming an RDF-XML result stream into a list
 */
public class SAXQuery18TemplateTransformer extends DefaultHandler implements SAXResultTransformer {
	private boolean entityConstructionBegin = false;
	private boolean resultElementBeginFlag = false;
	private boolean uriElementBeginFlag = false;
	private boolean literalElementBeginFlag = false;
	
	private static final String RESULT_XML_ELEMENT = "result";	
	private static final String URI_XML_ELEMENT = "uri";
	private static final String LITERAL_XML_ELEMENT = "literal";
	
	private static final String CWORK_BINDING_ELEMENT_ATTIRBUTE = "cwork";
	private static final String DATEMODIF_BINDING_ELEMENT_ATTRIBUTE = "dateModif";
	
	private static final String BINDING = "binding";
	private static final String NAME = "name";	
	
	private String currentBindingName;
	
	private StringBuilder cworkSb;
	private StringBuilder dateModifSb;
	
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
				cworkSb = new StringBuilder();
				dateModifSb = new StringBuilder();
				
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
			
			//each entity will be identified with a Creative Work ID. Each of the properties will follow query 25 predicate names
			entity.setURI(cworkSb.toString());
			entity.addTriple(cworkSb.toString(), "cwork:dateModified", dateModifSb.toString());
			entitiesList.add(entity);
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {		
		if (length > 0 && entityConstructionBegin) {
			String str = new String(ch, start, length);
			
			if (uriElementBeginFlag) {
				if (currentBindingName.equals(CWORK_BINDING_ELEMENT_ATTIRBUTE)) {
					cworkSb.append(str);
				}
			}
			
			if (literalElementBeginFlag) {
				if (currentBindingName.equals(DATEMODIF_BINDING_ELEMENT_ATTRIBUTE)) {									
					dateModifSb.append(str);	
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
