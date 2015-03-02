package eu.ldbc.semanticpublishing.generators.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.properties.Definitions;
import eu.ldbc.semanticpublishing.refdataset.DataManager;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.ExponentialDecayNumberGeneratorUtil;
import eu.ldbc.semanticpublishing.util.FileUtils;
import eu.ldbc.semanticpublishing.util.RandomUtil;

/**
 * The class responsible for managing data generation for the benchmark.
 * It is the entry point for any data generation related process.  
 *
 */
public class DataGenerator {
	private RandomUtil ru;
	private Configuration configuration;
	private Definitions definitions;
	private int generatorThreads = 1;
	private long triplesPerFile;
	private long targetedTriplesSize;
	private AtomicLong filesCount = new AtomicLong(0);
	private AtomicLong triplesGeneratedSoFar = new AtomicLong(0);
	private String destinationPath;
	private String serializationFormat;
	private static final long AWAIT_PERIOD_HOURS = 168; 
	private Object syncLock;
	
	//defines quotient for major events for 1M triples - number of major events per million triples
	private static final double EXP_DECAY_MAJOR_EVENTS_QT = 0.1;
	//defines quotient for minor events for 1M triples - number of minor events per million triples
	private static final double EXP_DECAY_MINOR_EVENTS_QT = 2.2;
	//defines quotient for correlations for 1M triples - number of correlations per million triples
	private static final double CORRELATIONS_QT = 1.3;
	
	public DataGenerator(RandomUtil ru, Configuration configuration, Definitions definitions, int generatorThreads, long totalTriples, long triplesPerFile, String destinationPath, String serializationFormat) {
		this.ru = ru;
		this.configuration = configuration;
		this.definitions = definitions;
		this.generatorThreads = generatorThreads;
		this.targetedTriplesSize = totalTriples;
		this.triplesPerFile = triplesPerFile;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.syncLock = this;
	}
	
	public void produceData() throws InterruptedException, IOException {
		produceData(true, true, true, true, false);		
	}
	
