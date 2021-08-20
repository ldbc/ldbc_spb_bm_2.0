package eu.ldbc.semanticpublishing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eu.ldbc.semanticpublishing.agents.HistoryAgent;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection;
import eu.ldbc.semanticpublishing.refdataset.AnalyticsDataManager;
import eu.ldbc.semanticpublishing.resultanalyzers.history.HistoryQueriesUtils;
import eu.ldbc.semanticpublishing.resultanalyzers.history.QueryResultsConverterUtil;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.agents.AbstractAsynchronousAgent;
import eu.ldbc.semanticpublishing.agents.AggregationAgent;
import eu.ldbc.semanticpublishing.agents.EditorialAgent;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryConnection.QueryType;
import eu.ldbc.semanticpublishing.endpoint.SparqlQueryExecuteManager;
import eu.ldbc.semanticpublishing.enterprise.ReplicationAndBackupHelper;
import eu.ldbc.semanticpublishing.generators.data.DataGenerator;
import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.resultanalyzers.CreativeWorksAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.LocationsAnalyzer;
import eu.ldbc.semanticpublishing.resultanalyzers.ReferenceDataAnalyzer;
import eu.ldbc.semanticpublishing.statistics.Statistics;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionParametersGenerator;
import eu.ldbc.semanticpublishing.substitutionparameters.SubstitutionQueryParametersManager;
import eu.ldbc.semanticpublishing.templates.MustacheTemplatesHolder;
import eu.ldbc.semanticpublishing.validation.AggregateOperationsValidator;
import eu.ldbc.semanticpublishing.validation.EditorialOperationsValidator;
import eu.ldbc.semanticpublishing.validation.ValidationValuesManager;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.RdfUtils;
import eu.ldbc.semanticpublishing.util.SesameUtils;
import eu.ldbc.semanticpublishing.util.ShellUtil;
import eu.ldbc.semanticpublishing.util.ThreadUtil;

/**
 * The start point of the semantic publishing test driver. Initializes and runs all parts of the benchmark.
 */
public class TestDriver {
	private final int aggregationAgentsCount;
	private final int editorialAgentsCount;
	private final int warmupPeriodSeconds;
	private final int benchmarkRunPeriodSeconds;
	private final String propertiesFile;
	private final SparqlQueryExecuteManager queryExecuteManager;
	private final AtomicBoolean inBenchmarkState = new AtomicBoolean(false);
	private final AtomicBoolean keepReporterAlive = new AtomicBoolean(false);
	private final AtomicBoolean benchmarkResultIsValid = new AtomicBoolean(false);
	private final AtomicBoolean maxUpdateRateReached = new AtomicBoolean(false);
	private final List<AbstractAsynchronousAgent> aggregationAgents = new ArrayList<>();
	private final List<AbstractAsynchronousAgent> historyAgents = new ArrayList<>();
	private final List<AbstractAsynchronousAgent> editorialAgents = new ArrayList<>();
	private boolean aggregationAgentsStarted = false;
	private boolean editorialAgentsStarted = false;
	private final AtomicBoolean runFlag = new AtomicBoolean(true);

	private final Configuration configuration = new Configuration();
	private final Definitions definitions = new Definitions();
	private final MustacheTemplatesHolder mustacheTemplatesHolder = new MustacheTemplatesHolder();
	private final RandomUtil randomGenerator;
	private final SubstitutionQueryParametersManager substitutionQueryParametersManager = new SubstitutionQueryParametersManager();
	private final ValidationValuesManager validationValuesManager = new ValidationValuesManager();
	private static final String CHECK_HISTORY_PLUGIN_ENABLED_QUERY = "select ?enabled { [] <http://www.ontotext.com/at/enabled> ?enabled }";
	private static final String ENABLE_HISTORY_PLUGIN_QUERY = "insert data { [] <http://www.ontotext.com/at/enabled> true }";
	public static boolean isFormatTrigstar;

	private final static Logger LOGGER = LoggerFactory.getLogger(TestDriver.class.getName());
	private final static Logger RLOGGER = LoggerFactory.getLogger(TestDriverReporter.class.getName());

	public TestDriver(String[] args) throws IOException {

		if( args.length < 1) {
			throw new IllegalArgumentException("Missing parameter - the configuration file must be specified");
		}

		propertiesFile = args[0];
		configuration.loadFromFile(propertiesFile);
		isFormatTrigstar = (SesameUtils.parseRdfFormat(configuration.getString(Configuration.GENERATE_CREATIVE_WORKS_FORMAT)) == RDFFormat.TRIGSTAR);
		definitions.loadFromFile(configuration.getString(Configuration.DEFINITIONS_PATH), configuration.getBoolean(Configuration.VERBOSE));
		mustacheTemplatesHolder.loadFrom(configuration.getString(Configuration.QUERIES_PATH));

		//initialize log4j
		//LoggingUtil.Configure(configuration);

		//will read the dictionary file from jar file as a resource
		randomGenerator = initializeRandomUtil(configuration.getString(Configuration.REFERENCE_DATASETS_PATH), configuration.getLong(Configuration.GENERATOR_RANDOM_SEED), definitions.getInt(Definitions.YEAR_SEED), definitions.getInt(Definitions.DATA_GENERATOR_PERIOD_YEARS));

		//will use initialized randomGenerator above
		definitions.initializeAllocations(randomGenerator.getRandom());

		aggregationAgentsCount = configuration.getInt(Configuration.AGGREGATION_AGENTS_COUNT);
		editorialAgentsCount = configuration.getInt(Configuration.EDITORIAL_AGENTS_COUNT);
		warmupPeriodSeconds = configuration.getInt(Configuration.WARMUP_PERIOD_SECONDS);
		benchmarkRunPeriodSeconds = configuration.getInt(Configuration.BENCHMARK_RUN_PERIOD_SECONDS);


		queryExecuteManager = new SparqlQueryExecuteManager(inBenchmarkState,
				configuration.getString(Configuration.ENDPOINT_URL),
				configuration.getString(Configuration.ENDPOINT_UPDATE_URL),
				configuration.getInt(Configuration.QUERY_TIMEOUT_SECONDS) * 1000,
				configuration.getInt(Configuration.SYSTEM_QUERY_TIMEOUT_SECONDS) * 1000,
				configuration.getBoolean(Configuration.VERBOSE));

		//set the nextId for Creative Works, default 0
		DataManager.creativeWorksNextId.set(configuration.getLong(Configuration.CREATIVE_WORK_NEXT_ID));
	}

