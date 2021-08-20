package eu.ldbc.semanticpublishing.refdataset;

import eu.ldbc.semanticpublishing.properties.Configuration;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class AnalyticsDataManager {

	public static void storeInCache(Configuration configuration) {
		File file = getFile(configuration);
		if (file.exists()) {
			try (FileOutputStream fileOutputStream = new FileOutputStream(file);
				 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {

				objectOutputStream.writeObject(DataManager.creativeWorksNextId);
				objectOutputStream.writeObject(DataManager.popularEntitiesList);
				objectOutputStream.writeObject(DataManager.regularEntitiesList);
				objectOutputStream.writeObject(DataManager.geonamesIdsList);
				objectOutputStream.writeObject(DataManager.locationsIdsList);

			} catch (IOException e) {
				System.err.println("Could not store data" + e.getMessage());
			}
		}
	}

	public static void loadFromCache(Configuration configuration) {
		File file = getFile(configuration);
		if (file.exists()) {
			try (FileInputStream fileInputStream = new FileInputStream(file);
				 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {

				DataManager.creativeWorksNextId = (AtomicLong) objectInputStream.readObject();
				loadList(objectInputStream, DataManager.popularEntitiesList);
				loadList(objectInputStream, DataManager.regularEntitiesList);
				loadList(objectInputStream, DataManager.geonamesIdsList);
				loadList(objectInputStream, DataManager.locationsIdsList);
			} catch (IOException | ClassNotFoundException e) {
				System.err.println("Could not load saved data" + e.getMessage());
			}
		}
	}

	private static <E> void loadList(ObjectInputStream objectInputStream, List<E> workList) throws IOException, ClassNotFoundException {
		workList.clear();
		workList.addAll((ArrayList<E>) objectInputStream.readObject());
	}

	private static File getFile(Configuration configuration) {
		File dataDir = Paths.get(configuration.getString(Configuration.CREATIVE_WORKS_PATH)).toFile();
		String fileName = configuration.getString(Configuration.ANALYTICAL_QUERY_RESULTS_FILE_NAME);
		File file = new File(dataDir, fileName);
		if (!dataDir.exists()) {
			try {
				org.apache.commons.io.FileUtils.forceMkdir(dataDir);
				file.createNewFile();
			} catch (IOException e) {
				System.err.println("Could not create directory " + dataDir + e.getMessage());
			}
		}
		return file;
	}
}
