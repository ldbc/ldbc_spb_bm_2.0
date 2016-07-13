package eu.ldbc.semanticpublishing.tools.resultsvalidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import eu.ldbc.semanticpublishing.util.FileUtils;

public class CompareResults {
//	private static final String QUERY_START = ">> ";
//	private static final String QUERY_ID = "*** Query";
//	private static final String QUERY_EXECUTION_TIME = "execution time :";
//	private static final String QUERY_END = "---------------------------------------------";
	
	ResultsCollection queryModelCollection1;
	ResultsCollection queryModelCollection2;

	public void initialize(String sourceDir1, String sourceDir2, String scanMask) throws IOException {
		List<File> filesList = new LinkedList<File>();		
		
		queryModelCollection1 = new ResultsCollection(sourceDir1);
		queryModelCollection2 = new ResultsCollection(sourceDir2);

		FileUtils.collectFilesList2(sourceDir1, filesList, "*", false);
		
		System.out.println("Analyzing source #1: " + queryModelCollection1.getName());
		
		for (File f : filesList) {
			if (f.getCanonicalFile().getName().toLowerCase().contains(scanMask)) {
				System.out.println("\tprocessing file: " + f.getCanonicalFile().getName());
				long start = System.currentTimeMillis();
				long processed = processPseudoJson(f, queryModelCollection1);
				System.out.println(String.format("\t\t%,d queries processed in %d ms", processed, System.currentTimeMillis() - start));
			}
		}
		
		filesList.clear();
		FileUtils.collectFilesList2(sourceDir2, filesList, "*", false);
	
		System.out.println("Analyzing source #2: " + queryModelCollection2.getName());
		for (File f : filesList) {
			if (f.getCanonicalFile().getName().toLowerCase().contains(scanMask)) {
				System.out.println("\tprocessing file: " + f.getCanonicalFile().getName());
				long start = System.currentTimeMillis();
				long processed = processPseudoJson(f, queryModelCollection2);
				System.out.println(String.format("\t\t%,d queries processed in %d ms", processed, System.currentTimeMillis() - start));
			}
		}		
	}
	
	private long processPseudoJson(File file, ResultsCollection queryResultCollectionManager) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		
		String line = null;
		long processed = 0;
		boolean queryStart = false;
		StringBuilder sb = new StringBuilder();
		DetailedResultModel queryModel = null;
		
		while ((line = br.readLine()) != null) {
			
			if (line.contains("\"agent\"")) {
				if (queryModel != null) {
					processed++;
					if (!queryResultCollectionManager.addItem(queryModel)) {
						System.out.println("failed to add: " + queryModel.toString());
					}
				}
			queryStart = false;
			queryModel = new DetailedResultModel();
			String[] tokens = line.split(":");
			String agent = tokens[1].trim().replace("\"", "").replace(",", "");
			queryModel.setAgent(agent.substring(agent.lastIndexOf(".") + 1, agent.length()));
			continue;
		}
		
		if (line.contains("\"thread\"")) {
			String[] tokens = line.split(":");
			queryModel.setThread(tokens[1].trim().replace("\"", "").replace(",", ""));
			continue;
		}
		
		if (line.contains("\"queryName\"")) {
			String[] tokens = line.split(":");
			queryModel.setQueryName(tokens[1].trim().replace("\"", "").replace(",", ""));
			continue;
		}
		
		if (line.contains("\"id\"")) {
			String[] tokens = line.split(":");
			queryModel.setId(Long.parseLong(tokens[1].trim().replace("\"", "").replace(",", "")));
			continue;
		}
		
		if (line.contains("\"timeStamp\"")) {
			String[] tokens = line.split(":");
			queryModel.setTimeStamp(tokens[1].trim().replace("\"", "").replace(",", ""));
			continue;
		}
		
		if (line.contains("\"executionTimeMs\"")) {
			String[] tokens = line.split(":");
			queryModel.setExecutionTimeMs(Long.parseLong(tokens[1].trim().replace("\"", "").replace(",", "")));
			continue;
		}
		
		if (line.contains("\"results\"")) {
			String[] tokens = line.split(":");
			queryModel.setResults(Long.parseLong(tokens[1].trim().replace("\"", "").replace(",", "")));
			continue;
		}
		
		if (line.contains("\"resultStrLength\"")) {
			String[] tokens = line.split(":");
			queryModel.setResultStrLength(Long.parseLong(tokens[1].trim().replace("\"", "").replace(",", "")));
			continue;
		}
		
		if (line.contains("\"query\"")) {
			queryStart = true;
			sb.setLength(0);
			String[] tokens = line.split(":");
			sb.append(tokens[1].trim());
			sb.append("\n");
			continue;
		}
		
		if (line.contains("\"queryResult\"")) {
			//leftover from "query"
			queryModel.setQuery(sb.toString());
			sb.setLength(0);
			String[] tokens = line.split(":");
			sb.append(tokens[1].trim());
			sb.append("\n");
			continue;
		}
		
		if (line.contains("\"status\"")) {
			//leftover from "queryResult"
			queryModel.setQueryResultText(sb.toString());
			sb.setLength(0);
			queryStart = false;
			String[] tokens = line.split(":");
			sb.append(tokens[1].trim());
			queryModel.setStatus(tokens[1].trim());
			continue;
		}				
		
		if (queryStart) {
			sb.append(line);
			sb.append("\n");
			}
		}
		
