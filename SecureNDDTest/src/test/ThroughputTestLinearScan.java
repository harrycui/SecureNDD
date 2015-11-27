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

import secure.PRF;
import secure.Paillier;
import throughput.TestThread;
import util.ConfigParser;
import util.FileTool;
import util.MyAnalysis;
import util.MyCounter;
import util.PrintTool;
import base.Distance;
import base.HammingLSH;
import base.Parameters;
import base.PlainNDD;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.Repository2;
import cloud.SecureRecord;
import cloud.SecureToken;
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
public class ThroughputTestLinearScan {

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
		
		Repository2 repo = new Repository2(rid, params, csp.getKeyV(rid), csp.getKeyPublic(rid));
		
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
		List<List<Long>> lshVectors = computeLSH(rawRecords, params);
		System.out.println(">> LSH converted.");
		
		// Encrypt fingerprints
		encryptFP(rawRecords, params, repo);
		System.out.println(">> fingerprints encrypted.");
		
		
		/////
		for (int i = 0; i < lshVectors.size(); i++) {
			
			List<SecureToken> secureTokens = new ArrayList<SecureToken>(lshL);
			
			for (int j = 0; j < lshL; j++) {
				
				long lshValue = lshVectors.get(i).get(j);

				// Step 1: encrypt each LSH value
				Element r = params.pairing.getGT().newRandomElement()
						.getImmutable();

				String strR = r.toString();

				// System.out.println(strR);

				// pairing + H1()
				String t = (params.pairing.pairing(
						params.h1Pre.pow(BigInteger.valueOf(lshValue)), params.g2))
						.powZn(repo.getKeyV()).toString();

				// System.out.println(t);

				// H2()
				long h = PRF.HMACSHA1ToUnsignedInt(t, strR);

				// System.out.println("c = " + c);

				SecureToken seT = new SecureToken(strR, h);
				
				secureTokens.add(seT);
			}
			
			SecureRecord sr = new SecureRecord(i + 1, secureTokens);
			
			repo.insert(sr);
			
			if ((i+1) % (lshVectors.size() / 100) == 0) {
                System.out.println("Inserting " + (i+1) / (lshVectors.size() / 100) + "%");
            }
		}
		
		/////
        
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
				
			} else if (operationType == SysConstant.OPERATION_ANALYZE) {
				
			} else if (operationType == SysConstant.OPERATION_ANALYZE_TOP_K) {
				
			} else if (operationType == SysConstant.OPERATION_ANALYZE_CDF) {
				
			} else if (operationType == SysConstant.OPERATION_COUNT_ITEMS) {
				
			} else if (operationType == SysConstant.OPERATION_THROUGHPUT) {
				System.out.println("\nModel: throughput test.");

				while (true) {
					System.out
							.println("\n\nNow, please set the number of users: (-1 means return to root menu)");

					String strNumOfUser = null;
					int userNum;
					String strNumOfRepo = null;
					int repoNum;
					
					String strStTime = null;
					long stTime;
					
					
					try {
						strNumOfUser = br.readLine();
						
						if (strNumOfUser.equals("-1")) {
							
							System.out.println("Return to root menu!");

							break;
						}

						System.out
						.println("\n\nNow, please set the number of repos: (-1 means return to root menu)");
						strNumOfRepo = br.readLine();
						
						if (strNumOfRepo.equals("-1")) {
							
							System.out.println("Return to root menu!");

							break;
						}
						
						System.out
						.println("\n\nNow, please set the startTime: (-1 means return to root menu)");
						strStTime = br.readLine();
						
						if (strStTime.equals("-1")) {
							
							System.out.println("Return to root menu!");

							break;
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (strNumOfUser == null || strNumOfUser.equals("-1") || strNumOfRepo == null || strNumOfRepo.equals("-1")|| strStTime == null || strStTime.equals("-1")) {

							System.out.println("Return to root menu!");

							break;
						} else if (Integer.parseInt(strNumOfUser) <=0 || Integer.parseInt(strNumOfRepo) <= 0) {

							System.out
									.println("Warning: query index should be larger than 0.");

							continue;
						} else {
							userNum = Integer.parseInt(strNumOfUser);
							
							repoNum = Integer.parseInt(strNumOfRepo);
							
							stTime = Long.parseLong(strStTime);

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
					List<MyCounter> throughput = new ArrayList<MyCounter>(userNum);
					
					int[] users = new int[userNum];
					
					for (int i = 0; i < userNum; i++) {
						
						users[i] = csp.register();
						
						RawRecord queryRecord;
						
						queryRecord = queryRecords.get((i+1)%queryRecords.size());
						
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
						
						throughput.add(new MyCounter());
					}
					
					
					
					// multithread to simulate
					if (userNum > 0) {
						
			        	//multiple threads
				        MyCountDown threadCounter3 = new MyCountDown(userNum);
				        
				        long nowTime = System.currentTimeMillis();
				        
				        for (int i = 0; i < userNum; i++) {
				        	
				        	TestThread t = new TestThread("Thread " + i, threadCounter3, users[i], queries.get(i), repo, repoNum, throughput.get(i),nowTime + stTime*1000);

					        t.start();
					        
					        
				        }

				        // wait for all threads done
				        while (true) {
				            if (!threadCounter3.hasNext())
				                break;
				        }
					}
					
					Long total = 0L;
					
					for (int i = 0; i < userNum; i++) {
						total += throughput.get(i).getCtr();
					}
					
					System.out.println("Total throughput is : " + total);
				}
			}
		}
	}

	private static List<List<Long>> computeLSH(List<RawRecord> rawRecords,
			Parameters params) {
		
		List<List<Long>> lshVectors = new ArrayList<List<Long>>(rawRecords.size());

		for (int i = 0; i < rawRecords.size(); i++) {
			
			RawRecord rd = rawRecords.get(i);

			// compute LSH vector
			long[] lshVector = params.lsh.computeLSH(rd.getValue());
			
			List<Long> lshValues = new ArrayList<>(lshVector.length);
			
			for (int j = 0; j < lshVector.length; j++) {
				
				
				lshValues.add(lshVector[j]);
			}
			
			lshVectors.add(lshValues);
		}
		return lshVectors;
	}
	
	private static void encryptFP(List<RawRecord> rawRecords, Parameters params, Repository2 repo) {

		//Map<Integer, EncryptedFingerprint> encryptedFingerprints = new HashMap<Integer, EncryptedFingerprint>();
		
		for (int i = 0; i < rawRecords.size(); i++) {
			
			// encrypt fingerprint
			BigInteger cipherFP = Paillier.Enc(rawRecords.get(i).getValue(), repo.getKeyF());
			
			repo.getEncryptedFingerprints().put(rawRecords.get(i).getId(),
					new EncryptedFingerprint(rawRecords.get(i).getName(), cipherFP));
		}
	}
}