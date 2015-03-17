package eu.ldbc.semanticpublishing.validation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
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
import eu.ldbc.semanticpublishing.resultanalyzers.sax.SPARQLResultStatementsCounter;
import eu.ldbc.semanticpublishing.resultanalyzers.sesame.RDFXMLResultStatementsCounter;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;

public class AggregateOperationsValidator extends Validator {
	private TestDriver testDriver;
	private ValidationValuesManager validationValuesManager;
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;
	private RandomUtil ru;
	private HashMap<String, String> aggregateQueryTemplates;
	private Configuration configuration;
	private Definitions definitions;
	private RDFXMLResultStatementsCounter rdfxmlResultStatementsCounter;
	private SPARQLResultStatementsCounter sparqlResultStatementsCounter;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(EditorialAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());	
	
	public AggregateOperationsValidator(TestDriver testDriver, ValidationValuesManager validationValuesManager, SparqlQueryExecuteManager queryExecuteManager, RandomUtil ru, HashMap<String, String> aggregateQueryTemplates, Configuration configuration, Definitions definitions) {
		this.testDriver = testDriver;
		this.validationValuesManager = validationValuesManager;
		this.queryExecuteManager = queryExecuteManager;
		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), queryExecuteManager.getTimeoutMilliseconds(), true);
		this.ru = ru;
		this.aggregateQueryTemplates = aggregateQueryTemplates;
		this.configuration = configuration;
		this.definitions = definitions;
		this.rdfxmlResultStatementsCounter = new RDFXMLResultStatementsCounter();
		this.sparqlResultStatementsCounter = new SPARQLResultStatementsCounter();	
	}
	
	@SuppressWarnings("unchecked")
	public void validate() throws Exception {
		System.out.println("\tvalidating AGGREGATE operations...");
		
		//load validation dataset into the database
		loadValidationData();
		
		//refresh statistics
		String validationPath = configuration.getString(Configuration.VALIDATION_PATH);
		testDriver.populateRefDataEntitiesListsFromFiles(true, false, true, "\t", validationPath + File.separator + "entities.txt", validationPath + File.separator + "dbpediaLocations.txt", validationPath + File.separator + "geonamesIDs.txt");

		Class<SubstitutionParametersGenerator> c = null;
		Constructor<?> cc = null;
		MustacheTemplate queryTemplate = null;
		String queryName = "";
		String queryString = "";
		String queryResult = "";
		QueryType queryType;

		for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
			ValidationValuesModel validationValues = validationValuesManager.getValidationValuesFor(i);
			
			c = (Class<SubstitutionParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.aggregation.Query%dTemplate", (i + 1)));
			cc = c.getConstructor(RandomUtil.class, HashMap.class, Definitions.class, String[].class);
			queryTemplate = (MustacheTemplate) cc.newInstance(ru, aggregateQueryTemplates, definitions, validationValues.getSubstitutionParameters());
			
			queryType = queryTemplate.getTemplateQueryType();
			queryName = queryTemplate.getTemplateFileName();
			queryString = queryTemplate.compileMustacheTemplate();			
			
			queryResult = queryExecuteManager.executeQuery(connection, queryName, queryString, queryType, false, true);			

			long actualResultsSize = 0;
			InputStream iStream = null;
					
			try {
				iStream = new ByteArrayInputStream(queryResult.getBytes("UTF-8"));
				
				if ((!queryResult.trim().isEmpty())) {
					if (queryType == QueryType.CONSTRUCT || queryType == QueryType.DESCRIBE) {
						actualResultsSize = rdfxmlResultStatementsCounter.getStatementsCount(iStream);
					} else {
						actualResultsSize = sparqlResultStatementsCounter.getStatementsCount(iStream);
					}
				}
			
				BRIEF_LOGGER.info(String.format("Query [%s] executed, iteration %d, results %d", queryName, (i + 1), actualResultsSize));
				LOGGER.info("\n*** Query [" + queryName + "], iteration " + (i + 1) + ", results " + actualResultsSize + "\n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryName + "]" + " : \n" + "Length : " + queryResult.length() + "\n" + queryResult + "\n\n");
		
				System.out.println(String.format("\tQuery %-1d : ", (i + 1)));
				int errorsForQuery = validateAggregate(queryResult, actualResultsSize, validationValues.getExpectedResultsSize(), "AGGREGATE", (i + 1), validationValues.getValidationResultsList(), false);
				System.out.print(String.format("\t\t%d errors found in %d validation values, at least %d expected results, %d returned results\n", errorsForQuery, validationValues.getValidationResultsList().size(), validationValues.getExpectedResultsSize(), actualResultsSize));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}			
		}
	}
	
	private void loadValidationData() throws IOException {
		System.out.println("Loading Validation Data...");
		
		String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);
		
		File[] files = new File(configuration.getString(Configuration.VALIDATION_PATH)).listFiles();
				
		int processedNQfiles = 0;
		Arrays.sort(files);
		for( File file : files ) {
			if( file.getName().endsWith(".nq")) {
				InputStream input = new FileInputStream(file);
				RdfUtils.postStatements(endpoint, RdfUtils.CONTENT_TYPE_SESAME_NQUADS, input);
				processedNQfiles++;
			}
		}
		
		if (processedNQfiles == 0) {
			System.out.println("\t\tNo validation data files (*.nq) found at : " + configuration.getString(Configuration.VALIDATION_PATH));
		}
	}
}
