package test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import local.NameFingerprintPair;
import util.AESCoder;
import util.ConfigParser;
import util.FileTool;
import util.PRF;
import util.Paillier;
import util.PrintTool;
import base.Parameters;
import base.SysConstant;
import cloud.CSP;
import cloud.EncryptedFingerprint;
import cloud.InsertThread;
import cloud.MyCountDown;
import cloud.RawRecord;
import cloud.Repository;
import cloud.SearchThread;

public class TestPhase1And2 {

	public static void main(String[] args) {

		if (args.length < 1) {

			PrintTool.println(PrintTool.ERROR,
					"please check the argument list!");

			return;
		}

		ConfigParser config = new ConfigParser(args[0]);

		String inputPath = config.getString("inputPath");
		String inputFileName = config.getString("inputFileName");
		// String outputPath = config.getString("outputPath");
		// String outFileName = config.getString("outFileName");
		int numOfLimit = config.getInt("numOfLimit");
		int numOfRepo = config.getInt("numOfRepo");
		String pairingSettingPath = config.getString("pairingSettingPath");
		int lshL = config.getInt("lshL");
		int bitLength = config.getInt("bitLength");
		int certainty = config.getInt("certainty");
		
		
		// Step 1: preprocess: setup keys and read file
		Parameters params = new Parameters(pairingSettingPath, lshL, bitLength, certainty);
		
		CSP csp = new CSP(params);
		
		System.out.println(">>> System parameters have been initialized");
		System.out.println(">>> Now, reading the raw test data from " + inputPath + inputFileName);
		
		// the first user is the detector
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
		
		List<Repository> repos = new ArrayList<Repository>(numOfRepo);
		
		for (int i = 0; i < numOfRepo; i++) {
			
			int rid = csp.register();
			
			Repository repo = new Repository(rid, params, csp.getKeyV(rid), csp.getKeyPublic(rid));
			
			Element delta = csp.authorize(rid, detectorId);
					
			// id = 0 is the detector
			repo.addDelta(detectorId, delta);
			
			repos.add(repo);
		}
		
		System.out.println(">>> Now, start inserting data into repositories...");
		
		
		
		/*for (int i = 0; i < numOfLimit; i++) {
			
			repos.get(0).insert(strRecords.get(i));
		}*/
		
		long stOfInsert = System.currentTimeMillis();
		
		//multiple threads
        MyCountDown threadCounter = new MyCountDown(numOfRepo);

        for (int i = 0; i < numOfRepo; i++) {

            InsertThread t = null;

            if (i == numOfRepo - 1) {

                List<RawRecord> partStrRecords = rawRecords.subList(numOfLimit / numOfRepo * i, numOfLimit);

                t = new InsertThread("Thread " + i, threadCounter, repos.get(i), partStrRecords);
            } else {

            	List<RawRecord> partStrRecords = rawRecords.subList(numOfLimit / numOfRepo * i, numOfLimit / numOfRepo * (i + 1));
                t = new InsertThread("Thread " + i, threadCounter, repos.get(i), partStrRecords);
            }

            t.start();
        }

        // wait for all threads done
        while (true) {
            if (!threadCounter.hasNext())
                break;
        }
        
        long etOfInsert = System.currentTimeMillis();
        
        System.out.println("Insert time: " + (etOfInsert - stOfInsert) + " ms.");

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
							.println("Now, you can search by input you query id range from [1, "
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
					

					long[] lsh = new long[params.lshL];
					for (int i = 0; i < lsh.length; i++) {
						lsh[i] = PRF.HMACSHA1ToUnsignedInt(rawQuery.getValue().toString(), String.valueOf(i));
					}
					
					for (int i = 0; i < lshL; i++) {
						
						Element t = params.h1Pre.pow(BigInteger.valueOf(lsh[i])).powZn(csp.getKeyV(detectorId));
						
						Q.add(t);
					}
					
					
					long time1 = System.currentTimeMillis();
					
					List<List<Integer>> results = new ArrayList<List<Integer>>(numOfRepo);
					
					for (int i = 0; i < numOfRepo; i++) {
						results.add(new ArrayList<Integer>());
					}
					
					int numOfNDD = 0;
					
					//multiple threads
			        MyCountDown threadCounter2 = new MyCountDown(numOfRepo);

			        for (int i = 0; i < numOfRepo; i++) {
			        	
			        	SearchThread t = new SearchThread("Thread " + i, threadCounter2, repos.get(i), detectorId, Q, results.get(i));

			            t.start();
			        }

			        // wait for all threads done
			        while (true) {
			            if (!threadCounter2.hasNext())
			                break;
			        }
					
					for (int i = 0; i < numOfRepo; i++) {
						
						numOfNDD += results.get(i).size();
					}

					long time2 = System.currentTimeMillis();

					System.out.println("Cost " + (time2 - time1) + " ms.");

					if (results != null && !results.isEmpty() && numOfNDD > 0) {

						PrintTool.println(PrintTool.OUT, "there are "
								+ numOfNDD + " near-duplicates: \n");
						
						for (int i = 0; i < repos.size(); i++) {
							
							Repository repo = repos.get(i);
							
							for (int j = 0; j < results.get(i).size(); j++) {
								
								int id = results.get(i).get(j);
								
								// rawRecords has 1 offset!!!
								// RawRecord item = rawRecords.get(id-1);
								EncryptedFingerprint item = repo.getEncryptedFingerprints().get(id);
								
								BigInteger plainFP;
								try {
									plainFP = Paillier.Dec(item.getCipherFP(), repo.getKeyF(), csp.getKeyPrivate(repo.getId()));
									
									System.out.println(id + " :: " + item.getName() + " :: " + plainFP);	
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					} else {
						System.out.println("No similar item!!!");
					}
				}
			}

		}
	}
}
