package base;

import java.util.List;
import java.util.Map;

import cloud.RawRecord;

public class MyAnalysis {
	
	public static float computeRecall(String queryName, Map<Integer, Integer> searchResult, List<RawRecord> rawRecords, int numOfPositive) {
		
		int numOfTruePositive = 0;
		//int numOfPositive = 0;
		
		for (Map.Entry<Integer, Integer> entry : searchResult.entrySet()) {

			int id = entry.getKey();
			//int counter = entry.getValue();
			
			RawRecord rawRecord = rawRecords.get(id - 1);
			
			if (checkTruePositive(queryName, rawRecord.getName())) {
				
				numOfTruePositive++;
			}
		}
		
		return (float)numOfTruePositive/numOfPositive;
	}

	public static float computePrecision(String queryName, Map<Integer, Integer> searchResult, List<RawRecord> rawRecords) {
		
		int numOfTruePositive = 0;
		int numOfMatches = searchResult.size();
		
		for (Map.Entry<Integer, Integer> entry : searchResult.entrySet()) {

			int id = entry.getKey();
			//int counter = entry.getValue();
			
			RawRecord rawRecord = rawRecords.get(id - 1);
			
			if (checkTruePositive(queryName, rawRecord.getName())) {
				
				numOfTruePositive++;
			}
		}
		
		return (float)numOfTruePositive/numOfMatches;
	}
	
	public static boolean checkTruePositive(String queryName, String testName) {
		
		String name1 = queryName.substring(queryName.lastIndexOf("_"), queryName.lastIndexOf("."));
		
		String name2 = testName.substring(testName.lastIndexOf("_"), testName.lastIndexOf("."));
		
		return (name1.equals(name2));
	}
}
