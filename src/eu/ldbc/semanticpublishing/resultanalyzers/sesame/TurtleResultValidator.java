package eu.ldbc.semanticpublishing.resultanalyzers.sesame;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

public class TurtleResultValidator {
	private static final String BASE_URI_STRING = "http://www.ldbc.eu";
	
	public int validate(String validationString, String resultString) throws RDFParseException, RDFHandlerException, IOException {
		int errors = 0;
		
		Model validationModel;
		Model resultModel;

		validationModel = parse(RDFFormat.TURTLE, validationString);
		resultModel = parse(RDFFormat.TURTLE, resultString);
		
		for (Statement st : validationModel) {
			if (!resultModel.contains(st)) {
				System.out.println("\t\tvalidation failed, missing : " + st.toString());
				errors++;
			}
		}

		return errors;
	}
	
	private Model parse(RDFFormat rdfFormat, String stringModel) throws RDFParseException, RDFHandlerException, IOException {
		Model model = new LinkedHashModel();
		URL documentUrl = new URL(BASE_URI_STRING);	
		RDFParser rdfParser = Rio.createParser(rdfFormat);
		InputStream inputStream = new ByteArrayInputStream(stringModel.getBytes("UTF8"));	
		rdfParser.setRDFHandler(new RdfModelBuilder(model));
		rdfParser.parse(inputStream, documentUrl.toString());
		
		return model;
	}
	
	static class RdfModelBuilder extends RDFHandlerBase {
		private int modelStatementsCount = 0;
		private Model model;		
		
		public RdfModelBuilder(Model model) {
			this.model = model;
		}
		
		@Override
		public void handleStatement(Statement st) {
			modelStatementsCount++;
			model.add(st);
		}
		
		public int getStatementsCount() {
			return modelStatementsCount;
		}	
		
		public Model getModel() {
			return model;
		}
	}
}