	public void produceData(boolean produceRandom, boolean produceClusterings, boolean produceCorrelations, boolean persistDatasetInfo, boolean silent) throws InterruptedException, IOException  {
		long creativeWorksInDatabase = DataManager.creativeWorksNextId.get();
		
		List<Entity> correlatedEntitiesList = null;
		List<Entity> expDecayingMajorEntitiesList = null;
		List<Entity> expDecayingMinorEntitiesList = null;
		
		if (creativeWorksInDatabase > 0) {
			System.out.println("\t" + creativeWorksInDatabase + " Creative Works currently exist.");
		}

		//Adjust the amount of correlations and clusterings, in relation to the targeted triples size, keeping ratio of 1/3 for each of the motellings in generated data
		if (configuration.getBoolean(Configuration.ALLOW_SIZE_ADJUSTMENTS_ON_DATA_MODELS)) {
			adjustDataAllocations(targetedTriplesSize, definitions, definitions.getInt(Definitions.DATA_GENERATOR_PERIOD_YEARS));
		}
		
		//create destination directory
		FileUtils.makeDirectories(this.destinationPath);
		
		//compress output?
		boolean compress = configuration.getBoolean(Configuration.ENABLE_COMPRESSION_ON_GENERATED_DATA);
		
		ExecutorService executorService = null;
		executorService = Executors.newFixedThreadPool(generatorThreads);
		
		long spawnedRuSeed = ru.getSeed() + 1;
		long nextCwId = DataManager.creativeWorksNextId.get();
		
		long currentTime = System.currentTimeMillis();

		//Generate Correlations between entities
		int correlationsAmount = definitions.getInt(Definitions.CORRELATIONS_AMOUNT);
		
		if (produceCorrelations && correlationsAmount > 0) {
			correlatedEntitiesList = buildCorrelationsList(correlationsAmount);
			
			for (int i = 0; i < (correlatedEntitiesList.size() / 3);i++) {
				RandomUtil spawnedRu = ru.randomUtilFactory(spawnedRuSeed++);
				
				Entity entityA = correlatedEntitiesList.get(i * 3);
				Entity entityB = correlatedEntitiesList.get(i * 3 + 1);
				Entity entityC = correlatedEntitiesList.get(i * 3 + 2);		

				long generatedCWsByWorker = 0; 
				int dataGeneratorPeriodYears = 1; //assuming that a correlation between entities will exist no longer than one year
				int correlationsMagnitude = definitions.getInt(Definitions.CORRELATIONS_MAGNITUDE);
				double correlationEntityLifespanPercent = definitions.getDouble(Definitions.CORRELATION_ENTITY_LIFESPAN);
				double correlationDurationPercent = definitions.getDouble(Definitions.CORRELATIONS_DURATION);
				int totalCorrelationPeriodDays = (int) (365 * dataGeneratorPeriodYears * (correlationEntityLifespanPercent * 2 - correlationDurationPercent));

				//initialize a list of correlations magnitudes for each day
				List<Integer> correlationsMagnitudesList = new ArrayList<Integer>();
				for (int j = 0; j < totalCorrelationPeriodDays; j++) {
					int nextRandom = spawnedRu.nextInt((int)(correlationsMagnitude * 0.75), correlationsMagnitude + 1);
					generatedCWsByWorker += nextRandom;
					correlationsMagnitudesList.add(nextRandom);
				}
				
				nextCwId = DataManager.creativeWorksNextId.incrementAndGet();				
				DataManager.creativeWorksNextId.addAndGet(generatedCWsByWorker - 1);				
				CorrelationsWorker crw = new CorrelationsWorker(spawnedRu, entityA, entityB, entityC, nextCwId,  totalCorrelationPeriodDays, correlationsMagnitudesList, dataGeneratorPeriodYears, 
															    correlationsMagnitude, correlationEntityLifespanPercent, correlationDurationPercent, syncLock, 
															    filesCount, targetedTriplesSize, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);
				executorService.execute(crw);
			}
		}
		
		ExponentialDecayNumberGeneratorUtil edgu;

		int exponentialDecayUpperLimitOfCws = definitions.getInt(Definitions.EXPONENTIAL_DECAY_UPPER_LIMIT_OF_CWS);
		
		//preinitialize
		if (definitions.getInt(Definitions.MAJOR_EVENTS) > 0) {
			expDecayingMajorEntitiesList = new ArrayList<Entity>();
			
			for (int i = 0; i < definitions.getInt(Definitions.MAJOR_EVENTS); i++) {
				Entity e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
				expDecayingMajorEntitiesList.add(e);				
			}			
		}
		
		if (definitions.getInt(Definitions.MINOR_EVENTS) > 0) {
			expDecayingMinorEntitiesList = new ArrayList<Entity>();
			
			for (int i = 0; i < definitions.getInt(Definitions.MINOR_EVENTS); i++) {
				Entity e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
				expDecayingMinorEntitiesList.add(e);
			}			
		}
		
		//Generate MAJOR EVENTS with exponential decay
		if (produceClusterings && definitions.getInt(Definitions.MAJOR_EVENTS) > 0) {			
			for (int i = 0; i < definitions.getInt(Definitions.MAJOR_EVENTS); i++) {
				edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000, */exponentialDecayUpperLimitOfCws, 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
				RandomUtil spawnedRu = ru.randomUtilFactory(spawnedRuSeed++);
				Date startDate = spawnedRu.randomDateTime();
				Entity e = expDecayingMajorEntitiesList.get(i);
			
				nextCwId = DataManager.creativeWorksNextId.incrementAndGet();
				DataManager.creativeWorksNextId.addAndGet(edgu.calculateTotal() - 1);
				ExpDecayWorker edw = new ExpDecayWorker(edgu.produceIterationStepsList(), nextCwId, startDate, e, spawnedRu, syncLock, filesCount, triplesPerFile, targetedTriplesSize, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);
				executorService.execute(edw);				
			}
		}

		//Generate MINOR EVENTS with exponential decay
		if (produceClusterings && definitions.getInt(Definitions.MINOR_EVENTS) > 0) {			
			for (int i = 0; i < definitions.getInt(Definitions.MINOR_EVENTS); i++) {
				edgu =  new ExponentialDecayNumberGeneratorUtil(/*ru.nextInt(1000,*/ exponentialDecayUpperLimitOfCws / 10, 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_RATE), 
							  									definitions.getDouble(Definitions.EXPONENTIAL_DECAY_THRESHOLD_PERCENT));
				RandomUtil spawnedRu = ru.randomUtilFactory(spawnedRuSeed++);
				Date startDate = spawnedRu.randomDateTime();
				Entity e = expDecayingMinorEntitiesList.get(i);
				
				nextCwId = DataManager.creativeWorksNextId.incrementAndGet();
				DataManager.creativeWorksNextId.addAndGet(edgu.calculateTotal() - 1);
				ExpDecayWorker edw = new ExpDecayWorker(edgu.produceIterationStepsList(), nextCwId, startDate, e, spawnedRu, syncLock, filesCount, triplesPerFile, targetedTriplesSize, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);				
				executorService.execute(edw);
			}
		}

