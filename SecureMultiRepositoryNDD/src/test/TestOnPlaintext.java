package test;

import it.unisa.dia.gas.jpbc.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cloud.RawRecord;
import base.Distance;
import base.MyAnalysis;
import base.PlainNDD;
import base.SysConstant;
import local.NameFingerprintPair;
import util.ConfigParser;
import util.FileTool;
import util.PrintTool;

public class TestOnPlaintext {

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
		int threshold = config.getInt("threshold");
		int numOfPositive = config.getInt("numOfPositive");

		List<NameFingerprintPair> fingerprints = FileTool
				.readFingerprintFromFile2List(inputPath, inputFileName, false);

		// %%%%%%%%%%%%%%%%%% test %%%%%%%%%%%%%%%%%%%

		if (fingerprints == null || fingerprints.isEmpty()) {

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
							+ "[QUIT] quit system.\n\n" + "--->");
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
					} else if (Integer.parseInt(inputStr) > 2
							|| Integer.parseInt(inputStr) < 1) {

						System.out
								.println("Warning: operation type should be limited in [1, 2], please try again!");

						continue;
					} else {
						operationType = Integer.parseInt(inputStr);
					}
				} catch (NumberFormatException e) {
					System.out
							.println("Warning: operation type should be limited in [1, 2], please try again!");
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
									+ fingerprints.size()
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
						} else if (Integer.parseInt(queryStr) > fingerprints
								.size() || Integer.parseInt(queryStr) <= 0) {

							System.out
									.println("Warning: query index should be limited in [1, limit]");

							continue;
						} else {
							queryIndex = Integer.parseInt(queryStr);

							System.out.println("For query item id : "
									+ queryIndex
									+ ", name : "
									+ fingerprints.get(queryIndex - 1)
											.getName()
									+ ", fingerprint : "
									+ fingerprints.get(queryIndex - 1)
											.getValue());
						}
					} catch (NumberFormatException e) {
						System.out
								.println("Warning: query index should be limited in [1, "
										+ fingerprints.size() + "]");
						continue;
					}

					long time1 = System.nanoTime();

					Set<NameFingerprintPair> searchResult = PlainNDD
							.searchOnPlaintext(fingerprints.get(queryIndex - 1)
									.getValue(), fingerprints, threshold);

					long time2 = System.nanoTime();

					System.out.println("Cost " + (time2 - time1) + " ns.");

					if (searchResult != null && !searchResult.isEmpty()) {

						PrintTool.println(PrintTool.OUT, "there are "
								+ searchResult.size() + " near-duplicates: \n");

						// create an iterator
						Iterator<NameFingerprintPair> iterator = searchResult
								.iterator();

						int num = 1;
						// check values
						while (iterator.hasNext()) {

							NameFingerprintPair temp = iterator.next();

							System.out.println((num++)
									+ " : "
									+ temp.getName()
									+ " :: "
									+ temp.getValue()
									+ " >>> dist: "
									+ Distance.getHammingDistanceV2(
											fingerprints.get(queryIndex - 1)
													.getValue(), temp
													.getValue()));
						}
					} else {
						System.out.println("No similar item!!!");
					}
				}
			} else if (operationType == SysConstant.OPERATION_ANALYZE) {

				NameFingerprintPair query;

				long avgSearchTime = 0;

				float avgRecall = 0;

				float avgPrecision = 0;

				int queryTimes = 0;

				for (int i = 0; i < fingerprints.size(); i++) {

					query = fingerprints.get(i);

					// Only check the image, of which name contains "original"
					if (query.getName().contains("original")) {

						System.out.println(++queryTimes);

						long time1 = System.currentTimeMillis();

						Set<NameFingerprintPair> searchResult = PlainNDD
								.searchOnPlaintext(fingerprints.get(i)
										.getValue(), fingerprints, threshold);

						long time2 = System.currentTimeMillis();

						avgSearchTime += time2 - time1;

						avgRecall += computeRecall(query.getName(),
								searchResult, fingerprints, numOfPositive);

						avgPrecision += computePrecision(query.getName(),
								searchResult, fingerprints);
					}
				}

				// print the statistics
				System.out.println("Average search time is : " + avgSearchTime
						/ (float) queryTimes + " ms");
				System.out.println("Average recall is      : " + avgRecall
						/ queryTimes + " %");
				System.out.println("Average precision is   : " + avgPrecision
						/ queryTimes + " %");
			}

		}
	}

	public static float computeRecall(String queryName,
			Set<NameFingerprintPair> searchResult,
			List<NameFingerprintPair> fingerprints, int numOfPositive) {

		int numOfTruePositive = 0;
		// int numOfPositive = 0;

		// create an iterator
		Iterator<NameFingerprintPair> iterator = searchResult.iterator();

		while (iterator.hasNext()) {

			NameFingerprintPair record = iterator.next();

			if (checkTruePositive(queryName, record.getName())) {

				numOfTruePositive++;
			}
		}

		return (float) numOfTruePositive / numOfPositive;
	}

	public static float computePrecision(String queryName,
			Set<NameFingerprintPair> searchResult,
			List<NameFingerprintPair> fingerprints) {

		int numOfTruePositive = 0;
		int numOfMatches = searchResult.size();

		Iterator<NameFingerprintPair> iterator = searchResult.iterator();

		while (iterator.hasNext()) {

			NameFingerprintPair record = iterator.next();

			if (checkTruePositive(queryName, record.getName())) {

				numOfTruePositive++;
			}
		}

		return (float) numOfTruePositive / numOfMatches;
	}

	public static boolean checkTruePositive(String queryName, String testName) {

		String name1 = queryName.substring(queryName.lastIndexOf("_"),
				queryName.lastIndexOf("."));

		String name2 = testName.substring(testName.lastIndexOf("_"),
				testName.lastIndexOf("."));

		return (name1.equals(name2));
	}
}
