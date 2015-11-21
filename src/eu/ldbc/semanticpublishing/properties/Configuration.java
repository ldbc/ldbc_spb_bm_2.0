package eu.ldbc.semanticpublishing.properties;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * A holder for all the benchmark configuration parameters.
 * 
 * A client is expected to instantiate this class, which will provide values
 * (defaults or blank) for all configuration parameters, and then to save this
 * to a file (to create a template configuration file) or to load it from a file
 * (which is the usual case).
 */
public class Configuration {
	
	public static final String ENDPOINT_URL = "endpointURL";
	public static final String ENDPOINT_UPDATE_URL = "endpointUpdateURL";
	public static final String DATASET_SIZE_TRIPLES = "datasetSize";
	public static final String AGGREGATION_AGENTS_COUNT = "aggregationAgents";
	public static final String EDITORIAL_AGENTS_COUNT = "editorialAgents";
	public static final String QUERY_TIMEOUT_SECONDS = "queryTimeoutSeconds";
	public static final String SYSTEM_QUERY_TIMEOUT_SECONDS = "systemQueryTimeoutSeconds";
	public static final String WARMUP_PERIOD_SECONDS = "warmupPeriodSeconds";
	public static final String BENCHMARK_RUN_PERIOD_SECONDS = "benchmarkRunPeriodSeconds";
	public static final String GENERATED_TRIPLES_PER_FILE = "generatedTriplesPerFile";
	public static final String DATA_GENERATOR_WORKERS = "dataGeneratorWorkers";
	public static final String VERBOSE = "verbose";
	
	public static final String LOAD_ONTOLOGIES = "loadOntologies";
	public static final String ONTOLOGIES_PATH = "ontologiesPath";
	public static final String ADJUST_REF_DATASETS_SIZES = "adjustRefDatasetsSizes";
	public static final String LOAD_REFERENCE_DATASETS = "loadReferenceDatasets";
	public static final String REFERENCE_DATASETS_PATH = "referenceDatasetsPath";
	public static final String GENERATE_CREATIVE_WORKS = "generateCreativeWorks";
	public static final String GENERATE_CREATIVE_WORKS_FORMAT = "generateCreativeWorksFormat";
	public static final String CREATIVE_WORKS_PATH = "creativeWorksPath";
	public static final String LOAD_CREATIVE_WORKS = "loadCreativeWorks";
	public static final String WARM_UP = "warmUp";
	public static final String RUN_BENCHMARK = "runBenchmark";
	public static final String CLEAR_DATABASE = "clearDatabase";
	public static final String CHECK_CONFORMANCE = "checkConformance";
	public static final String QUERIES_PATH = "queriesPath";
	public static final String DEFINITIONS_PATH = "definitionsPath";
	public static final String GENERATOR_RANDOM_SEED = "generatorRandomSeed";
	public static final String CREATIVE_WORK_NEXT_ID = "creativeWorkNextId";
	public static final String USE_RANDOM_DATA_GENERATORS = "useRandomDataGenerators";
	public static final String ALLOW_SIZE_ADJUSTMENTS_ON_DATA_MODELS = "allowSizeAdjustmentsOnDataModels";
	public static final String CREATIVE_WORKS_INFO = "creativeWorksInfo";
	public static final String GENERATE_QUERY_SUBSTITUTION_PARAMETERS = "generateQuerySubstitutionParameters";
	public static final String QUERY_SUBSTITUTION_PARAMETERS = "querySubstitutionParameters";
	public static final String VALIDATE_QUERY_RESULTS = "validateQueryResults";
	public static final String VALIDATION_PATH = "validationPath";
	public static final String VALIDATION_ITERATIONS = "validationIterations";
	public static final String BENCHMARK_BY_QUERY_RUNS = "benchmarkByQueryRuns";
	public static final String BENCHMARK_BY_QUERY_MIX_RUNS = "benchmarkByQueryMixRuns";
	public static final String RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP = "runBenchmarkOnlineReplicationAndBackup";
	public static final String SCRIPTS_PATH = "scriptsPath";
	public static final String MIN_UPDATE_RATE_THRESHOLD_OPS = "minUpdateRateThresholdOps";
	public static final String MIN_UPDATE_RATE_THRESHOLD_REACH_TIME_PERCENT = "minUpdateRateThresholdReachTimePercent";
	public static final String MAX_UPDATE_RATE_THRESHOLD_OPS = "maxUpdateRateThresholdOps";
	public static final String ENABLE_LOGS = "enableLogs";
	public static final String ENABLE_EDITORIAL_OPS_VALIDATION = "enableEditorialOpeartionsValidation";
	public static final String EDITORIAL_OPS_VALIDATION_INTEVAL = "editorialOpsValidationInterval";
	public static final String ENABLE_COMPRESSION_ON_GENERATED_DATA = "enableCompressionOnGeneratedData";
	public static final String INTERRUPT_SIGNAL_LOCATION = "interruptSignalLocation";
	public static final String CURRENT_RATE_REPORT_PERIOD_SECONDS = "currentRateReportPeriodSeconds";
	public static final String SAVE_DETAILED_QUERY_LOGS = "saveDetailedQueryLogs";
	
