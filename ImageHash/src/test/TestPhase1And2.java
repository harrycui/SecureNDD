package test;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cloud.Repository;
import local.NameFingerprintPair;
import util.ConfigParser;
import util.FileTool;
import util.PRF;
import util.PrintTool;
import base.Parameters;
import base.PlainNDD;
import base.SysConstant;

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
		
		
		// Step 1: preprocess: setup keys and read file
		Parameters params = new Parameters(pairingSettingPath, lshL);
		
		System.out.println(">>> System parameters have been initialized");
		System.out.println(">>> Now, reading the raw test data from " + inputPath + inputFileName);
		
		// keyV0 is the key for the detector
		Element keyV0 = params.pairing.getZr().newRandomElement().getImmutable();
		
		// TODO: read each line at the time of inserting
		// read file to lines list
		List<String> strRecords = FileTool.readLinesFromFile(inputPath, inputFileName);
		
		
		if (numOfLimit > strRecords.size()) {
			numOfLimit = strRecords.size();
		}
		

		// Step 2: initialize the repositories and secure insert records
		System.out.println(">>> There are " + numOfLimit + " records.");
		System.out.println(">>> Now, initializing " + numOfRepo + " repositories.");
		
		List<Repository> repos = new ArrayList<Repository>(numOfRepo);
		
		for (int i = 0; i < numOfRepo; i++) {
			
			Repository repo = new Repository(i, params);
			
			Element delta = params.g2.powZn(repo.getKeyV().div(keyV0));
					
			// id = 0 is the detector
			repo.addDelta(0, delta);
			
			repos.add(repo);
		}
		
		System.out.println(">>> Now, start inserting data into repositories...");
		
		//int avgSize = numOfLimit/numOfRepo;
		
		for (int i = 0; i < numOfLimit; i++) {
			
			//int repoId = i % avgSize;
			
			repos.get(0).insert(strRecords.get(i));
		}

		// %%%%%%%%%%%%%%%%%% test %%%%%%%%%%%%%%%%%%%

		if (strRecords == null || strRecords.isEmpty()) {

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
									+ strRecords.size()
									+ "]: (-1 means return to root menu)");

					String queryStr = null;
					int queryIndex;

					try {
						queryStr = br.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						if (queryStr == null || queryStr.equals("-1")) {

							System.out.println("Return to root menu!");

							break;
						} else if (Integer.parseInt(queryStr) > strRecords
								.size() || Integer.parseInt(queryStr) <= 0) {

							System.out
									.println("Warning: query index should be limited in [1, limit]");

							continue;
						} else {
							queryIndex = Integer.parseInt(queryStr);

							System.out.println("For query item id : "
									+ queryIndex);
						}
					} catch (NumberFormatException e) {
						System.out
								.println("Warning: query index should be limited in [1, "
										+ strRecords.size() + "]");
						continue;
					}

					// prepare the query message
					List<Element> Q = new ArrayList<Element>(lshL);
					
					// TODO: check the query data index in corresponding repo
					long[] lsh = new long[params.lshL];
					for (int i = 0; i < lsh.length; i++) {
						lsh[i] = PRF.HMACSHA1ToUnsignedInt(repos.get(0).getRawRecord().get(queryIndex).getValue().toString(), String.valueOf(i));
					}
					
					for (int i = 0; i < lshL; i++) {
						
						Element t = params.h1.pow(BigInteger.valueOf(lsh[i])).powZn(keyV0);
						
						Q.add(t);
					}
					
					
					long time1 = System.currentTimeMillis();
					
					List<List<Integer>> results = new ArrayList<List<Integer>>(numOfRepo);
					
					int numOfNDD = 0;
					
					for (int i = 0; i < numOfRepo; i++) {
						
						List<Integer> resultOfRepo = repos.get(i).secureSearch(0, Q);
						
						numOfNDD += resultOfRepo.size();
						
						results.add(resultOfRepo);
					}

					long time2 = System.currentTimeMillis();

					System.out.println("Cost " + (time2 - time1) + " ms.");

					if (results != null && !results.isEmpty() && numOfNDD > 0) {

						PrintTool.println(PrintTool.OUT, "there are "
								+ numOfNDD + " near-duplicates: \n");
						
						for (int i = 0; i < repos.size(); i++) {
							
							for (int j = 0; j < results.get(i).size(); j++) {
								
								int id = results.get(i).get(j);
								
								NameFingerprintPair item = repos.get(i).getRawRecord().get(id);
								
								System.out.println(id + " :: " + item.getName());
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
