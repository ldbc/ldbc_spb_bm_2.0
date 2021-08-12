package eu.ldbc.semanticpublishing.enterprise;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.TestDriver;
import eu.ldbc.semanticpublishing.agents.EditorialAgent;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;
import eu.ldbc.semanticpublishing.templates.editorial.InsertTemplate;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.validation.EditorialOperationsValidator;

/**
 * Class used to test replication and backup features of RDF Stores
 *
 */
public class ReplicationAndBackupHelper {
	private SparqlQueryExecuteManager queryExecuteManager;
	private SparqlQueryConnection connection;
	private MustacheTemplatesHolder mustacheTemplatesHolder;
	private RandomUtil ru;
	private Configuration configuration;
	private Definitions definitions;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(EditorialAgent.class.getName());
	private final static Logger BRIEF_LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	
	public final static String INCREMENTAL_BACKUP_START = "incremental_backup_start";
	public final static String SYSTEM_START = "system_start";
	public final static String SYSTEM_SHUTDOWN = "system_shutdown";
	public final static String FULL_BACKUP_START = "full_backup_start";
	public final static String FULL_BACKUP_RESTORE = "full_backup_restore";
	
	public ReplicationAndBackupHelper(SparqlQueryExecuteManager queryExecuteManager, RandomUtil ru, Configuration configuration, Definitions definitions, MustacheTemplatesHolder mustacheTemplatesHolder) {
		this.queryExecuteManager = queryExecuteManager;
		this.ru = ru;
		this.configuration = configuration;
		this.definitions = definitions;
		this.mustacheTemplatesHolder = mustacheTemplatesHolder;
	}
	
	public String[] changeToMilestoneQueryParameters(InsertTemplate insertTemplate, int milestoneID) throws IOException {
		String[] parameters = insertTemplate.generateSubstitutionParameters(null, 1).split(SubstitutionParametersGenerator.PARAMS_DELIMITER);
		
		if (parameters.length > 3) {
			parameters[0] = String.format("<http://www.bbc.co.uk/context/milestone-%d>", milestoneID);
			parameters[1] = parameters[0].replace("/context/", "/things/");
		}
		
		return parameters;
	}
	
	/**
	 * Executes INSERT query with specific values that are unique for current milestone point
	 * @param queryParameters - 
	 * @throws IOException 
	 */
	public String[] executeMilestoneQuery(int milestoneID) throws IOException {
		String queryName = "";
		String queryString = "";
		String queryResult = "";
		QueryType queryType;

		this.connection = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);
		
		InsertTemplate insertTemplate = new InsertTemplate("", ru, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions, false, null);

		String queryParameters[] = changeToMilestoneQueryParameters(insertTemplate, 1);
		
		insertTemplate = new InsertTemplate("", ru, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions, false, queryParameters);
		
		queryType = insertTemplate.getTemplateQueryType();
		queryName = insertTemplate.getTemplateFileName();
		queryString = insertTemplate.compileMustacheTemplate();		

		queryResult = queryExecuteManager.executeQueryWithStringResult(connection, queryName, queryString, queryType, false, true);
		
		BRIEF_LOGGER.info(String.format("Milestone Query [%s] executed", queryName));
		LOGGER.info("\n*** Milestone Query [" + queryName + "], \n" + queryString + "\n---------------------------------------------\n*** Result for query [" + queryName + "]" + " : \n" + "Length : " + queryResult.length() + "\n" + queryResult + "\n\n");
		
		return queryParameters;
	}
	
	public boolean validateMilestoneQuery(String[] queryParameters) throws IOException {

		EditorialOperationsValidator eov = new EditorialOperationsValidator(queryExecuteManager, ru, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.VALIDATION), configuration, definitions);
		int errors = eov.validateAction(EditorialOperationsValidator.EditorialOperation.INSERT, 0, queryParameters, true);

		return (errors == 0);
	}
	
	public void updateQueryExecutionTimeout(int milliseconds) {
		queryExecuteManager.setTimeoutsMilliseconds(milliseconds);
	}
}
