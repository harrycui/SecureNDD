package test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import secure.Paillier;
import util.ConfigParser;
import util.FileTool;
import util.MyAnalysis;
import util.PrintTool;
import base.Distance;
import base.HammingLSH;
import base.Parameters;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.SingleRepoInsertThread;
import cloudWithIndex.IndexedToken;
import cloudWithIndex.RepositoryWithIndex;
import cloudWithIndex.SingleRepoInsertThreadV2;

/**
 * For performance evaluation, we just use one repository and involve the ranking mechanism.
 * 
 * The thread is in "L" level.
 * @author Helei Cui
 * 
 * Modified by CHU Yilei on 2015 June 20
 * Update:  1570 images * 9 = 14,130 resized images
 * 			Using 157 original images to do 157 queries 
 * 			(query fingerprints in ahash_fp_jpeg_query.txt generated by BatchGenFingerprint.java)
 * 			For each query's result list (after voting), examine top 1, top 5, top 10, top 20, top 30... respectively
 * 			to get the average true positive rate
 * 			*** Note: here the ground truth is determined by plaintext's threshold rather than by examining the file name. ***  
 *
 */
public class PerformanceTestIndex {

	public static void main(String[] args) {

		if (args.length < 1) {

			PrintTool.println(PrintTool.ERROR,
					"please check the argument list!");

			return;
		}

		ConfigParser config = new ConfigParser(args[0]);

		String inputPath = config.getString("inputPath");
		String rawRecordFileName = config.getString("inputFileName");
		String querFileName = config.getString("queryFileName");
		int numOfLimit = config.getInt("numOfLimit");
		
		// hardcode: set numOfRepo = 1
		int numOfRepo = config.getInt("numOfRepo");
		String pairingSettingPath = config.getString("pairingSettingPath");
		
		int bitLength = config.getInt("bitLength");
		int certainty = config.getInt("certainty");
		
		int lshL = config.getInt("lshL");
		int lshDimension = config.getInt("lshDimension");
		int lshK = config.getInt("lshK");
		int threshold = config.getInt("threshold");
		int numOfPositive = config.getInt("numOfPositive");
		
		
		// Step 1: preprocess: setup keys and read file
		Parameters params = new Parameters(pairingSettingPath, lshL, lshDimension, lshK, bitLength, certainty);
		
		CSP csp = new CSP(params);
		
		HammingLSH lsh = new HammingLSH(lshDimension, lshL, lshK);
		
		System.out.println(">>> System parameters have been initialized");
		System.out.println(">>> Now, reading the raw test data from " + inputPath + rawRecordFileName);
		
		// the first user is the detector, id is from 0?1
		int detectorId = csp.register();
		
		// TODO: read each line at the time of inserting
		// read file to lines list
		List<RawRecord> rawRecords = FileTool.readFingerprintFromFile2ListV2(inputPath, rawRecordFileName, numOfLimit, false);
		
		List<RawRecord> queryRecords = FileTool.readFingerprintFromFile2ListV2(inputPath, querFileName, numOfLimit, false);
		
		
		if (numOfLimit > rawRecords.size()) {
			numOfLimit = rawRecords.size();
		}
		

		// Step 2: initialize the repositories and secure insert records
		System.out.println(">>> There are " + numOfLimit + " records.");
		System.out.println(">>> Now, initializing " + numOfRepo + " repositories.");
		
		
		// the repository registers
		int rid = csp.register();
		
		RepositoryWithIndex repo = new RepositoryWithIndex(rid, params, csp.getKeyV(rid), csp.getKeyPublic(rid));
		
		// authorize the detector
		Element delta = csp.authorize(rid, detectorId);
					
		// id = 0 is the detector
		repo.addDelta(detectorId, delta);
		
		System.out.println(">>> Now, start inserting data into repositories...");
		
		long startTimeOfInsert = System.currentTimeMillis();
		
		// Compute LSH
		List<Map<Integer, Long>> lshVectors = computeLSH(rawRecords, params);
		System.out.println(">> LSH converted.");
		
		// Encrypt fingerprints
		encryptFP(rawRecords, params, repo);
		System.out.println(">> fingerprints encrypted.");
		
		//multiple threads
        MyCountDown threadCounter = new MyCountDown(lshL);

        for (int i = 0; i < lshL; i++) {

        	SingleRepoInsertThreadV2 t = new SingleRepoInsertThreadV2("Thread " + i, threadCounter, repo.getKeyV(), lshVectors.get(i), repo.getHIndices().get(i), repo.getAIndices().get(i), params);

            t.start();
        }

        // wait for all threads done
        while (true) {
            if (!threadCounter.hasNext())
                break;
        }
        
        long etOfInsert = System.currentTimeMillis();
        
        System.out.println("Insert time: " + (etOfInsert - startTimeOfInsert) + " ms.");

		// %%%%%%%%%%%%%%%%%% test %%%%%%%%%%%%%%%%%%%

		if (rawRecords == null || rawRecords.isEmpty()) {

			PrintTool.println(PrintTool.ERROR,
					"reading failed, please check the input file!");

			return;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean rootFlag = true;

		while (rootFlag) {
			System.out
					.print("\n\n----------------------- Root Menu -----------------------\n"
							+ "Please select an operation:\n"
							+ "[1]  query test;\n"
							+ "[2]  analyze recall and precision;\n"
							+ "[3]  analyze top-k;\n"
							+ "[4]  analyze CDF;\n"
							+ "[QUIT] quit system.\n\n"
							+ "--->");
			String inputStr;
			int operationType;
			try {
				inputStr = br.readLine();

				try {
					if (inputStr == null
							|| inputStr.toLowerCase().equals("quit")
							|| inputStr.toLowerCase().equals("q")) {

						System.out.println("Quit!");

						break;
					} else if (Integer.parseInt(inputStr) > 4
							|| Integer.parseInt(inputStr) < 1) {

						System.out
								.println("Warning: operation type should be limited in [1, 4], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out
							.println("Warning: operation type should be limited in [1, 4], please try again!");
					continue;
				}

			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			if (operationType == SysConstant.OPERATION_QUERY) {

				System.out.println("\nModel: query.");

				while (true) {
					System.out
							.println("\n\nNow, you can search by input you query id range from [1, "
									+ queryRecords.size()
									+ "]: (-1 means return to root menu)");

					String queryStr = null;
					int queryIndex;
					RawRecord queryRecord;
					
					try {
						queryStr = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (queryStr == null || queryStr.equals("-1")) {

							System.out.println("Return to root menu!");

							break;
						} else if (Integer.parseInt(queryStr) > queryRecords
								.size() || Integer.parseInt(queryStr) <= 0) {

							System.out
									.println("Warning: query index should be limited in [1, limit]");

							continue;
						} else {
							queryIndex = Integer.parseInt(queryStr);
							
							queryRecord = queryRecords.get(queryIndex-1);

							System.out.println("For query item id : "
									+ queryRecord.getId() + ", name : " + queryRecord.getName() + ", fingerprint : " + queryRecord.getValue());
						}
					} catch (NumberFormatException e) {
						System.out
								.println("Warning: query index should be limited in [1, "
										+ rawRecords.size() + "]");
						continue;
					}

					long stOfGenQuery = System.currentTimeMillis();
					
					// prepare the query message
					List<IndexedToken> Q = new ArrayList<IndexedToken>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int i = 0; i < lshL; i++) {
						
						Element t1 = params.h1Pre.pow(BigInteger.valueOf(lshVector[i])).powZn(csp.getKeyV(detectorId));
						
						Element t2 = params.h11Pre.pow(BigInteger.valueOf(lshVector[i])).powZn(csp.getKeyV(detectorId));
						
						Q.add(new IndexedToken(t1, t2));
					}
					
					long etOfGenQuery = System.currentTimeMillis();
					
					System.out.println("Time cost of generate query: " + (etOfGenQuery - stOfGenQuery) + " ms.");
					
					long time1 = System.currentTimeMillis();
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, Q);
					
					long time2 = System.currentTimeMillis();

					System.out.println("Cost " + (time2 - time1) + " ms.");
					
					if (searchResult != null && searchResult.size() > 0) {
						
						for (Map.Entry<Integer, Integer> entry : searchResult.entrySet()) {

							int id = entry.getKey();
							int counter = entry.getValue();
							
							EncryptedFingerprint item = repo.getEncryptedFingerprints().get(id);
							
							BigInteger plainFP;
							try {
								plainFP = Paillier.Dec(item.getCipherFP(), repo.getKeyF(), csp.getKeyPrivate(repo.getId()));
								
								int dist = Distance.getHammingDistanceV2(queryRecord.getValue(), plainFP);
								
								if (dist > threshold) {
									
									continue;
								}
								
								System.out.println(id + " :: " + item.getName() + " :: " + plainFP + " >>> dist: " + dist + "  Counter::" + counter);	
								//System.out.println("Counter::" + counter);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("No similar item!!!");
					}
					
					// print the statistics
					//System.out.println("The recall is : " + MyAnalysis.computeRecall(queryRecord.getName(), searchResult, rawRecords, numOfPositive));
					
					//System.out.println("The precision is : " + MyAnalysis.computePrecision(queryRecord.getName(), searchResult, rawRecords));
				}
			} else if (operationType == SysConstant.OPERATION_ANALYZE) {
				
				RawRecord queryRecord;
				
				float avgGenTokenTime = 0;
				
				long avgSearchTime = 0;
				
				float avgRecall = 0;
				
				float avgPrecision = 0;
				
				int avgNumOfCandidate = 0;
				
				int queryTimes = 0;
				
				for (int i = 0; i < queryRecords.size(); i++) {
					
					queryRecord = queryRecords.get(i);
					

					
					System.out.println(++queryTimes);
					
					long stOfGenToken = System.currentTimeMillis();
					// prepare the query message
					List<IndexedToken> Q = new ArrayList<IndexedToken>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t1 = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Element t2 = params.h11Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(new IndexedToken(t1, t2));
					}
					
					long etOfGenToken = System.currentTimeMillis();
					
					long time1 = System.currentTimeMillis();
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, Q);
					
					long time2 = System.currentTimeMillis();

					avgGenTokenTime += etOfGenToken - stOfGenToken;
					
					avgSearchTime += time2 - time1;
					
					avgNumOfCandidate += searchResult.size();
					
					//avgRecall += MyAnalysis.computeRecall(queryRecord.getName(), searchResult, rawRecords, numOfPositive);
					
					//avgPrecision += MyAnalysis.computePrecision(queryRecord.getName(), searchResult, rawRecords);
				
				}
				
				// print the statistics
				//System.out.println("Average recall is        : " + avgRecall/queryTimes*100 + " %");
				//System.out.println("Average precision is     : " + avgPrecision/queryTimes*100 + " %");
				
				
				System.out.println("\nAverage genToken time is : " + avgGenTokenTime/(float)queryTimes + " ms");
				System.out.println("Average search time is   : " + avgSearchTime/(float)queryTimes + " ms");
				System.out.println("Average candidate size   : " + avgNumOfCandidate/queryTimes);
				
				
				
			} else if (operationType == SysConstant.OPERATION_ANALYZE_TOP_K) {
				
				RawRecord queryRecord;
				
				float avgGenTokenTime = 0;
				
				long avgSearchTime = 0;
				
				float avgRecall = 0;
								
				int avgNumOfCandidate = 0;
				
				int queryTimes = 0;
				
				int[] topK_list = {1,3,5,8,10,13,15,18,20,30,40,50}; // the choice of K for "top K"
				
				float[] avgAccuracy = new float[topK_list.length];
				
				for(int j=0; j<avgAccuracy.length;++j)
					avgAccuracy[j] = 0.0F;
				
				for (int i = 0; i < queryRecords.size(); i++) {
					
					queryRecord = queryRecords.get(i);
					
					++queryTimes;
					System.out.println(queryTimes);
					
					long stOfGenToken = System.currentTimeMillis();
					// prepare the query message
					List<IndexedToken> Q = new ArrayList<IndexedToken>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t1 = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Element t2 = params.h11Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(new IndexedToken(t1, t2));
					}
					
					long etOfGenToken = System.currentTimeMillis();
					
					long time1 = System.currentTimeMillis();
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, Q);
					
					// rank the result
					List<Entry<Integer, Integer>> rankedList = new ArrayList<Entry<Integer, Integer>>(searchResult.entrySet());   
					  
					Collections.sort(rankedList, new Comparator<Object>(){   
					          public int compare(Object e1, Object e2){   
					        int v1 = ((Entry<Integer, Integer>)e1).getValue();   
					        int v2 = ((Entry<Integer, Integer>)e2).getValue();   
					        return v2-v1;   
					           
					    }   
					}); 
					
					long time2 = System.currentTimeMillis();

					avgGenTokenTime += etOfGenToken - stOfGenToken;
					
					avgSearchTime += time2 - time1;
					
					avgNumOfCandidate += searchResult.size();
					
					// 2015 06 20 Update: for different topK, calculate a precision & recall 
					
					for (int j = 0; j < topK_list.length; j++) {
						
						int topK = topK_list[j];
						
						if (rankedList.size() < topK) {
							topK = rankedList.size();
						}
						
						avgAccuracy[j] += MyAnalysis.computeTopK(queryRecord, rankedList.subList(0, topK), rawRecords, threshold);
					}
				
				}
				
				// print the statistics
				//System.out.println("Average recall is        : " + avgRecall/queryTimes*100 + " %");
				for (int j = 0; j < topK_list.length; j++) {
					
					System.out.println("Average accuracy of top-" + topK_list[j] + " is     : " + avgAccuracy[j]/queryTimes*100 + " %");
				}
				
				
				System.out.println("\nAverage genToken time is : " + avgGenTokenTime/(float)queryTimes + " ms");
				System.out.println("Average search time is   : " + avgSearchTime/(float)queryTimes + " ms");
				System.out.println("Average candidate size   : " + avgNumOfCandidate/queryTimes);
	
			}else if (operationType == SysConstant.OPERATION_ANALYZE_CDF)
			{					
				RawRecord queryRecord;
				
				float avgRecall = 0;
								
				int avgNumOfCandidate = 0;
				
				int queryTimes = 0;
				
				int[] numOfRetrieval_list = {1,5,10,15,20,30,40,50};
				
				int[][] cntRecord = new int[numOfRetrieval_list.length][queryRecords.size()];
				
				//initialize result 2d array to 0
				for (int i = 0; i < numOfRetrieval_list.length; i++) 
					for (int j = 0; j < queryRecords.size(); j++) 
						cntRecord[i][j] = 0;
								
				for (int i = 0; i < queryRecords.size(); i++) {
					
					queryRecord = queryRecords.get(i);
					
					++queryTimes;
					
					// prepare the query message
					List<IndexedToken> Q = new ArrayList<IndexedToken>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t1 = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Element t2 = params.h11Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(new IndexedToken(t1, t2));
					}
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, Q);
					
					// rank the result
					List<Entry<Integer, Integer>> rankedList = new ArrayList<Entry<Integer, Integer>>(searchResult.entrySet());   
					  
					Collections.sort(rankedList, new Comparator<Object>(){   
					          public int compare(Object e1, Object e2){   
					        int v1 = ((Entry<Integer, Integer>)e1).getValue();   
					        int v2 = ((Entry<Integer, Integer>)e2).getValue();   
					        return v2-v1;   
					           
					    }   
					}); 
									
					
					// 2015 06 20 Update: for different numOfRetrieval, print cntCompare per query 					
					for (int j = 0; j < numOfRetrieval_list.length; j++) {						
						int numOfRetrieval = numOfRetrieval_list[j];
						
						if (rankedList.size() <= numOfRetrieval) {
							cntRecord[j][i] = rankedList.size();
						}else {
							cntRecord[j][i] = MyAnalysis.computeCDF(queryRecord, rankedList, rawRecords, threshold,numOfRetrieval);
						}						
					}									
				} // end of query
				
		        //print result to file
				BufferedWriter writer = null;
		        
		        try {
					writer = new BufferedWriter(new FileWriter("./cdfResult.txt", false));
		        
			        for (int i = 0; i < numOfRetrieval_list.length; i++)
			        {
						for (int j = 0; j < queryRecords.size(); j++) 
							writer.write(cntRecord[i][j]+";");
						writer.write("\n\n\n");
			        }
			        
			        writer.close();
		        	
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}

	private static List<Map<Integer, Long>> computeLSH(List<RawRecord> rawRecords,
			Parameters params) {
		
		List<Map<Integer, Long>> lshVectorsInL = new ArrayList<Map<Integer, Long>>(params.lshL);

		for (int i = 0; i < params.lshL; i++) {
			lshVectorsInL.add(new HashMap<Integer, Long>());
		}

		for (int i = 0; i < rawRecords.size(); i++) {
			
			RawRecord rd = rawRecords.get(i);

			// compute LSH vector
			long[] lshVector = params.lsh.computeLSH(rd.getValue());
			
			for (int j = 0; j < lshVector.length; j++) {
				lshVectorsInL.get(j).put(rd.getId(), lshVector[j]);
			}
		}
		return lshVectorsInL;
	}
	
	private static void encryptFP(List<RawRecord> rawRecords, Parameters params, RepositoryWithIndex repo) {

		//Map<Integer, EncryptedFingerprint> encryptedFingerprints = new HashMap<Integer, EncryptedFingerprint>();
		
		for (int i = 0; i < rawRecords.size(); i++) {
			
			// encrypt fingerprint
			BigInteger cipherFP = Paillier.Enc(rawRecords.get(i).getValue(), repo.getKeyF());
			
			repo.getEncryptedFingerprints().put(rawRecords.get(i).getId(),
					new EncryptedFingerprint(rawRecords.get(i).getName(), cipherFP));
		}
	}
}
