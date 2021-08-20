package eu.ldbc.semanticpublishing.refdataset;

import eu.ldbc.semanticpublishing.properties.Configuration;
import eu.ldbc.semanticpublishing.refdataset.model.Entity;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestAnalyticDataManager {

	private final long creativeWorks = 1234567890L;
	private final Entity event = new Entity("<http://dbpedia.org/resource/Bernard_Lambourde>", "Bernard Lambourde", "Bernard Lambourde", "Persons", "0.89");
	private final Entity person = new Entity("<http://dbpedia.org/resource/Blackpool_Council_election,_2011>", "Blackpool Council election, 2011", "Blackpool Council election, 2011", "Event", "1.30");
	private final List<String> expectedLocations1 = new ArrayList<>();
	private final List<String> expectedLocations2 = new ArrayList<>();
	private final List<Entity> expectedEntities1 = new ArrayList<>();
	private final List<Entity> expectedEntities2 = new ArrayList<>();
	private final Path DIRECTORY_PLACE = Paths.get("./temp");

	private final Configuration conf = new Configuration();

	@Before
	public void bootUp() {
		DataManager.creativeWorksNextId.set(creativeWorks);

		fillList(expectedEntities1, event, person);
		DataManager.popularEntitiesList.addAll(expectedEntities1);

		fillList(expectedEntities2, person, event);
		DataManager.regularEntitiesList.addAll(expectedEntities2);

		fillList(expectedLocations1, "Sofia", "London");
		DataManager.geonamesIdsList.addAll(expectedLocations1);

		fillList(expectedLocations2, "London", "Sofia");
		DataManager.locationsIdsList.addAll(expectedLocations2);

		conf.setProperty(Configuration.CREATIVE_WORKS_PATH, DIRECTORY_PLACE.toString());
	}

	private <E> void fillList(List<E> list, E first, E second) {
		list.add(first);
		list.add(second);
	}

	@After
	public void tearDown() throws IOException {
		Files.delete(DIRECTORY_PLACE.resolve(conf.getString(Configuration.ANALYTICAL_QUERY_RESULTS_FILE_NAME)));
		Files.delete(DIRECTORY_PLACE);
	}

	@Test
	public void reloadingAnalyticalQueryResults_should_revertToSameState() {

		AnalyticsDataManager.storeInCache(conf);
		DataManager.creativeWorksNextId.set(0L);
		DataManager.popularEntitiesList.add(null);
		DataManager.regularEntitiesList.add(null);
		DataManager.geonamesIdsList.add(null);
		DataManager.locationsIdsList.add(null);
		AnalyticsDataManager.loadFromCache(conf);

		Assert.assertEquals(creativeWorks, DataManager.creativeWorksNextId.get());
		Assert.assertEquals(expectedEntities1, DataManager.popularEntitiesList);
		Assert.assertEquals(expectedEntities2, DataManager.regularEntitiesList);
		Assert.assertEquals(expectedLocations1, DataManager.geonamesIdsList);
		Assert.assertEquals(expectedLocations2, DataManager.locationsIdsList);
		Assert.assertEquals(expectedEntities1.get(0).toString(), DataManager.popularEntitiesList.get(0).toString());
		Assert.assertEquals(expectedEntities2.get(0).toString(), DataManager.regularEntitiesList.get(0).toString());
	}
}
