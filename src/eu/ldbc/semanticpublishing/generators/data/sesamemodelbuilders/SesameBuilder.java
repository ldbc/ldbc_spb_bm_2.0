package eu.ldbc.semanticpublishing.generators.data.sesamemodelbuilders;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;

public interface SesameBuilder {
	
	public static final ValueFactory sesameValueFactory = ValueFactoryImpl.getInstance();
	/**
	 * Method is responsible for building a Sesame model using an initialized templateParameterValues array
	 */
	public Model buildSesameModel();
}