		//reset allocations back to initial state by setting back the initial random generator (CreativeWorksBuilder constructor will change random generator with each new instance)
		Definitions.reconfigureAllocations(ru.getRandom());

		//Generate random Creative Works to fill-in with rest of the generated data with randomly distributed tags of creative works, i.e. generate "noise"
		if (configuration.getBoolean(Configuration.USE_RANDOM_DATA_GENERATORS) == false) {
			System.out.println("* Skipping execution of GeneralWorkers in data generation, see test.properties parameter: useRandomDataGenerators");
		}
		if (produceRandom && (triplesGeneratedSoFar.get() < targetedTriplesSize) && configuration.getBoolean(Configuration.USE_RANDOM_DATA_GENERATORS)) {
			for (int i = 0; i < generatorThreads; i++) {				
				RandomWorker rw = new RandomWorker(ru, syncLock, filesCount, targetedTriplesSize, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);
				executorService.execute(rw);
			}
		}		
		executorService.shutdown();
		executorService.awaitTermination(AWAIT_PERIOD_HOURS, TimeUnit.HOURS);		
		
		//persist information about generated dataset
		String persistFilePath = DataManager.buildDataInfoFilePath(configuration);
		if (persistDatasetInfo && !persistFilePath.isEmpty()) {			
			DataManager.persistDatasetInfo(persistFilePath, correlatedEntitiesList, expDecayingMajorEntitiesList, expDecayingMinorEntitiesList);
		}
		
		System.out.println("\tcompleted! Total Creative Works created : " + String.format("%,d", (DataManager.creativeWorksNextId.get() - creativeWorksInDatabase)) + ". Time : " + (System.currentTimeMillis() - currentTime) + " ms");		
	}
	
	private synchronized ArrayList<Entity> buildCorrelationsList(int correlationsAmount) {
		ArrayList<Entity> arrayList = new ArrayList<Entity>();
		
		for (int i = 0; i < correlationsAmount; i++) {			
			//First main entity in correlation
			Entity e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			arrayList.add(e);
			//Second main entity in correlation
			e = DataManager.popularEntitiesList.get(ru.nextInt(DataManager.popularEntitiesList.size()));
			arrayList.add(e);
			//Third entity which participates sparsely in the correlation period
			e = DataManager.regularEntitiesList.get(ru.nextInt(DataManager.regularEntitiesList.size()));
			arrayList.add(e);
		}
		
		return arrayList;
	}
	
	/**
	 * The method would dynamically 'adjust' the amount of generated data with Clusterings, Correlations and Random Workers depending on the 
	 * targeted triples size. Currently ratio of the three types of modeled data (clusterings, correlations, random) is intended to be 33% : 33% : 33%
	 * 
	 * @param targetTriples - targeted triples size to be generated
	 * @param definitions - definitions properties
	 */
	private void adjustDataAllocations(long targetTriples, Definitions definitions, int dataGeneratorPeriodYears) {
		long majorEvents = (int)(EXP_DECAY_MAJOR_EVENTS_QT * (targetedTriplesSize / 1000000)) > 0 ? (int)(EXP_DECAY_MAJOR_EVENTS_QT * (targetedTriplesSize / 1000000)) : 0;
		long minorEvents = (int)(EXP_DECAY_MINOR_EVENTS_QT * (targetedTriplesSize / 1000000)) > 0 ? (int)(EXP_DECAY_MINOR_EVENTS_QT * (targetedTriplesSize / 1000000)) : 0;
		long correlations = (int)(CORRELATIONS_QT * (targetedTriplesSize / 1000000)) > 0 ? (int)(CORRELATIONS_QT * (targetedTriplesSize / 1000000)) : 0;
		
		definitions.setLong(Definitions.MAJOR_EVENTS, majorEvents);
		definitions.setLong(Definitions.MINOR_EVENTS, minorEvents);
		definitions.setLong(Definitions.CORRELATIONS_AMOUNT, correlations);
	}
}
