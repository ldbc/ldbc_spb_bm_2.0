package eu.ldbc.semanticpublishing.util;

import org.eclipse.rdf4j.rio.RDFFormat;

/**
 * Utility class for providing functionality specific to the Sesame RDF Framework
 *
 */
public class SesameUtils {
	public static RDFFormat parseRdfFormat(String serializationFormat) {
		RDFFormat rdfFormat = RDFFormat.NQUADS;
		
		if (serializationFormat.equalsIgnoreCase("BinaryRDF")) {
			rdfFormat = RDFFormat.BINARY;
		} else if (serializationFormat.equalsIgnoreCase("TriG")) {
			rdfFormat = RDFFormat.TRIG;
		} else if (serializationFormat.equalsIgnoreCase("TriG*")) {
			rdfFormat = RDFFormat.TRIGSTAR;
		} else if (serializationFormat.equalsIgnoreCase("TriX")) {
			rdfFormat = RDFFormat.TRIX;
		} else if (serializationFormat.equalsIgnoreCase("N-Triples")) {
			rdfFormat = RDFFormat.NTRIPLES;
		} else if (serializationFormat.equalsIgnoreCase("N-Quads")) {	
			rdfFormat = RDFFormat.NQUADS;
		} else if (serializationFormat.equalsIgnoreCase("N3")) {
			rdfFormat = RDFFormat.N3;
		} else if (serializationFormat.equalsIgnoreCase("RDF/XML")) {
			rdfFormat = RDFFormat.RDFXML;
		} else if (serializationFormat.equalsIgnoreCase("RDF/JSON")) {
			rdfFormat = RDFFormat.RDFJSON;
		} else if (serializationFormat.equalsIgnoreCase("Turtle")) {
			rdfFormat = RDFFormat.TURTLE;
		} else if (serializationFormat.equalsIgnoreCase("Turtle*")) {
			rdfFormat = RDFFormat.TURTLESTAR;
		} else {
			throw new IllegalArgumentException("Warning : unknown serialization format : " + serializationFormat + ", defaulting to N-Quads");
		}		
		
		return rdfFormat;
	}
}
