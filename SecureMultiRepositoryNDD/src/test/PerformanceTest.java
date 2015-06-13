package test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.BufferedReader;
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
import base.Parameters;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.SecureToken;
import cloud.SingleRepoInsertThread;

/**
 * For performance evaluation, we just use one repository and involve the ranking mechanism.
 * 
 * The thread is in "L" level.
 * @author Helei Cui
 *
 */
public class PerformanceTest {

	public static void main(String[] args) {

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
		
		int lshL = config.getInt("lshL");
		int lshDimension = config.getInt("lshDimension");
		int lshK = config.getInt("lshK");
		int threshold = config.getInt("threshold");
		
		
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

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean rootFlag = true;

		while (rootFlag) {
			System.out
					.print("\n\n----------------------- Root Menu -----------------------\n"
							+ "Please select an operation:\n"
							+ "[1]  query test;\n"
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
					} else if (Integer.parseInt(inputStr) > 1
							|| Integer.parseInt(inputStr) < 1) {

						System.out
								.println("Warning: operation type should be limited in [1, 1], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out
							.println("Warning: operation type should be limited in [1, 1], please try again!");
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
									+ rawRecords.size()
									+ "]: (-1 means return to root menu)");

					String queryStr = null;
					int queryIndex;
					RawRecord rawQuery;
					
					try {
						queryStr = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (queryStr == null || queryStr.equals("-1")) {

							System.out.println("Return to root menu!");

							break;
						} else if (Integer.parseInt(queryStr) > rawRecords
								.size() || Integer.parseInt(queryStr) <= 0) {

							System.out
									.println("Warning: query index should be limited in [1, limit]");

							continue;
						} else {
							queryIndex = Integer.parseInt(queryStr);
							
							rawQuery = rawRecords.get(queryIndex-1);

							System.out.println("For query item id : "
									+ rawQuery.getId() + ", name : " + rawQuery.getName() + ", fingerprint : " + rawQuery.getValue());
						}
					} catch (NumberFormatException e) {
						System.out
								.println("Warning: query index should be limited in [1, "
										+ rawRecords.size() + "]");
						continue;
					}

					// prepare the query message
					List<Element> Q = new ArrayList<Element>(lshL);
					

					long[] lshVector = lsh.computeLSH(rawQuery.getValue());
					
					for (int i = 0; i < lshL; i++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lshVector[i])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
					}
					
					
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
								
								int dist = Distance.getHammingDistanceV2(rawQuery.getValue(), plainFP);
								
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
					
					/*for (int i = 0; i < numOfRepo; i++) {
						
						numOfNDD += results.get(i).size();
					}

					long time2 = System.currentTimeMillis();

					System.out.println("Cost " + (time2 - time1) + " ms.");

					if (results != null && !results.isEmpty() && numOfNDD > 0) {

						PrintTool.println(PrintTool.OUT, "there are "
								+ numOfNDD + " near-duplicates: \n");
						
						//for (int i = 0; i < repos.size(); i++) {
							
							//Repository repo = repos.get(i);
							
							for (int j = 0; j < results.get(0).size(); j++) {
								
								int id = results.get(0).get(j);
								
								// rawRecords has 1 offset!!!
								// RawRecord item = rawRecords.get(id-1);
								EncryptedFingerprint item = repo.getEncryptedFingerprints().get(id);
								
								BigInteger plainFP;
								try {
									plainFP = Paillier.Dec(item.getCipherFP(), repo.getKeyF(), csp.getKeyPrivate(repo.getId()));
									
									System.out.println(id + " :: " + item.getName() + " :: " + plainFP + " >>> dist: " + Distance.getHammingDistanceV2(rawQuery.getValue(), plainFP));	
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						//}
					} else {
						System.out.println("No similar item!!!");
					}*/
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
