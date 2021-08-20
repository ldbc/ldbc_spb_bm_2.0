package eu.ldbc.semanticpublishing.validation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.agents.EditorialAgent;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParametersManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.templates.editorial.DeleteTemplate;
import eu.ldbc.semanticpublishing.templates.editorial.InsertTemplate;
import eu.ldbc.semanticpublishing.templates.editorial.UpdateTemplate;
import eu.ldbc.semanticpublishing.templates.validation.ValidateDeleteTemplate;
import eu.ldbc.semanticpublishing.templates.validation.ValidateInsertTemplate;
import eu.ldbc.semanticpublishing.templates.validation.ValidateUpdateTemplate;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;

public class EditorialOperationsValidator extends Validator {
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;
	private RandomUtil ru;
	private HashMap<String, String> editorialQueryTemplates;
	private HashMap<String, String> validationQueryTemplates;
	private Configuration configuration;
	private Definitions definitions;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(EditorialAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	
	private static final String STRING_RDF_TYPE = "<http://www.w3.org/2001/XMLSchema#string>";
	private static final String DATETIME_RDF_TYPE = "<http://www.w3.org/2001/XMLSchema#dateTime>";
	
	public static enum EditorialOperation {INSERT, UPDATE, DELETE};
	
	public EditorialOperationsValidator(SparqlQueryExecuteManager queryExecuteManager, RandomUtil ru, HashMap<String, String> editorialQueryTemplates, HashMap<String, String> validationQueryTemplates, Configuration configuration, Definitions definitions) {
		this.queryExecuteManager = queryExecuteManager;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_TURTLE, queryExecuteManager.getTimeoutMilliseconds(), true);
		this.ru = ru;
		this.editorialQueryTemplates = editorialQueryTemplates;
		this.validationQueryTemplates = validationQueryTemplates;
		this.configuration = configuration;
		this.definitions = definitions;
	}
	
	public void validate() throws IOException, InterruptedException {
		//generate insert substitution parameters
		int insertErrors = 0;
		int updateErrors = 0;
		int deleteErrors = 0;
		String targetFolder = configuration.getString(Configuration.VALIDATION_PATH);
		FileUtils.makeDirectories(targetFolder);
		BufferedWriter bw = null;
		int totalValidationIterations = 0;
		
		InsertTemplate insertQuery = new InsertTemplate("", ru, editorialQueryTemplates, definitions, false, null);
		
		try {
			bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%s%s", SubstitutionQueryParametersManager.INSERT_QUERY_NAME, SubstitutionQueryParametersManager.SUBST_PARAMETERS_FILE_SUFFIX))));
			totalValidationIterations = configuration.getInt(Configuration.VALIDATION_ITERATIONS);
			insertQuery.generateSubstitutionParameters(bw, totalValidationIterations);
			bw.close();
		} finally {
			try {bw.close();} catch(Exception e) {};
		}
		
		//retrieve them
		SubstitutionQueryParametersManager sqpm = new SubstitutionQueryParametersManager();
		sqpm.intiSubstitutionParameters(targetFolder, true, false);
		
		//INSERT
		System.out.println("\tvalidating INSERT operation :");
		for (int i = 0; i < totalValidationIterations; i++) {
			String[] parameters = sqpm.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.EDITORIAL, 0).get(i);
			insertErrors += validateAction(EditorialOperation.INSERT, i, parameters, true);
		}
		System.out.println("\t\t" + insertErrors + " errors");

		//UPDATE
		System.out.println("\tvalidating UPDATE operation :");
		for (int i = 0; i < totalValidationIterations; i++) {
			String[] parameters = sqpm.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.EDITORIAL, 0).get(i);
			
