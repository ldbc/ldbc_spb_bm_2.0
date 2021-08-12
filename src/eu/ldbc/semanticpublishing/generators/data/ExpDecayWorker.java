package eu.ldbc.semanticpublishing.generators.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
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
 * Each worker will be responsible for generating data for a single "phenomenon" in datasets.
 * e.g. starts with a given date and using exponential decay will produce Creative Works which will tag in
 * their about tags certain entity (URI)
 *
 */
public class ExpDecayWorker extends RandomWorker {
	private List<Long> exponentialDecayIerations;
	private Date startDate;
	private Entity entity;
	private long firstCwId;
	
	public ExpDecayWorker(List<Long> exponentialDecayIerations, long firstCwId, Date startDate, Entity entity, 
						  RandomUtil ru, Object lock, AtomicLong globalFilesCount, long triplesPerFile, long totalTriples, 
						  AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat, boolean compress, boolean silent) {
		super(ru, lock, globalFilesCount, totalTriples, triplesPerFile, triplesGeneratedSoFar, destinationPath, serializationFormat, compress, silent);
		this.exponentialDecayIerations = exponentialDecayIerations;
		this.startDate = startDate;
		this.entity = entity;
		this.firstCwId = firstCwId;
	}
	
	@Override
	public void execute() throws Exception {
		OutputStream os = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);

		int cwsInFileCount = 0;
		int currentTriplesCount = 0;

		long currentFilesCount = filesCount.incrementAndGet();
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
				
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		long creativeWorksForCurrentIteration = 0;
		long iterationStep = 0;
		
		try {
			os = new BufferedOutputStream(new FileOutputStream(fileName));

			for (int i = 0; i < exponentialDecayIerations.size(); i++) {
				creativeWorksForCurrentIteration = exponentialDecayIerations.get(i);
				iterationStep = i + 1;
				for (int j = 0; j < creativeWorksForCurrentIteration; j++) {
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
					
					Model sesameModel;
					
					synchronized (lock) {	
						CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder(firstCwId++, ru);
						creativeWorkBuilder.setDateIncrement(startDate, (int)iterationStep);
						creativeWorkBuilder.setAboutPresetUri(entity.getURI());
						creativeWorkBuilder.setUsePresetData(true);
						sesameModel = creativeWorkBuilder.buildSesameModel();												
					}
					
					Rio.write(sesameModel, os, rdfFormat);
										
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();					

					triplesGeneratedSoFar.addAndGet(sesameModel.size());					
				}
			}
		} catch (RDFHandlerException e) {
			throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
		} catch (NoSuchElementException nse) {
			//reached the end of iteration, close file stream in finally section
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
}