		processed++;
		if (!queryResultCollectionManager.addItem(queryModel)) {
			System.out.println("failed to add at the end: " + queryModel.toString());
		}
		
		br.close();	
		return processed;
	}
	
	public void compare(boolean verbose) {
				
		ResultsCollection qBase;
		ResultsCollection qCompared;
		
		long collectionC1MinElement = ((Map<?, ?>)queryModelCollection1.getMinimalCompleteCollection()[1]).size();
		long collectionC2MinElement = ((Map<?, ?>)queryModelCollection2.getMinimalCompleteCollection()[1]).size();
		
		if (collectionC1MinElement < collectionC2MinElement) {
			qBase = queryModelCollection1;
			qCompared = queryModelCollection2;
		} else {
			qBase = queryModelCollection2;
			qCompared = queryModelCollection1;
		}
		
		System.out.println("Selected base collection  : " + qBase.getName() + ", because of minimal items found in one of its sub-collections");
		System.out.println("Selected target collection: " + qCompared.getName() + ", the collection has a greater number of items than the base one");
		
		String[] keysSorted = new String[qBase.getModel().keySet().size()];
		qBase.getModel().keySet().toArray(keysSorted); 
		Arrays.sort(keysSorted);
		
		for (String key : keysSorted) {
			if (key.toLowerCase().contains("insert") || 
				key.toLowerCase().contains("update") ||
				key.toLowerCase().contains("delete")) {
				continue;
			} 
			
			long different = 0;
			System.out.println("Comparing items for: " + key);
			
			for (Long queryId : qBase.getModel().get(key).keySet()) {
				DetailedResultModel qmBase = qBase.getModel().get(key).get(queryId);
				DetailedResultModel qmCompared = qCompared.getModel().get(key).get(queryId);
				
				if (qmBase.compareTo(qmCompared) != 0) {
					different++;
					if (verbose) {
						System.out.println("\tDIFFERENT: " + qmCompared.toShortString() + " (expected (from base): " + qmBase.toShortString() + ")");
					}
				}
			}
			
			System.out.println(String.format("  Summary (%s): different: %,d , total items: %,d, (%.3f%% different)", key, different, qBase.getModel().get(key).size(), ((float)different / (float)qBase.getModel().get(key).size()) * 100));		
		}				
	}
	
/*	
   private void process(File file, List<QueryResultModel> queriesList) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		
		String line = null;
		int lineNumber = 0;
		boolean queryStart = false;
		StringBuilder queryStringSb = new StringBuilder();
		QueryResultModel queryModel = null;

		try {
			while ((line = br.readLine()) != null) {
				lineNumber++;
				
				if (line.startsWith(QUERY_START)) {
					queryModel = new QueryResultModel();
					String tmp = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
					String[] tokens = tmp.split(":");
					queryModel.setAgentThread(tokens[0]);
					queryModel.setAgentThread(tokens[1]);
					continue;
				}
				
				if (line.startsWith(QUERY_ID)) {
					String tmp = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
					if (line.contains(", iteration")) {
						queryModel.setQueryName(tmp);
					} else {
						String[] tokens = tmp.split(",");
						queryModel.setQueryName(tokens[0]);
						String[] tokens2 = tokens[1].split(":");
						if (tokens2 != null) {
							queryModel.setId(Long.parseLong(tokens2[1]));
						} else {
							queryModel.setId(-1);
						}
						
						//queryExecutionTime
						String executionTimeString = "";
						if (line.contains("(")) {					
							executionTimeString = line.substring(line.indexOf("(") + 1, line.indexOf(" ms")).trim();
						} else {
							executionTimeString = line.substring(line.indexOf(QUERY_EXECUTION_TIME) + QUERY_EXECUTION_TIME.length(), line.indexOf(" ms")).trim();
						}
						queryModel.setExecutionTimeMs(Long.parseLong(executionTimeString));
						queryStart = true;
						queryStringSb.setLength(0);
						continue;
					}					
				}
				
				if (queryStart) {
					queryStringSb.append(line);
					queryStringSb.append("\n");
				}
				
				if (line.contains(QUERY_END)) {
					queryStart = false;
					queryModel.setQueryString(queryStringSb.toString());
					queryStringSb.setLength(0);
					queriesList.add(queryModel);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("WARN: for line: " + line + "| lineNumner: " + lineNumber);
			e.printStackTrace();
		}
	}
*/
	
	public static void showHelp() {
		System.out.println("\n\tUSAGE: eu.ldbc.semanticpublishing.tools.resultsvalidation.CompareResults <path_to_dir_base> <path_to_dir_new> <verbose_boolean>\n");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			showHelp();
			System.exit(0);
		}
		
		CompareResults vbr = new CompareResults();
		vbr.initialize(args[0], args[1], "semantic_publishing_benchmark_queries_detailed");
		System.out.println("\n" + vbr.queryModelCollection1.getStats());
		System.out.println("\n" + vbr.queryModelCollection2.getStats());
		vbr.compare(Boolean.parseBoolean(args[2]));
	}
}
