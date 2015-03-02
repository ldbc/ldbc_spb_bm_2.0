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

public class SAXQuery21TemplateTransformer extends DefaultHandler implements SAXResultTransformer {
	protected boolean entityConstructionBegin = false;
	protected boolean resultElementBeginFlag = false;
	protected boolean uriElementBeginFlag = false;
	protected boolean literalElementBeginFlag = false;

	protected static final String RESULT_XML_ELEMENT = "result";
	protected static final String URI_XML_ELEMENT = "uri";
	protected static final String LITERAL_XML_ELEMENT = "literal";
	
	protected static final String MONTH_BINDING_ELEMENT_ATTRIBUTE = "month";
	protected static final String YEAR_BINDING_ELEMENT_ATTRIBUTE = "year";
	
	protected static final String BINDING = "binding";
	protected static final String NAME = "name";	
	
	protected String currentBindingName;
	
	protected StringBuilder monthSb;
	protected StringBuilder yearSb;
	protected ArrayList<String> datesList;
	
	@Override
	public void startDocument() throws SAXException {
		datesList = new ArrayList<String>();
	}
	
	/////////////////////////
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		updateFlagsState(localName, true);
		
		if (resultElementBeginFlag) {
			String attributeName = atts.getValue(NAME);
			if (attributeName != null && localName.equals(BINDING)) {
				currentBindingName = attributeName;
			}
			
			if (entityConstructionBegin == false) {
				monthSb = new StringBuilder();
				yearSb = new StringBuilder();
				
				currentBindingName = "";
				entityConstructionBegin = true;
			}
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {		
		if (length > 0 && entityConstructionBegin) {
			String str = new String(ch, start, length);
			
			if (uriElementBeginFlag) {
				//keeping it empty for future changes of the query
			}
			
			if (literalElementBeginFlag) {
				if (currentBindingName.equals(MONTH_BINDING_ELEMENT_ATTRIBUTE)) {									
					monthSb.append(str);	
				}
				
				if (currentBindingName.equals(YEAR_BINDING_ELEMENT_ATTRIBUTE)) {									
					yearSb.append(str);	
				}				
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
			
			datesList.add(yearSb.toString() + "-" + monthSb.toString() + "-" + "0");
		}
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
	
	/**
	 * Updates boolean flags about which element from parsing is currently visited.
	 */
	protected void updateFlagsState(String element, boolean state) {
		if (element.equals(RESULT_XML_ELEMENT)) {
			resultElementBeginFlag = state;
			
		} else if (element.equals(URI_XML_ELEMENT)) {
			uriElementBeginFlag = state;
			
		} else if (element.equals(LITERAL_XML_ELEMENT)) {
			literalElementBeginFlag = state;
		}
	}
	
	public ArrayList<String> getDatesList() {
		return datesList;
	}
}
