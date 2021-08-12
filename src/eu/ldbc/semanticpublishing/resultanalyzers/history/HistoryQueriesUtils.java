package eu.ldbc.semanticpublishing.resultanalyzers.history;

import eu.ldbc.semanticpublishing.statistics.Statistics;

import java.util.ArrayList;
import java.util.List;

public class HistoryQueriesUtils {
	private static List<Integer> historyQueriesList;

	public static void setHistoryQueriesList(String queriesNumbersAsString) {
		assert queriesNumbersAsString != null;
		historyQueriesList = new ArrayList<>();
		String[] splitArr = queriesNumbersAsString.split(",");
		for (String currNum : splitArr) {
			assert currNum != null;
			int queryNum = Integer.parseInt(currNum.trim());
			if (!historyQueriesList.contains(queryNum)) {
				historyQueriesList.add(queryNum);
			} else {
				System.err.println("Query with number " + queryNum + " already added! Ignoring it");
			}
		}
		Statistics.setHistoryQueriesStatistics();
	}

	public static int getIndexFromQueryNum(int queryNumber) {
		for (int index = 0; index < historyQueriesList.size(); index++) {
			if (queryNumber == historyQueriesList.get(index)) {
				return index;
			}
		}
		throw new IllegalArgumentException("Query with " + queryNumber + " should not be added for history validation");
	}

	public static List<Integer> getHistoryQueriesList() {
		return historyQueriesList;
	}
}
