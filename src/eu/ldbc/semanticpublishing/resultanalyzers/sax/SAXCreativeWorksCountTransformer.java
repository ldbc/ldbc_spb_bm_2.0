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

/**
 * SAX Parser for extracting a total count of Creative Works from an RDF-XML result stream.
 */
public class SAXCreativeWorksCountTransformer extends DefaultHandler implements SAXResultTransformer {
	private boolean literalElementStarted = false;
	private static final String LITERAL = "literal";
	
	private StringBuilder resultSb;
	
	@Override
	public void startDocument() throws SAXException {
		resultSb = new StringBuilder();
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (localName.equals(LITERAL)) {
			literalElementStarted = true;
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equals(LITERAL)) {
			literalElementStarted = false;
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (length > 0 && literalElementStarted) {
			String str = new String(ch, start, length);
			resultSb.append(str);
		}
	}	
	
	public long getResult() {
		if (resultSb.toString().isEmpty()) {
			return 0;
		}
		return Long.parseLong(resultSb.toString());
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
