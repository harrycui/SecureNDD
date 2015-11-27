package test;

import it.unisa.dia.gas.jpbc.Element;

import java.awt.SystemTray;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import secure.Paillier;
import throughput.TestThread;
import util.ConfigParser;
import util.FileTool;
import util.PrintTool;
import base.Distance;
import base.HammingLSH;
import base.MyAnalysis;
import base.Parameters;
import base.PlainNDD;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.SingleRepoInsertThread;
import cloud.SingleRepoSearchThread;

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
public class PerformanceTestLinearScan {

	public static void main(String[] args) {

		if (args.length < 1) {

			PrintTool.println(PrintTool.ERROR,
					"please check the argument list!");

			return;
		}

		ConfigParser config = new ConfigParser(args[0]);

		String inputPath = config.getString("inputPath");
		String rawRecordFileName = config.getString("inputFileName");
		String queryFileName = config.getString("queryFileName");

		int numOfLimit = config.getInt("numOfLimit");
		
		// hardcode: set numOfRepo = 1
		int numOfRepo = 1; //config.getInt("numOfRepo");
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
		
		List<RawRecord> queryRecords = FileTool.readFingerprintFromFile2ListV2(inputPath, queryFileName, numOfLimit, false);

		
		
		if (numOfLimit > rawRecords.size()) {
			numOfLimit = rawRecords.size();
		}
		

		// Step 2: initialize the repositories and secure insert records
		System.out.println(">>> There are " + numOfLimit + " records.");
		System.out.println(">>> Now, initializing " + numOfRepo + " repositories.");
		
		
		// the repository registers
		int rid = csp.register();
		
		Repository repo = new Repository(rid, params, csp.getKeyV(rid), csp.getKeyPublic(rid));
		
		long stOfAuth = System.nanoTime();
		
		// authorize the detector
		Element delta = csp.authorize(rid, detectorId);
		
		long etOfAuth = System.nanoTime();
		
		System.out.println("Avg auth time is:" + (double)(etOfAuth - stOfAuth) / 1000000 + " ms.");
					
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

        	SingleRepoInsertThread t = new SingleRepoInsertThread("Thread " + i, threadCounter, repo.getKeyV(), lshVectors.get(i), repo.getSecureRecords().get(i), params);

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
							+ "[5]  average located items;\n"
							+ "[6]  throughput;\n"
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
					} else if (Integer.parseInt(inputStr) > 6
							|| Integer.parseInt(inputStr) < 1) {

						System.out.println("Warning: operation type should be limited in [1, 6], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out.println("Warning: operation type should be limited in [1, 6], please try again!");

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
					List<Element> tArray = new ArrayList<Element>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int i = 0; i < lshL; i++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[i])).powZn(csp.getKeyV(detectorId));
						
