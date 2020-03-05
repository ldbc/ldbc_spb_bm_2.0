package eu.ldbc.semanticpublishing.generators.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;

import eu.ldbc.semanticpublishing.generators.data.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import eu.ldbc.semanticpublishing.util.CompressionUtil;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.SesameUtils;

/**
 * A class for generating Creative Works containing correlations between entities.
 * Currently implemented are correlations between two popular entities and a third entity.
 *
 */
public class CorrelationsWorker extends RandomWorker {

	//first entity to participate in the correlation
	private Entity entityA;
	//second entity to participate in the correlation
	private Entity entityB;
	//third entity to participate in a sparse manner to the correlation - it will participate in a separate correlation separately with A and B and very sparsely with A and B during their overlap
	private Entity entityC;
	
	private long firstCwId;
	private List<Integer> correlationsMagnitudesForSingleIterationList;
	private int dataGenerationPeriodYears = 1;
	private int correlationsMagnitude = 10;
	private int totalCorrelationPeriodDays = 1;
	private double correlationEntityLifespanPercent = 0.4;
	private double correlationDurationPercent = 0.1;
	
	//a distance in days between third entity appearance in a correlation
	private static final int THRID_ENTITY_CORRELATION_DISTANCE = 9;
	
	public CorrelationsWorker(RandomUtil ru, Entity entityA, Entity entityB, Entity entityC, long firstCwId, int totalCorrelationPeriodDays, List<Integer> correlationsMagnitudesForSingleIterationList, int dataGenerationPeriodYears, int correlationsMagnitude, 
							  double correlationEntityLifespan, double correlationDuration, Object lock, AtomicLong filesCount, 
							  long totalTriples, long triplesPerFile, AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat, boolean compress, boolean silent) {
		super(ru, lock, filesCount, totalTriples, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);
		this.entityA = entityA;
		this.entityB = entityB;
		this.entityC = entityC;
		this.firstCwId = firstCwId;
		this.totalCorrelationPeriodDays = totalCorrelationPeriodDays;
		this.correlationsMagnitudesForSingleIterationList = correlationsMagnitudesForSingleIterationList;
		this.dataGenerationPeriodYears = dataGenerationPeriodYears;
		this.correlationsMagnitude = correlationsMagnitude;
		this.correlationEntityLifespanPercent = correlationEntityLifespan;
		this.correlationDurationPercent = correlationDuration;
	}

	@Override
	public void execute() throws Exception {
		
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		OutputStream os = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);

		int cwsInFileCount = 0;
		int currentTriplesCount = 0;
		int thirdEntityCountdown = 0;
		int thirdEntityOutsideCorrelationCountdown = 0;
		int correlationsMagnitudeForIteration = this.correlationsMagnitude;
		long currentFilesCount = filesCount.incrementAndGet();		
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
				
