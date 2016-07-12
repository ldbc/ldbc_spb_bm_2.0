package eu.ldbc.semanticpublishing.tools.resultsvalidation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Contains sub-collections(maps) of items 
 * Contains statistics for those items
 */
public class ResultsCollection {
	private String name;
	private Map<Long, DetailedResultModel> itemMap;
	private Map<String, Map<Long, DetailedResultModel>> queryModelCollection;
	
	private Set<String> unique = new HashSet<>();
	
	public ResultsCollection(String name) {
		this.name = name;
		this.queryModelCollection = new HashMap<String, Map<Long, DetailedResultModel>>();
	}
	
	public boolean addItem(DetailedResultModel queryResultModel) {
		if (queryResultModel == null) {
			return false;
		}
		
		String key = queryResultModel.getQueryName();
		if (!unique.contains(key)) {
			unique.add(key);
			itemMap = new TreeMap<Long, DetailedResultModel>();
			queryModelCollection.put(key, itemMap);
		} else {
			itemMap = getItem(key);
		}
		
		if (itemMap.containsKey(queryResultModel.getId())) {
			System.out.println("WARNING: map: " + key + ", contains duplicate id: " + queryResultModel.getId() + ", skipping");
		}
		itemMap.put(queryResultModel.getId(), queryResultModel);
		
		return true;
	}
	
	public Map<Long, DetailedResultModel> getItem(String key) {
		return queryModelCollection.get(key);
	}
	
	public Map<String, Map<Long, DetailedResultModel>> getModel() {
		return queryModelCollection;
	}
	
	/**
	 * @return - minimal number of all completed query mixes, i.e. max id for each query where all queries have it
	 */
	public Object[] getMinimalCompleteCollection() {
		
		long min = Long.MAX_VALUE;
		String collectionName = "";
		Map<Long, DetailedResultModel> collection = null;
		
		//pick collection with min number of members		
		for (String key : queryModelCollection.keySet()) {
			if (key.toLowerCase().contains("insert") || 
				key.toLowerCase().contains("update") ||
				key.toLowerCase().contains("delete")) {
				continue;
			}
			
			if (min > queryModelCollection.get(key).size()) {
				collectionName = key;
				collection = queryModelCollection.get(key);
				min = queryModelCollection.get(key).size();
			}
		}
		
		return new Object[] {collectionName, collection};
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getStats() {
		long totalSize = 0;
		StringBuilder sb = new StringBuilder();
		
		sb.append("QueryResultsCollection: " + this.getName());
		sb.append("\n");
		sb.append("--------------------------------------------------------------------------------------------------");
		sb.append("\n");
		for (String key : queryModelCollection.keySet()) {
			sb.append(String.format(":: collection: %s :: size: %,d", key, queryModelCollection.get(key).size()));
			sb.append("\n");
			totalSize += queryModelCollection.get(key).size();
		}
		
		Object[] minimalCompleteCollection = getMinimalCompleteCollection();
		sb.append(String.format("Min number of items (%,d) were found in sub-collection: %s", ((Map<?, ?>)minimalCompleteCollection[1]).size(), minimalCompleteCollection[0]));
		sb.append("\n");
		sb.append(String.format("Total items count: %,d", totalSize));
		sb.append("\n");
		
		return sb.toString();
	}
}