	public SparqlQueryExecuteManager getQueryExecuteManager() {
		return queryExecuteManager;
	}

	private RandomUtil initializeRandomUtil(String datasetsPath, long seed, int yearSeed, int generorPeriodYears) {
		//File WordsDictionary.txt is one level up
		String ontPath = FileUtils.normalizePath(datasetsPath);
		String oneLevelUp = ontPath.substring(0, ontPath.lastIndexOf(File.separator) + 1);
		String filePath = oneLevelUp + "dictionaries" + File.separator + "WordsDictionary.txt";

		return new RandomUtil(filePath, seed, yearSeed, generorPeriodYears);
	}

	private void loadOntologies(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading ontologies...");

			String ontologiesPath = FileUtils.normalizePath(configuration.getString(Configuration.ONTOLOGIES_PATH));
			load(ontologiesPath);
		}
	}

	private void adjustRefDatasetsSizes(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Adjusting reference datasets size...");
			int magnitudeOfEntities = 100000;//magnitude in terms of entities used to get from reference dataset
			int avgTriplesPerCw = 19;//average number of triples per Creative Work
			String dpbediaPrefix = "dbpedia";
			String personEntityTypeUri = "http://xmlns.com/foaf/0.1/Person"; // foaf:Person
			String datasetsPath = FileUtils.normalizePath(configuration.getString(Configuration.REFERENCE_DATASETS_PATH));

			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(datasetsPath, collectedFiles, "adjustablettl", true);

			//calculate the amount of triples to be used for reference knowledge. Value is related to current size of the dataset to be generated.
			//triplesLimit = Log10(CreativeWorksCount) * magnitudeTriples
			long entitiesLimit = (long) (Math.log10(configuration.getLong(Configuration.DATASET_SIZE_TRIPLES) / avgTriplesPerCw) * magnitudeOfEntities);

			for( File file : collectedFiles ) {
				if (file.getPath().contains(dpbediaPrefix)) {
					System.out.println("\tAdjusting entities size for file : " + file.getName() + ", entities to be used : " + entitiesLimit);
					RdfUtils.cropDatasetFile(file.getPath(), datasetsPath + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")) + ".adjusted.ttl", entitiesLimit, personEntityTypeUri);
				}
			}
		}
	}

	private void loadDatasets(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading reference datasets...");

			String datasetsPath = FileUtils.normalizePath(configuration.getString(Configuration.REFERENCE_DATASETS_PATH));
			load(datasetsPath);
		}
	}

	private void load(String path) throws IOException {
		String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);

		List<File> collectedFiles = new ArrayList<File>();
		FileUtils.collectFilesList2(path, collectedFiles, "ttl", true);

		Collections.sort(collectedFiles);

		for (File file : collectedFiles) {
			loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TURTLE);
		}
	}

	public void populateRefDataEntitiesLists(boolean showDetails, boolean populateFromDatasetInfoFile, boolean suppressDatasetInfoWarnings, String messagePrefix) throws IOException {

		if (showDetails) {
			System.out.println(messagePrefix + "Analyzing existing reference knowledge in database, it may take a while...");
		}

		if (configuration.getBoolean(Configuration.PRELOAD_ANALYTICAL_QUERY_RESULTS)) {
			System.out.println("Loading analytics from file...");
			long start = System.currentTimeMillis();

			AnalyticsDataManager.loadFromCache(configuration);

			System.out.println("Loading done in " + (System.currentTimeMillis() - start) + "ms");
		} else {
			//retrieve entity URIs from database
			ReferenceDataAnalyzer refDataAnalyzer = new ReferenceDataAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
			ArrayList<Entity> entitiesList = refDataAnalyzer.analyzeEntities();
			splitEntities(entitiesList);

			fillDataManagerCreativeWorkNextId();
			fillDataManagerList(DataManager.locationsIdsList, "analyzelocations.txt");
			fillDataManagerList(DataManager.geonamesIdsList, "analyzegeonames.txt");

			AnalyticsDataManager.storeInCache(configuration);
		}

		//initialize dataset info, required for query parameters
		if (populateFromDatasetInfoFile) {
			initialiseDatasetInfo(suppressDatasetInfoWarnings);
		}

		reportResult(showDetails, messagePrefix);
	}

	private void initialiseDatasetInfo(boolean suppressDatasetInfoWarnings) {
		if ((DataManager.correlatedEntitiesList.size() + DataManager.exponentialDecayEntitiesMinorList.size() + DataManager.exponentialDecayEntitiesMajorList.size()) == 0) {
			String datasetInfoFile = DataManager.buildDataInfoFilePath(configuration);
			if (!datasetInfoFile.isEmpty()) {
				DataManager.initDatasetInfo(datasetInfoFile, suppressDatasetInfoWarnings);
			}
		}
	}

	private void fillDataManagerCreativeWorkNextId() throws IOException {
		long creativeWorksCount = configuration.getLong(Configuration.CREATIVE_WORK_NEXT_ID);
		if (creativeWorksCount > 0) {
			DataManager.creativeWorksNextId.set(creativeWorksCount);
			System.out.println("\tNext id for Creative Works : " + creativeWorksCount);
		} else {
			CreativeWorksAnalyzer cwk = new CreativeWorksAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
			creativeWorksCount = cwk.getResult();
			DataManager.creativeWorksNextId.set(creativeWorksCount);
		}
	}

	private void fillDataManagerList(List<String> listToFill, String query) throws IOException {
		LocationsAnalyzer gna = new LocationsAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		ArrayList<String> locationsIds = gna.collectLocationsIds(query);
		listToFill.addAll(locationsIds);
	}

	public void populateRefDataEntitiesListsFromFiles(boolean showDetails, boolean populateFromDatasetInfoFile, boolean suppressDatasetInfoWarnings, String messagePrefix, String entitiesFullPath, String dbpediaLocationsFullPathName, String geonamesFullPathName) throws IOException {

		if (showDetails) {
			System.out.println(messagePrefix + "Analyzing existing reference knowledge in database from persisted data...");
		}

		if (configuration.getBoolean(Configuration.PRELOAD_ANALYTICAL_QUERY_RESULTS)) {
			System.out.println("Loading analytics from file...");
			long start = System.currentTimeMillis();
			AnalyticsDataManager.loadFromCache(configuration);
			System.out.println("Done in " + (System.currentTimeMillis() - start) + "ms");
		} else {
			//retrieve entity URIs from database
			ReferenceDataAnalyzer refDataAnalyzer = new ReferenceDataAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
			ArrayList<Entity> entitiesList = refDataAnalyzer.initFromFile(entitiesFullPath);
			splitEntities(entitiesList);

			fillDataManagerCreativeWorkNextId();
			fillDataManagerListFromFile(dbpediaLocationsFullPathName, DataManager.locationsIdsList);
			fillDataManagerListFromFile(geonamesFullPathName, DataManager.geonamesIdsList);

			AnalyticsDataManager.storeInCache(configuration);
		}

		//initialize dataset info, required for query parameters
		if (populateFromDatasetInfoFile) {
			initialiseDatasetInfo(suppressDatasetInfoWarnings);
		}

		reportResult(showDetails, messagePrefix);
	}

	private void reportResult(boolean showDetails, String messagePrefix) {
		if (configuration.getBoolean(Configuration.VERBOSE) && showDetails) {
			System.out.println(messagePrefix + "\t(reference data entities size : " + (DataManager.regularEntitiesList.size() +
					DataManager.popularEntitiesList.size()) + ", greatest Creative Work id : " + DataManager.creativeWorksNextId.get() +
					", dbpedia locations : " + DataManager.locationsIdsList.size() + ", geonames locations : " + DataManager.geonamesIdsList.size() + ")");
		}
	}

	private void splitEntities(List<Entity> entitiesList) {
		long popularEntitiesCount = (int) (entitiesList.size() * Definitions.entityPopularity.getAllocationsArray()[0]);
		for (int i = 0; i < entitiesList.size(); i++) {
			Entity e = entitiesList.get(i);
			if (i <= popularEntitiesCount) {
				DataManager.popularEntitiesList.add(e);
			} else {
				DataManager.regularEntitiesList.add(e);
			}
		}
	}

	private void fillDataManagerListFromFile(String dbpediaLocationsFullPathName, List<String> listToFill) throws IOException {
		LocationsAnalyzer gna = new LocationsAnalyzer(queryExecuteManager, mustacheTemplatesHolder);
		ArrayList<String> locationsIds = gna.initFromFile(dbpediaLocationsFullPathName);
		listToFill.addAll(locationsIds);
	}

	public void loadCreativeWorks(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Loading Creative Works...");

			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);

			File[] files = new File(configuration.getString(Configuration.CREATIVE_WORKS_PATH)).listFiles();

			Arrays.sort(files);
			int size=0;
			long startTime = System.currentTimeMillis();
			for( File file : files ) {
				if( file.getName().endsWith(".nq")) {
					loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_SESAME_NQUADS);
					size++;
				} else if( file.getName().endsWith(".ttl")) {
					loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TURTLE);
					size++;
				} else if( file.getName().endsWith(".trig")) {
					loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TRIG);
					size++;
				} else if( file.getName().endsWith(".ttls")) {
					loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TURTLE_STAR);
					size++;
				} else if (file.getName().endsWith(".trigs")) {
					loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TRIG_STAR);
					size++;
				}
			}
			long totalTime = System.currentTimeMillis() - startTime;
			System.out.println("Loaded " + size + " files with Creative Works in " + totalTime + " milliseconds");
		}
	}

	private void loadFiles(String endpoint, File file, String contentType) throws IOException {
		System.out.print("\tloading " + file.getName());
		InputStream input = new FileInputStream(file);

		RdfUtils.postStatements(endpoint, contentType, input);
		System.out.println();
	}

	private void executeScripts(boolean enable, String scriptsSubFolder) {
		if (enable) {
			try {
				String scriptsPath = configuration.getString(Configuration.SCRIPTS_PATH) + File.separator + scriptsSubFolder;
				List<File> scriptFiles = new ArrayList<File>();
				FileUtils.collectFilesList2(scriptsPath, scriptFiles, (FileUtils.isWindowsOS() ? "bat" : "sh"), true);
				Collections.sort(scriptFiles);

				if (scriptFiles.size() > 0) {
					System.out.println("Executing custom scripts (" + scriptsSubFolder + ")...");
				}

				for( File file : scriptFiles ) {
					System.out.println("\texecuting " + scriptsSubFolder + " script: " + file.getName() + " parameters: " + propertiesFile);
					ShellUtil.execute(scriptsPath + " " + propertiesFile, file.getName(), true);
				}
			} catch (NullPointerException npe) {
				System.out.println("Warning : Possible wrong configuration for property 'scriptsPath' (test.properties)...");
//				npe.printStackTrace();
			} catch (IOException | InterruptedException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	private void generateCreativeWorks(boolean enable) throws IOException, InterruptedException {
		if (enable) {
			System.out.println("Generating Creative Works data files...");

			//assuming that if regularEntitiesList is empty, no entity lists were populated
			saveAnalyticalQueryResults(true);

			//if configuration property creativeWorkNextId > 0, use that value for next generated Creative Work. 
			//Use-case : starting data generator in several JVMs to generate data in parallel
			if (configuration.getLong(Configuration.CREATIVE_WORK_NEXT_ID) > 0) {
				DataManager.creativeWorksNextId.set(configuration.getLong(Configuration.CREATIVE_WORK_NEXT_ID));
				System.out.println("\tData generation will start with next Creative Work id : " + DataManager.creativeWorksNextId.get());
			}

			long triplesPerFile = configuration.getLong(Configuration.GENERATED_TRIPLES_PER_FILE);
			long totalTriples = configuration.getLong(Configuration.DATASET_SIZE_TRIPLES);
			String destinationPath = configuration.getString(Configuration.CREATIVE_WORKS_PATH);
			String serializationFormat = configuration.getString(Configuration.GENERATE_CREATIVE_WORKS_FORMAT);

			int generatorThreads = configuration.getInt(Configuration.DATA_GENERATOR_WORKERS);

			DataGenerator dataGenerator = new DataGenerator(randomGenerator, configuration, definitions, generatorThreads, totalTriples, triplesPerFile, destinationPath, serializationFormat);
			dataGenerator.produceData();
		}
	}

	@SuppressWarnings("unchecked")
	public void generateQuerySubstitutionParameters(boolean enable) throws InterruptedException, IOException {
		if (enable) {
			System.out.println("Generating query parameters");

			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
			}

			if (DataManager.creativeWorksNextId.get() == 0) {
				System.out.println("\tNo creative works were found in the database, load creative works first! aborting...");
				return;
			}

			String targetFolder = configuration.getString(Configuration.CREATIVE_WORKS_PATH);
			FileUtils.makeDirectories(targetFolder);

			BufferedWriter bw = null;
			Class<SubstitutionParametersGenerator> c = null;
			Constructor<?> cc = null;
			SubstitutionParametersGenerator queryTemplate = null;
			try {
/*
				//Editorial query parameters
				//Insert
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "insert"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "InsertTemplate"));
				cc = c.getConstructor(String.class, RandomUtil.class, HashMap.class, int.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance("", randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions.getInt(Definitions.YEAR_SEED));			
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();

				//Update
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "update"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "UpdateTemplate"));
				cc = c.getConstructor(String.class, RandomUtil.class, HashMap.class, int.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance("", randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), definitions.getInt(Definitions.YEAR_SEED));
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();

				//Delete
				bw = new BufferedWriter(new FileWriter(new File(targetFolder + File.separator + String.format("%sSubstParameters.txt", "delete"))));
				c = (Class<QueryParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.editorial.%s", "DeleteTemplate"));
				cc = c.getConstructor(RandomUtil.class, HashMap.class);
				queryTemplate = (QueryParametersGenerator) cc.newInstance(randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL));
				queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));
				System.out.print(".");
				bw.close();
*/

				//Aggregate query parameters
				for (int i = 1; i <= Statistics.AGGREGATE_QUERIES_COUNT; i++) {
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFolder + File.separator + String.format("query%01dSubstParameters", i) + ".txt"), "UTF-8"));

					c = (Class<SubstitutionParametersGenerator>) Class.forName(String.format("eu.ldbc.semanticpublishing.templates.aggregation.Query%dTemplate", i));
					cc = c.getConstructor(RandomUtil.class, HashMap.class, Definitions.class, String[].class);
					queryTemplate = (SubstitutionParametersGenerator) cc.newInstance(randomGenerator.randomUtilFactory(configuration.getLong(Configuration.GENERATOR_RANDOM_SEED)), mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), definitions, null);
					queryTemplate.generateSubstitutionParameters(bw, configuration.getInt(Configuration.QUERY_SUBSTITUTION_PARAMETERS));

					bw.close();

					//indicate activity in console
					if (i != Statistics.AGGREGATE_QUERIES_COUNT) {
						System.out.print(".");
					} else {
						System.out.println(".");
					}
				}
				System.out.println("\n");
			} catch (Exception e) {
				System.out.println("\n\tException caught during generation of query substitution parameters : " + e.getClass().getName() + " :: " + e.getMessage());
			} finally {
				try {
					bw.close();
				} catch (Exception e) {
				}
				;
			}
		}
	}

	public void initializeQuerySubstitutionParameters(boolean enable) throws IOException, InterruptedException {
		if (enable) {
			boolean validationPhaseIsEnabled = configuration.getBoolean(Configuration.VALIDATE_QUERY_RESULTS);

			if (!validationPhaseIsEnabled) {
				System.out.println("Initializing query substitution parameters...");
			}
			substitutionQueryParametersManager.intiSubstitutionParameters(configuration.getString(Configuration.CREATIVE_WORKS_PATH), validationPhaseIsEnabled, true);
		}
	}

	public void validateQueryResults(boolean enable) throws Exception {
		if (enable) {

			String validationPath = configuration.getString(Configuration.VALIDATION_PATH);

			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, false, true, "");
			}
			validationValuesManager.initValidationValues(configuration.getString(Configuration.VALIDATION_PATH), false);

			//validate reference data for consistency
			System.out.println("Validating reference data...");

			if (DataManager.checkReferenceDataConsistency()) {
				System.out.println("\t0 errors");
			}

			System.out.println("Validating operations...");

			EditorialOperationsValidator eov = new EditorialOperationsValidator(queryExecuteManager, randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.VALIDATION), configuration, definitions);
			eov.validate();

			//refresh info about reference data and CWs stored in database 
			populateRefDataEntitiesListsFromFiles(false, true, true, "", validationPath + File.separator + "entities.txt", validationPath + File.separator + "dbpediaLocations.txt", validationPath + File.separator + "geonamesIDs.txt");

			AggregateOperationsValidator aov = new AggregateOperationsValidator(this, validationValuesManager, queryExecuteManager, randomGenerator, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), configuration, definitions);
			aov.validate();
		}
	}


	private void setupAsynchronousAgents() {
		for(int i = 0; i < aggregationAgentsCount; ++i ) {
			aggregationAgents.add(new AggregationAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.AGGREGATION), configuration, definitions, substitutionQueryParametersManager, configuration.getLong(Configuration.BENCHMARK_BY_QUERY_MIX_RUNS)));
		}

		for(int i = 0; i < editorialAgentsCount; ++i ) {
			editorialAgents.add(new EditorialAgent(inBenchmarkState, queryExecuteManager, randomGenerator, runFlag, mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.EDITORIAL), mustacheTemplatesHolder.getQueryTemplates(MustacheTemplatesHolder.VALIDATION), configuration, definitions, maxUpdateRateReached));
		}
	}

	private void warmUp(boolean enable) throws IOException {
		if (enable) {
			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialize it with ontologies and reference and generated data first! Exiting.");
					System.exit(-1);
				}
			}

			String message = "Warming up...";

			printAndLog(message);

			aggregationAgentsStarted = true;

			for(int i = 0; i < aggregationAgentsCount; ++i ) {
				aggregationAgents.get(i).start();
			}

			ThreadUtil.sleepSeconds(warmupPeriodSeconds);
		}
	}

	private void benchmark(boolean enable, long benchmarkByQueryMixRuns, long benchmarkByQueryRuns, double mileStonePosition) throws IOException {
		if (enable) {
			if (configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP)) {
				System.out.println("Error : runBenchmark and runBenchmarkWithOnlineReplication phases are both enabled, disable one first!");
				System.exit(-1);
			}

			if (benchmarkByQueryMixRuns > 0 && (definitions.getString(Definitions.QUERY_POOLS).trim().isEmpty() || aggregationAgentsCount <= 0)) {
				System.out.println("Error : incorrect configuration of parameters: 'queryPools' (in definition.properties) and benchmarkByQueryMixRuns, aggregationAgents (in test.properties), exiting...");
				System.exit(-1);
			}

			if (benchmarkByQueryRuns > 0 && aggregationAgentsCount <= 0) {
				System.out.printf("Error : aggregation agents amount : %d is not acceptable for execution of the benchmark in that mode, exiting...%n", aggregationAgentsCount);
				System.exit(-1);
			}

			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialize it with ontologies and reference and generated data first! Exiting.");
					System.exit(-1);
				}
			}

			String message;

			if (benchmarkByQueryMixRuns > 0) {
				message = String.format("Starting the benchmark... (will run until %d query mixes have been executed)", benchmarkByQueryMixRuns);
			} else if (benchmarkByQueryRuns > 0) {
				message = String.format("Starting the benchmark... (will run until %d aggregate executions have been completed)", benchmarkByQueryRuns);
			} else {
				message = "Starting the benchmark...";
			}

			printAndLog(message);

			inBenchmarkState.set(true);
			startAgents();
			Thread reporterThread = startReporterThread(configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_OPS));

			if (benchmarkByQueryMixRuns > 0) {
				while ((Statistics.totalCompletedQueryMixRuns.get() < benchmarkByQueryMixRuns) && (inBenchmarkState.get())) {
					ThreadUtil.sleepMilliseconds(50);
				}
			} else if (benchmarkByQueryRuns > 0) {
				while ((Statistics.totalAggregateQueryStatistics.getRunsCount() < benchmarkByQueryRuns) && (inBenchmarkState.get())) {
					ThreadUtil.sleepMilliseconds(50);
				}
			} else {
				ThreadUtil.sleepSeconds(benchmarkRunPeriodSeconds);
			}

			inBenchmarkState.set(false);

			ThreadUtil.join(reporterThread);

			if (configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_OPS) > 0.0) {
				message = benchmarkResultIsValid.get() ?
						"Benchmark result is valid!" :
						String.format("Warning : Benchmark results are not valid! Required query rate has not been reached, or has dropped below threshold (%.1f ops) during the benchmark run.", configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_OPS));
				printAndLog(message);
			}

			//create an empty file for signaling other drivers (if any) that the benchmark has completed 
			FileUtils.writeToTextFile(TestDriverInterrupter.BENCHMARK_INTERRUPT_SIGNAL, "");

			printAndLog("Stopping the benchmark...");
		}
	}

	private void startAgents() {
		if (!aggregationAgentsStarted) {
			aggregationAgentsStarted = true;
			for (AbstractAsynchronousAgent agent : aggregationAgents) {
				if (!agent.isAlive()) {
					agent.start();
				}
			}
		}

		editorialAgentsStarted = true;
		for (AbstractAsynchronousAgent agent : editorialAgents) {
			agent.start();
		}

		if (configuration.getBoolean(Configuration.VALIDATE_HISTORY_PLUGIN)) {
			createAndStartHistoryAgents();
		}

		Thread interrupterThread = new TestDriverInterrupter(Thread.currentThread(), inBenchmarkState, configuration.getString(Configuration.INTERRUPT_SIGNAL_LOCATION));
		interrupterThread.setDaemon(true);
		interrupterThread.start();
	}

	private Thread startReporterThread(double minUpdateRateThresholdOps) {
		Thread reporterThread = new TestDriverReporter(Statistics.totalAggregateQueryStatistics.getRunsCountAtomicLong(),
				Statistics.totalCompletedQueryMixRuns,
				inBenchmarkState,
				keepReporterAlive,
				benchmarkResultIsValid,
				configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_REACH_TIME_PERCENT),
				minUpdateRateThresholdOps,
				configuration.getDouble(Configuration.MAX_UPDATE_RATE_THRESHOLD_OPS),
				maxUpdateRateReached,
				editorialAgents,
				aggregationAgents,
				historyAgents,
				configuration.getLong(Configuration.BENCHMARK_RUN_PERIOD_SECONDS),
				definitions.getString(Definitions.QUERY_POOLS),
				configuration.getInt(Configuration.CURRENT_RATE_REPORT_PERIOD_SECONDS),
				configuration.getInt(Configuration.REPORT_INTERVAL_SECONDS),
				configuration.getBoolean(Configuration.VERBOSE),
				configuration.getBoolean(Configuration.VALIDATE_HISTORY_PLUGIN));
		reporterThread.setDaemon(true);
		reporterThread.start();
		return reporterThread;
	}

	/**
	 * @param enable 				 - enable the phase
	 * @param benchmarkByQueryRuns   - if zero, then time interval set by parameter 'benchmarkRunPeriodSeconds' will be used for completing the phase.
	 * 								   if greater than zero, then its value the amount of aggregate queries that will be executed for completing the phase.
	 * @param milestonePosition - defines after the position of execution of a 'mileStone' query ( the query that will verify that certain milestone has been reached).
	 * 								   This parameter is considered only if benchmarkByQueryRuns > 0.
	 * 								   e.g. if mileStoneQueryPosition = 0.2 in terms of percents, then after 20% of executed queries a mileStone query is started.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void benchmarkOnlineReplicationAndBackup(boolean enable, long benchmarkByQuryMixRuns, long benchmarkByQueryRuns, double milestonePosition) throws IOException, InterruptedException {
		if (enable) {
			if (configuration.getBoolean(Configuration.RUN_BENCHMARK)) {
				System.out.println("Error : runBenchmark and runBenchmarkWithOnlineReplication phases are both enabled, disable one first!");
				System.exit(-1);
			}

			if (benchmarkByQuryMixRuns > 0) {
				System.out.println("Error : running the benchmark with parameter benchmarkByQuryMixExecutions > 0 is not supported in this phase, run it with benchmarkByQueryRuns > 0 instead...");
				System.exit(-1);
			}

			if (benchmarkByQueryRuns > 0 && aggregationAgentsCount <= 0) {
				System.out.printf("Error : aggregation agents amount : %d is not acceptable for execution of the benchmark in that mode, exiting...%n", aggregationAgentsCount);
				System.exit(-1);
			}

			//assuming that if regularEntitiesList is empty, no entity lists were populated
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.correlatedEntitiesList.size() == 0) {
				populateRefDataEntitiesLists(true, true, false, "");
				if (DataManager.creativeWorksNextId.get() == 0) {
					System.err.println("Warning : no Creative Works were found stored in the database, initialize it with ontologies and reference and generated data first! Exiting.");
					System.exit(-1);
				}
			}

			if (benchmarkByQueryRuns > 0) {
				printAndLog(String.format("Starting the benchmark... (will run until %d aggregate executions have been completed)", benchmarkByQueryRuns));
			} else {
				printAndLog("Warning : The benchmark driver is not configured properly, set a positive value to property 'benchmarkByQueryRuns'. Exiting.");
				System.exit(-1);
			}

			inBenchmarkState.set(true);
			keepReporterAlive.set(true);
			startAgents();
			Thread reporterThread = startReporterThread(0.0);

			String[] milestoneSubstitutionParameters = null;
			ReplicationAndBackupHelper replicationHelper = new ReplicationAndBackupHelper(queryExecuteManager, randomGenerator, configuration, definitions, mustacheTemplatesHolder);
			boolean milestoneQueryExecuted = false;

			try {
				while (Statistics.totalAggregateQueryStatistics.getRunsCount() < benchmarkByQueryRuns) {
					ThreadUtil.sleepMilliseconds(50);

					if (!milestoneQueryExecuted && (Statistics.totalAggregateQueryStatistics.getRunsCount() >= benchmarkByQueryRuns * milestonePosition)) {
						//Milestone point reached, mark it by executing a milestone INSERT query
						printAndLog("Setting a milestone before starting incremental backup...");
						milestoneSubstitutionParameters = replicationHelper.executeMilestoneQuery(1);
						milestoneQueryExecuted = true;

						//Start incremental backup
						logAndExecuteShell("Starting incremental backup (incremental_backup_start)...", ReplicationAndBackupHelper.INCREMENTAL_BACKUP_START);
					}
				}
			} catch (IOException ioe) {
				inBenchmarkState.set(false);
				System.out.println("Warning : Stopping the benchmark : IOExcetion : " + ioe.getMessage());
				throw new IOException(ioe);
			}

			//stop all agents, but keep measuring until database has been restarted and milestone point has been confirmed
			inBenchmarkState.set(false);

			logAndExecuteShell("Shutting down the database (system_shutdown)...", ReplicationAndBackupHelper.SYSTEM_SHUTDOWN);

			//update query timeout value to allow longer timeouts for milestone validation queries. In cases when startup of the database requires extra time to recover.
			replicationHelper.updateQueryExecutionTimeout(48 * 60 * 60 * 1000);

			logAndExecuteShell("Starting up the database (system_startup)...", ReplicationAndBackupHelper.SYSTEM_START);
			printAndLog("Verifying that milestone point exists");

			String message = replicationHelper.validateMilestoneQuery(milestoneSubstitutionParameters) ?
					"\tOK : milestone point found!" :
					"\tError : milestone doesn't exist";
			printAndLog(message);

			keepReporterAlive.set(false);

			printAndLog("Stopping the benchmark...");

			ThreadUtil.join(reporterThread);

			if (configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_OPS) > 0.0) {
				message = benchmarkResultIsValid.get() ?
						"Benchmark result is valid!" :
						String.format("Warning : Benchmark results are not valid! Required query rate has not been reached, or has dropped below threshold (%.1f ops) during the benchmark run.", configuration.getDouble(Configuration.MIN_UPDATE_RATE_THRESHOLD_OPS));
				printAndLog(message);
			}

			logAndExecuteShell("Verifying milestone points...\nShutting down the database (system_shutdown)...", ReplicationAndBackupHelper.SYSTEM_SHUTDOWN);
			logAndExecuteShell("Starting up the database (system_startup)...", ReplicationAndBackupHelper.SYSTEM_START);
			logAndExecuteShell("Restoring state from full backup (before the warmup and benchmarking phases) (full_backup_restore)...", ReplicationAndBackupHelper.FULL_BACKUP_RESTORE);
			logAndExecuteShell("Starting up the database (system_startup)...", ReplicationAndBackupHelper.SYSTEM_START);

			printAndLog("Verifying that current state of the database doesn't contain the milestone point");

			message = replicationHelper.validateMilestoneQuery(milestoneSubstitutionParameters) ?
					"\tError : milestone point found! It shouldn't exist after restoring from full backup taken before the benchmark run." :
					"\tOK : milestone point doesn't exist!";

			printAndLog(message);
		}
	}

	private void logAndExecuteShell(String message, String systemCommand) throws IOException, InterruptedException {
		printAndLog(message);
		ShellUtil.execute(FileUtils.normalizePath(configuration.getString(Configuration.SCRIPTS_PATH) + File.separator + "enterprise"), systemCommand + (FileUtils.isWindowsOS() ? ".bat" : ".sh"), true);
	}

	private void printAndLog(String message) {
		System.out.println(message);
		LOGGER.info(message);
	}

	private void stopAsynchronousAgents() {
		runFlag.set(false);

		if( aggregationAgentsStarted ) {
			for(AbstractAsynchronousAgent agent : aggregationAgents ) {
				ThreadUtil.join(agent);
			}
		}

		if( editorialAgentsStarted ) {
			for(AbstractAsynchronousAgent agent : editorialAgents ) {
				ThreadUtil.join(agent);
			}
		}

		if (configuration.getBoolean(Configuration.VALIDATE_HISTORY_PLUGIN)) {
			for (AbstractAsynchronousAgent history : historyAgents) {
				ThreadUtil.join(history);
			}
		}
	}

	private void checkConformance(boolean enable) throws IOException {
		if (enable) {

			String queriesPath = configuration.getString(Configuration.QUERIES_PATH) + File.separator + "conformance";
			String endpoint = configuration.getString(Configuration.ENDPOINT_UPDATE_URL);

			List<File> collectedFiles = new ArrayList<File>();
			FileUtils.collectFilesList2(queriesPath, collectedFiles, "ttl", true);

			Collections.sort(collectedFiles);

			System.out.println("Preparing for conformance tests...");
			for( File file : collectedFiles ) {
				loadFiles(endpoint, file, RdfUtils.CONTENT_TYPE_TURTLE);
			}

			collectedFiles.clear();

			//Collect Conformance Queries
			List<String> collectedQueries = new ArrayList<String>();
			FileUtils.collectFilesList(queriesPath, collectedQueries, "txt", true);

			Collections.sort(collectedQueries);

			StringBuilder resultSb = new StringBuilder();
			resultSb.append("\n\nTesting conformance capabilities...\n\n");
			for (String filePath : collectedQueries) {
				boolean askQuery = false;
				if (filePath.contains("-ask")) {
					askQuery = true;
				}
				boolean skipReporting = false;
				if (filePath.contains("-skipreporting")) {
					skipReporting = true;
				}

				StringBuilder sb = new StringBuilder();
				String[] queryArray = FileUtils.readTextFile(filePath);
				String constraintTest = queryArray[0].startsWith("#") ? queryArray[0] : "";

				for (int i = 1; i < queryArray.length; i++) {
					if (queryArray[i].trim().startsWith("#")) {
						continue;
					}
					sb.append(queryArray[i]);
				}

				boolean constraintViolationCheckSucceeded = false;
				try {
					if (askQuery) {
						String queryResult = queryExecuteManager.executeQueryWithStringResult(constraintTest, sb.toString(), QueryType.SELECT, RdfUtils.CONTENT_TYPE_RDFXML);
						if (queryResult.toLowerCase().contains("<boolean>true</boolean>")) {
							constraintViolationCheckSucceeded = true;
						}
					} else {
						queryExecuteManager.executeQueryWithStringResult(constraintTest, sb.toString(), QueryType.INSERT, RdfUtils.CONTENT_TYPE_RDFXML);
					}
				} catch (IOException ioe) {
					//deliberately catching IOException from queryExecuteManager as a sign of a successful constraint violation test
					constraintViolationCheckSucceeded = true;
				}

				if (!skipReporting) {
					resultSb.append(String.format("\t%-84s : %s\n", constraintTest, constraintViolationCheckSucceeded ? "success" : "failed"));
				}
			}
			System.out.println(resultSb.toString());
			RLOGGER.info(resultSb.toString());
		}
	}

	public void clearDatabase(boolean enable) throws IOException {
		if (enable) {
			System.out.println("Cleaning up database ...");
			queryExecuteManager.executeQueryWithStringResult("SERVICE-DELETE", " CLEAR ALL ", QueryType.DELETE, RdfUtils.CONTENT_TYPE_RDFXML);
		}
	}

	public void executePhases() throws Exception {
		loadOntologies(configuration.getBoolean(Configuration.LOAD_ONTOLOGIES));
		adjustRefDatasetsSizes(configuration.getBoolean(Configuration.ADJUST_REF_DATASETS_SIZES));
		loadDatasets(configuration.getBoolean(Configuration.LOAD_REFERENCE_DATASETS));
		generateCreativeWorks(configuration.getBoolean(Configuration.GENERATE_CREATIVE_WORKS));
		loadCreativeWorks(configuration.getBoolean(Configuration.LOAD_CREATIVE_WORKS));
		saveAnalyticalQueryResults(configuration.getBoolean(Configuration.SAVE_ANALYTICAL_QUERY_RESULTS));
		executeScripts(configuration.getBoolean(Configuration.LOAD_CREATIVE_WORKS) || configuration.getBoolean(Configuration.VALIDATE_QUERY_RESULTS), "postLoad");
		generateQuerySubstitutionParameters(configuration.getBoolean(Configuration.GENERATE_QUERY_SUBSTITUTION_PARAMETERS));
		initializeQuerySubstitutionParameters(configuration.getBoolean(Configuration.WARM_UP) || configuration.getBoolean(Configuration.RUN_BENCHMARK) || configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP));
		validateQueryResults(configuration.getBoolean(Configuration.VALIDATE_QUERY_RESULTS));
		setupAsynchronousAgents();
		warmUp(configuration.getBoolean(Configuration.WARM_UP));
		benchmark(configuration.getBoolean(Configuration.RUN_BENCHMARK), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_MIX_RUNS), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_RUNS), definitions.getDouble(Definitions.MILESTONE_QUERY_POSITION));
		benchmarkOnlineReplicationAndBackup(configuration.getBoolean(Configuration.RUN_BENCHMARK_ONLINE_REPlICATION_AND_BACKUP), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_MIX_RUNS), configuration.getLong(Configuration.BENCHMARK_BY_QUERY_RUNS), definitions.getDouble(Definitions.MILESTONE_QUERY_POSITION));
		stopAsynchronousAgents();
		checkConformance(configuration.getBoolean(Configuration.CHECK_CONFORMANCE));
		clearDatabase(configuration.getBoolean(Configuration.CLEAR_DATABASE));

		System.out.println("END OF RUN, all agents shut down...");
		System.exit(0);
	}

	private void saveAnalyticalQueryResults(boolean enable) {
		if (enable) {
			if (DataManager.regularEntitiesList.size() == 0 || DataManager.popularEntitiesList.size() == 0 || DataManager.creativeWorksNextId.get() == 0 ||
					DataManager.geonamesIdsList.size() == 0 || DataManager.locationsIdsList.size() == 0) {
				//we can't have saveAnalyticalQueryResults call populateRefDataEntitiesLists whit PRELOAD_ANALYTICAL_QUERY_RESULTS = true
				boolean saved = configuration.getBoolean(Configuration.PRELOAD_ANALYTICAL_QUERY_RESULTS);
				configuration.setProperty(Configuration.PRELOAD_ANALYTICAL_QUERY_RESULTS, "false");
				try {
					populateRefDataEntitiesLists(true, false, true, "");
				} catch (IOException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
				//revert to saved state
				configuration.setProperty(Configuration.PRELOAD_ANALYTICAL_QUERY_RESULTS, Boolean.toString(saved));
			}
		}
	}

	private void checkIfHistoryPluginIsEnabled() {
		SparqlQueryConnection conn = null;
		try {
			conn = new SparqlQueryConnection(queryExecuteManager.getEndpointUrl(), queryExecuteManager.getEndpointUpdateUrl(), RdfUtils.CONTENT_TYPE_RDFXML, queryExecuteManager.getTimeoutMilliseconds(), true);

			if ("false".equalsIgnoreCase(executeHistoryPluginQuery(conn, QueryType.SELECT))) {
				System.out.println("Enabling History plugin.....");
				executeHistoryPluginQuery(conn, QueryType.INSERT);
			}
			if ("true".equalsIgnoreCase(executeHistoryPluginQuery(conn, QueryType.SELECT))) {
				System.out.println("History plugin enabled!");
			}
		} catch (IOException e) {
			System.err.println("Couldn't start History plugin");
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private String executeHistoryPluginQuery(SparqlQueryConnection conn, QueryType queryType) throws IOException {
		boolean selectQuery = queryType == QueryType.SELECT;
		InputStream inputStreamResult = queryExecuteManager.executeQueryWithInputStreamResult(conn, "",
				selectQuery ? CHECK_HISTORY_PLUGIN_ENABLED_QUERY : ENABLE_HISTORY_PLUGIN_QUERY,
				queryType, false, false);

		return selectQuery ? QueryResultsConverterUtil.getBindingSetsList(inputStreamResult).get(0).getValue("enabled").stringValue() : "";
	}

	private void createAndStartHistoryAgents() {
		HistoryQueriesUtils.setHistoryQueriesList(configuration.getString(Configuration.HISTORY_QUERIES));
		checkIfHistoryPluginIsEnabled();
		for (int i = 0; i < aggregationAgentsCount; ++i) {
			AggregationAgent aggregationAgent = (AggregationAgent) aggregationAgents.get(i);
			aggregationAgent.startHistoryValidation();
			HistoryAgent historyAgent = new HistoryAgent(runFlag, aggregationAgent.getPlayedQueries(),
					queryExecuteManager, configuration.getBoolean(Configuration.LOG_FAILED_HISTORY_QUERIES));
			historyAgents.add(historyAgent);
			if (!historyAgent.isAlive()) {
				historyAgent.start();
			}
		}
	}

	public static void showHelp() {
		String helpMsg = "\n\tLDBC Semantic Publishing Benchmark v.2.0";
		helpMsg += "\n\tUsage:";
		helpMsg += "\n\t\t- java -jar <semantic_publishing_benchmark.jar> test.properties - provide a set of configuration options which control the benchmark driver";

		System.out.println(helpMsg);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			showHelp();
			System.exit(0);
		}

		TestDriver testDriver = new TestDriver(args);
		testDriver.executePhases();
	}
}