		Date startDate;
		int thirdEntityInCorrelationOccurences = (int) ((365 * dataGenerationPeriodYears * correlationDurationPercent) / 10);
		int thirdEntityOutsideCorrelationOccurences = (int) ((365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent * 2 - correlationDurationPercent)) / 10) / 2;
		
		os = new BufferedOutputStream(new FileOutputStream(fileName));
		
		//pick a random date starting from 1.Jan to the value of totalCorrelationPeriodDays
		startDate = ru.randomDateTime(365 * dataGenerationPeriodYears - totalCorrelationPeriodDays);
		thirdEntityCountdown = ru.nextInt((int)(THRID_ENTITY_CORRELATION_DISTANCE * 0.6), THRID_ENTITY_CORRELATION_DISTANCE + 1);
		thirdEntityOutsideCorrelationCountdown = ru.nextInt((int)(THRID_ENTITY_CORRELATION_DISTANCE * 0.6), THRID_ENTITY_CORRELATION_DISTANCE + 1) / 2;
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		try {
			for (int dayIncrement = 0; dayIncrement < totalCorrelationPeriodDays; dayIncrement++) {
				correlationsMagnitudeForIteration = correlationsMagnitudesForSingleIterationList.get(dayIncrement);
				
				boolean thirdEntityForCurrentDaySet = false;				
				
				//generate Creative Works with correlations for that day
				for (int i = 0; i < correlationsMagnitudeForIteration; i++) {
					if (currentTriplesCount >= triplesPerFile) {						
						flushClose(os);
						if (compress) {
							CompressionUtil.compressFile(fileName, true);
						}
						if (!silent && cwsInFileCount > 0) {
							System.out.println(Thread.currentThread().getName() + " " + this.getClass().getSimpleName() + " :: Saving " + (compress ? "compressed " : "") + "file #" + currentFilesCount + " with " + String.format("%,d", cwsInFileCount) + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
						}
	
						cwsInFileCount = 0;
						currentTriplesCount = 0;
						
						currentFilesCount = filesCount.incrementAndGet();
						fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
	
						os = new BufferedOutputStream(new FileOutputStream(fileName));
					}
					
					if (triplesGeneratedSoFar.get() > targetTriples) {
						return;
					}
					
					Model sesameModel = null;
					
					synchronized(lock) {
						if (dayIncrement < 365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent - correlationDurationPercent)) {
							if ((thirdEntityOutsideCorrelationCountdown <= 0) && (thirdEntityOutsideCorrelationOccurences > 0) && !thirdEntityForCurrentDaySet) {
								sesameModel = buildCreativeWorkModel(entityA, entityC, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
								thirdEntityForCurrentDaySet = true;
								thirdEntityOutsideCorrelationOccurences--;
							} else {
								sesameModel = buildCreativeWorkModel(entityA, null, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
							}
						} else if ((dayIncrement >= 365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent - correlationDurationPercent)) && dayIncrement < (365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent))) {
							//reset for the last third of correlation period
							thirdEntityOutsideCorrelationOccurences = (int) ((365 * dataGenerationPeriodYears * (correlationEntityLifespanPercent * 2 - correlationDurationPercent)) / 10) / 2;
							
							if ((thirdEntityCountdown <= 0) && (thirdEntityInCorrelationOccurences > 0) && !thirdEntityForCurrentDaySet) {
								//introduce a third entity correlation in a tiny amount of all correlations
								sesameModel = buildCreativeWorkModel(entityA, entityB, entityC, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
								thirdEntityForCurrentDaySet = true;
								thirdEntityInCorrelationOccurences--;
								thirdEntityCountdown = ru.nextInt((int)(THRID_ENTITY_CORRELATION_DISTANCE * 0.6), THRID_ENTITY_CORRELATION_DISTANCE + 1);
							} else {
								sesameModel = buildCreativeWorkModel(entityA, entityB, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
							}
						} else if (dayIncrement >= 365 * dataGenerationPeriodYears * correlationEntityLifespanPercent) {
							if ((thirdEntityOutsideCorrelationCountdown <= 0) && (thirdEntityOutsideCorrelationOccurences > 0) && !thirdEntityForCurrentDaySet) {
								sesameModel = buildCreativeWorkModel(entityB, entityC, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
								thirdEntityForCurrentDaySet = true;
								thirdEntityOutsideCorrelationOccurences--;
							} else {
								sesameModel = buildCreativeWorkModel(entityB, null, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
							}
						} else {
							sesameModel = buildCreativeWorkModel(entityA, entityC, null, firstCwId++, true, calendar.getTime(), 0, rdfFormat);
							if (!silent) {
								System.out.println(Thread.currentThread().getName() + " :: Warning : Unexpected stage in data generation reached, defaulting");
							}
						}
					}
					
					Rio.write(sesameModel, os, rdfFormat);
										
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();
					
					triplesGeneratedSoFar.addAndGet(sesameModel.size());				
				}
				
				thirdEntityCountdown--;
				thirdEntityOutsideCorrelationCountdown--;
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			
		} catch(RDFHandlerException e) {
			throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
		} finally {
			flushClose(os);
			if (compress) {
				CompressionUtil.compressFile(fileName, true);
			}
			if (!silent && cwsInFileCount > 0) {
				System.out.println(Thread.currentThread().getName() + " " + this.getClass().getSimpleName() + " :: Saving " + (compress ? "compressed " : "") + "file #" + currentFilesCount + " with " + String.format("%,d", cwsInFileCount) + " Creative Works. Generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + ". Target: " + String.format("%,d", targetTriples) + " triples");
			}
		}
	}
	
	private Model buildCreativeWorkModel(Entity a, Entity b, Entity c, long firstCwId, boolean aboutOrMentionsB,
										 Date startDate, int dayIncrement, RDFFormat rdfFormat) {
		CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder(firstCwId, ru);
		creativeWorkBuilder.setDateIncrement(startDate, dayIncrement);
		creativeWorkBuilder.setAboutPresetUri(a.getURI());
		if (b != null) {
			if (aboutOrMentionsB) {
				creativeWorkBuilder.setOptionalAboutPresetUri(b.getURI());
			} else {
				creativeWorkBuilder.setMentionsPresetUri(b.getURI());
			}
		}
		if (c != null) {
			creativeWorkBuilder.setOptionalMentionsPresetUri(c.getURI());
		}
		creativeWorkBuilder.setUsePresetData(true);
		return creativeWorkBuilder.buildSesameModel(rdfFormat);
	}
}