						tArray.add(t);
					}
					
					long etOfGenQuery = System.currentTimeMillis();
					
					System.out.println("Time cost of generate query: " + (etOfGenQuery - stOfGenQuery) + " ms.");
					
					long time1 = System.currentTimeMillis();
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, tArray);
					
					long time2 = System.currentTimeMillis();

					System.out.println("Cost " + (time2 - time1) + " ms.");
					
					long avgDecTime = 0;
					
					if (searchResult != null && searchResult.size() > 0) {
						
						for (Map.Entry<Integer, Integer> entry : searchResult.entrySet()) {

							int id = entry.getKey();
							int counter = entry.getValue();
							
							EncryptedFingerprint item = repo.getEncryptedFingerprints().get(id);
							
							BigInteger plainFP;
							try {
								
								long stOfDec = System.nanoTime();
								
								plainFP = Paillier.Dec(item.getCipherFP(), repo.getKeyF(), csp.getKeyPrivate(repo.getId()));
								
								long etOfDec = System.nanoTime();
								
								avgDecTime += etOfDec - stOfDec;
								
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
						
						System.out.println("Avg dec time is:" + (double)avgDecTime/searchResult.size() / 1000000 + " ms.");
						
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
					List<Element> Q = new ArrayList<Element>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
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
					List<Element> Q = new ArrayList<Element>(lshL);
					

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
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
	

			} else if (operationType == SysConstant.OPERATION_ANALYZE_CDF) {
				
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
					List<Element> Q = new ArrayList<Element>(lshL);
					
					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
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
		        
			} else if (operationType == SysConstant.OPERATION_COUNT_ITEMS) {
				
				RawRecord queryRecord;
				
				int[] numOfItemsInThreshold = new int [threshold+1];

				int queryTimes = 0;
				
				long avgGenQueryTime = 0;
				
				long avgSearchTime = 0;

				for (int i = 0; i < queryRecords.size(); i++) {

					queryRecord = queryRecords.get(i);
					
					System.out.println(++queryTimes);
					
					long stOfGenQuery = System.nanoTime();
					
					// prepare the query message
					List<Element> Q = new ArrayList<Element>(lshL);

					long[] lshVector = lsh.computeLSH(queryRecord.getValue());
					
					for (int j = 0; j < lshL; j++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
					}
					
					long etOfGenQuery = System.nanoTime();
					
					avgGenQueryTime += etOfGenQuery - stOfGenQuery;
					
					long stSearchTime = System.currentTimeMillis();
					
					Map<Integer, Integer> searchResult = repo.secureSearch(detectorId, Q);
					
					long etSearchTime = System.currentTimeMillis();
					
					avgSearchTime += etSearchTime - stSearchTime;
					
					int[] tmpResult = new int[threshold+1];
					
					for (Map.Entry<Integer, Integer> entry : searchResult.entrySet()) {

						int id = entry.getKey();
						
						int dist = Distance.getHammingDistanceV2(queryRecord.getValue(), rawRecords.get(id-1).getValue());
						
						if (dist <= threshold) {
							
							for (int j = dist; j <= threshold; j++) {
								tmpResult[j]++;
							}
						}
					}
					
					
					for (int j = 0; j <= threshold; j++) {
						
						numOfItemsInThreshold[j] += tmpResult[j];
					}
					
				}
				
				System.out.println("Avg gen query time is:" + (double)avgGenQueryTime/queryRecords.size() / 1000000 + " ms.");
				System.out.println("Avg search time is:" + (double)avgSearchTime/queryRecords.size() + " ms.");
				// print the statistics
				System.out.println("Average located items' number are:\n");
				
				for (int i = 0; i <= threshold; i++) {
					
					System.out.println("threshold < " + i + ": " + numOfItemsInThreshold[i]/queryRecords.size());
				}
			} else if (operationType == SysConstant.OPERATION_THROUGHPUT) {
				System.out.println("\nModel: query.");

				while (true) {
					System.out
							.println("\n\nNow, please set the number of users: (-1 means return to root menu)");

					String strNumOfUser = null;
					int userNum;
					String strNumOfRepo = null;
					int repoNum;
					
					
					try {
						strNumOfUser = br.readLine();
						System.out
						.println("\n\nNow, please set the number of repos: (-1 means return to root menu)");
						strNumOfRepo = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (strNumOfUser == null || strNumOfUser.equals("-1") || strNumOfRepo == null || strNumOfRepo.equals("-1")) {

							System.out.println("Return to root menu!");

							break;
						} else if (Integer.parseInt(strNumOfUser) <=0 || Integer.parseInt(strNumOfRepo) <= 0) {

							System.out
									.println("Warning: query index should be limited in [1, limit]");

							continue;
						} else {
							userNum = Integer.parseInt(strNumOfUser);
							
							repoNum = Integer.parseInt(strNumOfRepo);
							

							System.out.println("Test on : "
									+ userNum + " users, " + repoNum + " repos.\n");
						}
					} catch (NumberFormatException e) {
						System.out
								.println("Warning: format error.");
						continue;
					}

					
					// prepare userNum of queries
					
					List<List<Element>> queries = new ArrayList<List<Element>>(userNum);
					List<Long> throughput = new ArrayList<Long>(userNum);
					
					int[] users = new int[userNum];
					
					for (int i = 0; i < userNum; i++) {
						
						users[i] = csp.register();
						
						RawRecord queryRecord;
						
						queryRecord = queryRecords.get(i+1);
						
						// prepare the query message
						List<Element> tArray = new ArrayList<Element>(lshL);
						

						long[] lshVector = lsh.computeLSH(queryRecord.getValue());
						
						for (int j = 0; j < lshL; j++) {
							
							Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[j])).powZn(csp.getKeyV(users[i]));
							
							tArray.add(t);
						}
						
						queries.add(tArray);
						
						// add users to repo
						Element auth = csp.authorize(rid, users[i]);
									
						// id = 0 is the detector
						repo.addDelta(users[i], auth);
						
						throughput.add(new Long(0));
					}
					
					
					
					// multithread to simulate
					if (userNum > 0) {
						
			        	//multiple threads
				        MyCountDown threadCounter3 = new MyCountDown(userNum);
				        for (int i = 0; i < userNum; i++) {
				        	
				        	//TestThread t = new TestThread("Thread " + i, threadCounter3, users[i], queries.get(i), repo, throughput.get(i));

					        //t.start();
				        }

				        // wait for all threads done
				        while (true) {
				            if (!threadCounter3.hasNext())
				                break;
				        }
					}
					
					Long total = 0L;
					
					for (int i = 0; i < userNum; i++) {
						total += throughput.get(i);
					}
					
					System.out.println("Total throughput is : " + total);
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
	
	private static void encryptFP(List<RawRecord> rawRecords, Parameters params, Repository repo) {

		//Map<Integer, EncryptedFingerprint> encryptedFingerprints = new HashMap<Integer, EncryptedFingerprint>();
		
		for (int i = 0; i < rawRecords.size(); i++) {
			
			// encrypt fingerprint
			BigInteger cipherFP = Paillier.Enc(rawRecords.get(i).getValue(), repo.getKeyF());
			
			repo.getEncryptedFingerprints().put(rawRecords.get(i).getId(),
					new EncryptedFingerprint(rawRecords.get(i).getName(), cipherFP));
		}
	}
}