	/**
	 * Initialise and set default values for parameters that make sense.
	 */
	public Configuration() {
		properties.setProperty(ENDPOINT_URL, "" );
		properties.setProperty(ENDPOINT_UPDATE_URL, "" );
		properties.setProperty(DATASET_SIZE_TRIPLES, "" );
		properties.setProperty(AGGREGATION_AGENTS_COUNT, "16" );
		properties.setProperty(EDITORIAL_AGENTS_COUNT, "2" );
		properties.setProperty(QUERY_TIMEOUT_SECONDS, "90" );
		properties.setProperty(SYSTEM_QUERY_TIMEOUT_SECONDS, "3600");
		properties.setProperty(WARMUP_PERIOD_SECONDS, "30" );
		properties.setProperty(BENCHMARK_RUN_PERIOD_SECONDS, "60" );
		properties.setProperty(GENERATED_TRIPLES_PER_FILE, "100000");
		properties.setProperty(DATA_GENERATOR_WORKERS, "1");
		properties.setProperty(VERBOSE, "false" );
		
		properties.setProperty(LOAD_ONTOLOGIES, "true");
		properties.setProperty(ONTOLOGIES_PATH, "./data/ontologies");
		properties.setProperty(ADJUST_REF_DATASETS_SIZES, "false");
		properties.setProperty(LOAD_REFERENCE_DATASETS, "true");
		properties.setProperty(REFERENCE_DATASETS_PATH, "./data/datasets");
		properties.setProperty(GENERATE_CREATIVE_WORKS, "true");
		properties.setProperty(CREATIVE_WORKS_PATH, "");
		properties.setProperty(WARM_UP, "true");
		properties.setProperty(RUN_BENCHMARK, "true");
		properties.setProperty(CLEAR_DATABASE, "false");
		properties.setProperty(CHECK_CONFORMANCE, "false");
		properties.setProperty(QUERIES_PATH, "./data/sparql");
		properties.setProperty(DEFINITIONS_PATH, "./definitions.properties");
		properties.setProperty(GENERATOR_RANDOM_SEED, "0");
		properties.setProperty(CREATIVE_WORK_NEXT_ID, "0");
		properties.setProperty(USE_RANDOM_DATA_GENERATORS, "true");
		properties.setProperty(ALLOW_SIZE_ADJUSTMENTS_ON_DATA_MODELS, "true");
		properties.setProperty(CREATIVE_WORKS_INFO, "");
		properties.setProperty(GENERATE_QUERY_SUBSTITUTION_PARAMETERS, "true");
		properties.setProperty(QUERY_SUBSTITUTION_PARAMETERS, "1000");
		properties.setProperty(VALIDATE_QUERY_RESULTS, "false");
		properties.setProperty(VALIDATION_PATH, "./data/validation");
		properties.setProperty(VALIDATION_ITERATIONS, "1");
		properties.setProperty(BENCHMARK_BY_QUERY_RUNS, "0");
		properties.setProperty(RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP, "false");
		properties.setProperty(SCRIPTS_PATH, "./data/scripts");
		properties.setProperty(MIN_UPDATE_RATE_THRESHOLD_OPS, "0.0");
		properties.setProperty(MIN_UPDATE_RATE_THRESHOLD_REACH_TIME_PERCENT, "0.1");
		properties.setProperty(ENABLE_LOGS, "true");
		properties.setProperty(ENABLE_EDITORIAL_OPS_VALIDATION, "true");
		properties.setProperty(EDITORIAL_OPS_VALIDATION_INTEVAL, "100");
		properties.setProperty(ENABLE_COMPRESSION_ON_GENERATED_DATA, "false");
		properties.setProperty(MAX_UPDATE_RATE_THRESHOLD_OPS, "0.0");
		properties.setProperty(INTERRUPT_SIGNAL_LOCATION, "");
		properties.setProperty(BENCHMARK_BY_QUERY_MIX_RUNS, "0");
		properties.setProperty(CURRENT_RATE_REPORT_PERIOD_SECONDS, "0");
		properties.setProperty(SAVE_DETAILED_QUERY_LOGS, "true");
	}
	
