package eu.ldbc.semanticpublishing.util.sesame;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * Custom implementation of the RDFHandlerBase class, used to crop and serialize TURTLE files limited by number of entities of certain rdf:type 
 */
public class CroppingRDFHandler extends RDFHandlerBase {
	private long entitiesCount = 0;
	private long maxEntityLimit = 0;
	private String outputFileName;
	private List<Statement> entityStatements = new ArrayList<Statement>();
	
	private final ValueFactory factory = ValueFactoryImpl.getInstance();
	private FileOutputStream outputStream;
	private RDFWriter rdfWriter;
	private Resource lastSubject = null;
	private Resource foafPersonResource = null;
	private final URI rdfTypeUri = factory.createURI(RDF_TYPE_URI);
	
	protected static final String RDF_TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"; 
	
	public CroppingRDFHandler(String outputFileName, long maxEntityLimit, String filterURI) {
		this.outputFileName = outputFileName;
		this.maxEntityLimit = maxEntityLimit;
		this.foafPersonResource = factory.createURI(filterURI);
	}
	
	private void initializeRdfWriter(String outputFileName) {
		try {
			outputStream = new FileOutputStream(outputFileName);
			rdfWriter = Rio.createWriter(RDFFormat.TURTLE, outputStream);
			rdfWriter.startRDF();
		} catch (RDFHandlerException ex) {
			ex.printStackTrace();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeStatements(List<Statement> statements) {
		try {
			for (Statement st : statements) {
				rdfWriter.handleStatement(st);
			}
		} catch (RDFHandlerException e) {
			e.printStackTrace();
		}		
	}
	
	private void finalizeRdfWriter() {
		try {
			rdfWriter.endRDF();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RDFHandlerException ex) {
			ex.printStackTrace();			
		}
	}
	
	@Override 
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		initializeRdfWriter(outputFileName);
	}
	
	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		if (entitiesCount <= maxEntityLimit) {
			if (st.getSubject().equals(lastSubject) || lastSubject == null) {
				entityStatements.add(st);
			} else {
				writeStatements(entityStatements);
				entityStatements.clear();
				entityStatements.add(st);
			}
		} else {
			return;
		}
		
		if (st.getPredicate().equals(rdfTypeUri) && st.getObject().equals(foafPersonResource)) {
			entitiesCount++;
		}

		lastSubject = st.getSubject();
	}
	
	@Override
	public void endRDF() throws RDFHandlerException {
		super.endRDF();
		
		if (entityStatements.size() > 0) {
			writeStatements(entityStatements);
		}
		
		finalizeRdfWriter();
	}
}