			//modify substitution parameters before validation
			parameters = modifySubstitutionParameters(i, prepareSubstitutionParametersForValidation(parameters));
			updateErrors += validateAction(EditorialOperation.UPDATE, i, parameters, true);
		}
		System.out.println("\t\t" + updateErrors + " errors");
		
		//DELETE
		System.out.println("\tvalidating DELETE operation :");
		for (int i = 0; i < totalValidationIterations; i++) {
			String[] parameters = sqpm.getSubstitutionParametersFor(SubstitutionQueryParametersManager.QueryType.EDITORIAL, 0).get(i);
			deleteErrors += validateAction(EditorialOperation.DELETE, i, parameters, true);
		}
		System.out.println("\t\t" + deleteErrors + " errors");
	}
	
	public int validateAction(EditorialOperation operationType, int iteration, String[] validationParameters, boolean closeConnection) throws IOException {
		int errors = 0;
		String queryName = "";
		String queryString = "";
		String queryResult = "";
		QueryType queryType;
		MustacheTemplate actionQuery = null;
		MustacheTemplate validateQuery = null;

		switch (operationType) {
			case INSERT :
				actionQuery = new InsertTemplate("", ru, editorialQueryTemplates, definitions, false, validationParameters);
			break;
			case UPDATE : 
				actionQuery = new UpdateTemplate("", ru, editorialQueryTemplates, definitions, false, validationParameters);
			break;
			case DELETE :
				actionQuery = new DeleteTemplate(ru, editorialQueryTemplates, validationParameters);
			break;
		}
		
		queryType = actionQuery.getTemplateQueryType();
		queryName = actionQuery.getTemplateFileName();
		queryString = actionQuery.compileMustacheTemplate();		

		queryResult = queryExecuteManager.executeQueryWithStringResult(connection, queryName, queryString, queryType, false, closeConnection);

		BRIEF_LOGGER.info(String.format("Query [%s] executed, iteration %d", queryName, iteration));
		LOGGER.info("\n*** Query [" + queryName + "], iteration " + iteration + "\n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryName + "]" + " : \n" + "Length : " + queryResult.length() + "\n" + queryResult + "\n\n");		
		
		String validationOperation = "";
		boolean validateAskQuery = false;
		
		if (validationParameters.length > 0) {	
			switch (operationType) {
				case INSERT :
					validationOperation = "INSERT";
					validateAskQuery = false;
					validateQuery = new ValidateInsertTemplate(ru, validationQueryTemplates, definitions, validationParameters);
				break;
				case UPDATE : 
					validationOperation = "UPDATE";
					validateAskQuery = false; 
					validateQuery = new ValidateUpdateTemplate(ru, validationQueryTemplates, definitions, validationParameters);
				break;
				case DELETE :
					validationOperation = "DELETE";
					validateAskQuery = true;
					validateQuery = new ValidateDeleteTemplate(ru, validationQueryTemplates, definitions, validationParameters);
				break;
			}			
			
			queryType = validateQuery.getTemplateQueryType();
			queryName = validateQuery.getTemplateFileName();
			queryString = validateQuery.compileMustacheTemplate();

			queryResult = queryExecuteManager.executeQueryWithStringResult(connection, queryName, queryString, queryType, false, closeConnection,
					!validateAskQuery && TestDriver.isFormatTrigstar);

			BRIEF_LOGGER.info(String.format("Query [%s] executed, iteration %d", queryName, iteration));
			LOGGER.info("\n*** Query [" + queryName + "], iteration " + iteration + "\n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryName + "]" + " : \n" + "Length : " + queryResult.length() + "\n" + queryResult + "\n\n");
			
			errors += validateEditorial(queryResult, validationOperation, validateAskQuery, iteration, validationParameters, false);			
		}	

		return errors;	
	}
	

	/**
	 * Duplicates parameter[0] at index 0, update query template requires an extra cwURI
	 * @param parameters - parameters generated druing insert
	 * @return extended parameters array
	 */
	private String[] prepareSubstitutionParametersForValidation(String[] parameters) {
		if (parameters.length > 0) {
			String[] newParameters = new String[parameters.length + 1];
			newParameters[0] = parameters[0];
			
			for (int i = 0; i < parameters.length; i++) {
				newParameters[i + 1] = parameters[i];
			}
			return newParameters;
		}
		return parameters;
	}
	
	private String[] modifySubstitutionParameters(int iteration, String[] parameters) {		
		for (int i = 0; i < parameters.length; i++) {
			//modify strings
			if (parameters[i].contains(STRING_RDF_TYPE)) {
				parameters[i] = buildModifiedString(parameters[i], iteration);
			}
			
			if (parameters[i].contains(DATETIME_RDF_TYPE)) {
				parameters[i] = ru.currentDateTimeString();
			}
		}
		return parameters;
	}
	
	private String buildModifiedString(String inputString, int iteration) {
		StringBuilder sb = new StringBuilder();
		// New implementation of method randomWordFromDictionary of RandomUtil class
		// literals of type <http://www.w3.org/2001/XMLSchema#string> are in ''' instead of quotes.
		String firstPart = inputString.substring(0, 3);
		String secondPart = inputString.substring(3);
		sb.append(firstPart);
		sb.append("_updated_" + iteration);
		sb.append(secondPart);
		
		return sb.toString();
	}
}