	/**
	 * Load the configuration from the given file (java properties format).
	 * @param filename A readable file on the file system.
	 * @throws IOException
	 */
	public void loadFromFile(String filename) throws IOException {
		
		InputStream input = new FileInputStream(filename);
		try {
			properties.load(input);
		}
		finally {
			input.close();
		}
	}
	
	/**
	 * Save the configuration to a text file (java properties format).
	 * @param filename
	 * @throws IOException
	 */
	public void saveToFile(String filename) throws IOException {
		OutputStream output = new FileOutputStream(filename);
		try {
			properties.store(output, "");
		}
		finally {
			output.close();
		}
	}
	
	/**
	 * Read a configuration parameter's value as a string
	 * @param key
	 * @return
	 */
	public String getString( String key) {
		String value = properties.getProperty(key);
		
		if(value == null) {
			throw new IllegalStateException( "Missing configuration parameter: " + key);
		}
		return value;
	}

	/**
	 * Read a configuration parameter's value as a boolean
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key) {
		String value = getString(key);
		
		if(value.equalsIgnoreCase("true") || value.equals("1") || value.equalsIgnoreCase("y") ) {
			return true;
		}
		if(value.equalsIgnoreCase("false") || value.equals("0") || value.equalsIgnoreCase("n") ) {
			return false;
		}
		throw new IllegalStateException( "Illegal value for boolean configuration parameter: " + key);
	}

	/**
	 * Read a configuration parameter's value as an int
	 * @param key
	 * @return
	 */
	public int getInt(String key) {
		String value = getString(key);
		
		try {
			return Integer.parseInt(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for integer configuration parameter: " + key);
		}
	}

	/**
	 * Read a configuration parameter's value as a long
	 * @param key
	 * @return
	 */
	public long getLong(String key) {
		String value = getString(key);
		
		try {
			return Long.parseLong(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for long integer configuration parameter: " + key);
		}
	}
	
	/**
	 * Read a configuration parameter's value as a double
	 * @param key
	 * @return
	 */
	public double getDouble(String key) {
		String value = getString(key);
		
		try {
			return Double.parseDouble(value);
		}
		catch( NumberFormatException e ) {
			throw new IllegalStateException( "Illegal value for double configuration parameter: " + key);
		}
	}	
	
	private final Properties properties = new Properties();
	
	public static void main(String[] args) throws IOException  {
		Configuration c = new Configuration();
		c.saveToFile("default_config.properties");
	}
}