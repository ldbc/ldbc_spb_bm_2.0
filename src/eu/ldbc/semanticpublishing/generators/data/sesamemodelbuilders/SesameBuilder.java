package eu.ldbc.semanticpublishing.generators.data.sesamemodelbuilders;

import org.openrdf.model.Model;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public interface SesameBuilder {
	
	public static final ValueFactory sesameValueFactory = ValueFactoryImpl.getInstance();
	/**
	 * Method is responsible for building a Sesame model using an initialized templateParameterValues array
	 */
	public Model buildSesameModel();
}
