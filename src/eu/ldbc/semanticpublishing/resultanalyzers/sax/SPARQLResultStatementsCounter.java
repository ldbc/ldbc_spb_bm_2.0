package eu.ldbc.semanticpublishing.resultanalyzers.sax;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class SPARQLResultStatementsCounter extends DefaultHandler {
	private boolean resultElementBeginFlag = false;
	private long statementsCount = 0;
	
	private SAXParserFactory spf = SAXParserFactory.newInstance();
	private SAXParser saxParser;
	private XMLReader xmlReader;
	
	//stores last parse time for correcting query execution times
	private long parseTime = 0;

	
	private static final String RESULT_XML_ELEMENT = "result";
	
	public SPARQLResultStatementsCounter() {
		try {
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			saxParser = spf.newSAXParser();
			xmlReader = saxParser.getXMLReader();
			xmlReader.setContentHandler(this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void startDocument() throws SAXException {
		statementsCount = 0;
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		updateFlagsState(localName, true);
	}	

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		boolean wasInResultElement = false;
		
		if (resultElementBeginFlag && localName.equals(RESULT_XML_ELEMENT)) {
			wasInResultElement = true;
		}
			
		updateFlagsState(localName, false);
		
		if(wasInResultElement) {
			statementsCount++;
		}
	}

	private void updateFlagsState(String element, boolean state) {
		if (element.equals(RESULT_XML_ELEMENT)) {
			resultElementBeginFlag = state;
		} 
	}
	
	public long getStatementsCount(InputStream is) {
		try {
			long currentTime = System.currentTimeMillis();			
			xmlReader.parse(new InputSource(is));
			parseTime = System.currentTimeMillis() - currentTime;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return statementsCount;
	}	
	
	/**
	 * Method will be used to correct query execution times in statistics
	 * 
	 * @return time needed to count(parse) the result, ms
	 */
	public long getParseTime() {
		return parseTime;
	}
}
