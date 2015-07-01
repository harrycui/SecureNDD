package test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import secure.Paillier;
import util.ConfigParser;
import util.FileTool;
import util.PrintTool;
import base.Distance;
import base.HammingLSH;
import base.MyAnalysis;
import base.Parameters;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.SingleRepoInsertThread;

/**
 * For performance evaluation, we just use one repository and involve the ranking mechanism.
 * 
 * This is a batch test.
 * 
 * The thread is in "L" level.
 * @author Helei Cui
 *
 */
public class BatchPerformanceTest {

	public static void main(String[] args) throws IOException {

		if (args.length < 1) {

			PrintTool.println(PrintTool.ERROR,
					"please check the argument list!");

			return;
		}

		ConfigParser config = new ConfigParser(args[0]);

		String inputPath = config.getString("inputPath");
		String inputFileName = config.getString("inputFileName");
		int numOfLimit = config.getInt("numOfLimit");
		
		// hardcode: set numOfRepo = 1
		int numOfRepo = 1; //config.getInt("numOfRepo");
		String pairingSettingPath = config.getString("pairingSettingPath");
		
		int bitLength = config.getInt("bitLength");
		int certainty = config.getInt("certainty");
		
		//int lshL = config.getInt("lshL");
		int lshDimension = config.getInt("lshDimension");
		//int lshK = config.getInt("lshK");
		int threshold = config.getInt("threshold");
		int numOfPositive = config.getInt("numOfPositive");
		
		int[] lArray = {5,8,10,12,15};
		int[] kArray = {5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,23,25,30,35,40,45,50,64};

        
		
		for (int l = 0; l < lArray.length; l++) {
			
			for (int k = 0; k < kArray.length; k++) {
				
				int lshL = lArray[l];
				int lshK = kArray[k];
				
				BufferedWriter writer = null;
		        
		        try {
					writer = new BufferedWriter(new FileWriter("./analysisResult.txt", true));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				writer.write("\n-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.\n\nINFOCOM 2016 Experiment - batch test:\n\n\tl = " + lshL + " \tk = " + lshK);
				
				// Step 1: preprocess: setup keys and read file
				Parameters params = new Parameters(pairingSettingPath, lshL, lshDimension, lshK, bitLength, certainty);
				
				CSP csp = new CSP(params);
				
				HammingLSH lsh = new HammingLSH(lshDimension, lshL, lshK);
				
				System.out.println(">>> System parameters have been initialized");
				System.out.println(">>> Now, reading the raw test data from " + inputPath + inputFileName);
				
				// the first user is the detector, id is from 0?1
				int detectorId = csp.register();
				
				// TODO: read each line at the time of inserting
				// read file to lines list
				List<RawRecord> rawRecords = FileTool.readFingerprintFromFile2ListV2(inputPath, inputFileName, numOfLimit, false);
				
				
				if (numOfLimit > rawRecords.size()) {
					numOfLimit = rawRecords.size();
				}
				

				// Step 2: initialize the repositories and secure insert records
				System.out.println(">>> There are " + numOfLimit + " records.");
				System.out.println(">>> Now, initializing " + numOfRepo + " repositories.");
				
				
				// the repository registers
				int rid = csp.register();
				
				Repository repo = new Repository(rid, params, csp.getKeyV(rid), csp.getKeyPublic(rid));
				
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
				
				
				// start analyze
				RawRecord rawQuery;
				
				float avgGenTokenTime = 0;
				
				long avgSearchTime = 0;
				
				float avgRecall = 0;
				
				float avgPrecision = 0;
				
				int avgNumOfCandidate = 0;
				
				int queryTimes = 0;
				
				for (int i = 0; i < rawRecords.size(); i++) {
					
					rawQuery = rawRecords.get(i);
					
					// Only check the image, of which name contains "original"
					if (rawQuery.getName().contains("original") && !rawQuery.getName().contains("scale")) {
						
						System.out.println(++queryTimes);
						
						long stOfGenToken = System.currentTimeMillis();
						// prepare the query message
						List<Element> Q = new ArrayList<Element>(lshL);
						

						long[] lshVector = lsh.computeLSH(rawQuery.getValue());
						
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
						
						avgRecall += MyAnalysis.computeRecall(rawQuery.getName(), searchResult, rawRecords, numOfPositive);
						
						avgPrecision += MyAnalysis.computePrecision(rawQuery.getName(), searchResult, rawRecords);
					}
				}
				
				// print the statistics
				System.out.println("Average recall is        : " + avgRecall/queryTimes*100 + " %");
				System.out.println("Average precision is     : " + avgPrecision/queryTimes*100 + " %");
				
				
				System.out.println("\nAverage genToken time is : " + avgGenTokenTime/(float)queryTimes + " ms");
				System.out.println("Average search time is   : " + avgSearchTime/(float)queryTimes + " ms");
				System.out.println("Average candidate size   : " + avgNumOfCandidate/queryTimes);
				
				writer.write("\n\n\t\tInsert time: " + (etOfInsert - startTimeOfInsert) + " ms" +
	                    "\n\n\t\tAverage recall is        : " + avgRecall/queryTimes*100 + " %" +
	                    "\n\t\tAverage precision is     : " + avgPrecision/queryTimes*100 + " %" +
	                    "\n\n\t\tAverage genToken time is : " + avgGenTokenTime/(float)queryTimes + " ms" +
	                    "\n\t\tAverage search time is   : " + avgSearchTime/(float)queryTimes + " ms" +
	                    "\n\t\tAverage candidate size   : " + avgNumOfCandidate/queryTimes);
				
				writer.close();

	            System.out.println("        ---> Done");
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
