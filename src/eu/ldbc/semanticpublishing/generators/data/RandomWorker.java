package eu.ldbc.semanticpublishing.generators.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;

import eu.ldbc.semanticpublishing.generators.data.sesamemodelbuilders.CreativeWorkBuilder;
import eu.ldbc.semanticpublishing.util.CompressionUtil;
import eu.ldbc.semanticpublishing.util.RandomUtil;
import eu.ldbc.semanticpublishing.util.SesameUtils;

/**
 * A class for generating Creative Works using components for serializing from the Sesame. 
 *
 */
public class RandomWorker extends AbstractAsynchronousWorker {
	
	protected long targetTriples;
	protected long triplesPerFile;
	protected long totalTriplesForWorker;
	protected String destinationPath;
	protected String serializationFormat;
	protected AtomicLong filesCount;
	protected AtomicLong triplesGeneratedSoFar;
	protected RandomUtil ru;
	protected Object lock;
	protected boolean compress;
	protected boolean silent;
	
	public RandomWorker(RandomUtil ru, Object lock, AtomicLong filesCount, long totalTriples, long triplesPerFile, AtomicLong triplesGeneratedSoFar, String destinationPath, String serializationFormat, boolean compress, boolean silent) {
		this.ru = ru;
		this.lock = lock;
		this.targetTriples = totalTriples;
		this.filesCount = filesCount;
		this.triplesPerFile = triplesPerFile;
		this.triplesGeneratedSoFar = triplesGeneratedSoFar;
		this.destinationPath = destinationPath;
		this.serializationFormat = serializationFormat;
		this.compress = compress;
		this.silent = silent;
	}
	
	@Override
	public void execute() throws Exception {
		OutputStream os = null;
		RDFFormat rdfFormat = SesameUtils.parseRdfFormat(serializationFormat);

		long currentFilesCount = filesCount.incrementAndGet();
		String fileName = String.format(FILENAME_FORMAT + rdfFormat.getDefaultFileExtension(), destinationPath, File.separator, currentFilesCount);
		
		int cwsInFileCount = 0;
		int currentTriplesCount = 0;
		
		//skip data generation if targetTriples size has already been reached 
		if (triplesGeneratedSoFar.get() > targetTriples) {
//			System.out.println(Thread.currentThread().getName() + " :: generated triples so far: " + String.format("%,d", triplesGeneratedSoFar.get()) + " have reached the targeted triples size: " + String.format("%,d", targetTriples) + ". Generating is cancelled");
			return;
		}
		
		//loop until the generated triples have reached the targeted totalTriples size
		while (true) {
			if (triplesGeneratedSoFar.get() > targetTriples) {					
				break;
			}
			
			try {
				os = new BufferedOutputStream(new FileOutputStream(fileName));
				
				Model sesameModel;

				while (true) {
					if (currentTriplesCount > triplesPerFile) {
						break;
					}					

					if (triplesGeneratedSoFar.get() > targetTriples) {					
						break;
					}
					
					//using a synchronized block, to guarantee the exactly equal generated data no matter the number of threads
					synchronized(lock) {							
						CreativeWorkBuilder creativeWorkBuilder = new CreativeWorkBuilder("", ru);
						sesameModel = creativeWorkBuilder.buildSesameModel(rdfFormat);
					}
					
					Rio.write(sesameModel, os, rdfFormat);
					
					cwsInFileCount++;
					currentTriplesCount += sesameModel.size();											
					triplesGeneratedSoFar.addAndGet(sesameModel.size());					
				}
				
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
			} catch (RDFHandlerException e) {
				flushClose(os);
				if (compress) {
					CompressionUtil.compressFile(fileName, true);
				}
				throw new IOException("A problem occurred while generating RDF data: " + e.getMessage());
			}
		}
	}
	
	protected synchronized void flushClose(OutputStream fos) throws IOException {
		if (fos != null) {
			fos.flush();
			fos.close();
		}
	}	
}
