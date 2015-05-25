package test;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import local.NameFingerprintPair;
import util.ConfigParser;
import util.FileTool;
import util.PrintTool;
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
		int limitNum = config.getInt("limitNum");
		int numOfRepo = config.getInt("numOfRepo");
		
		
		// Step 2: preprocess: setup keys and read file
		Pairing pairing = PairingFactory.getPairing("./ImageHash/src/test/d159.properties");
		
		// constant element of H1, H2 function
		Element h1 = pairing.getG1().newRandomElement().getImmutable();
		
		Element h2 = pairing.getG2().newRandomElement().getImmutable();

		// constant element g for system
		Element g1 = pairing.getG1().newRandomElement().getImmutable();
		
		Element g2 = pairing.getG2().newRandomElement().getImmutable();

		Element Z = pairing.pairing(g1, g2).getImmutable();
		
		Element[] keyVs = new Element[numOfRepo];
		
		for (int i = 0; i < keyVs.length; i++) {
			
			keyVs[i] = pairing.getZr().newRandomElement().getImmutable();
		}
		
		// key for the detector
		Element keyV0 = pairing.getZr().newRandomElement().getImmutable();
		
		// read file to lines list
		List<String> strRecords = FileTool.readLinesFromFile(inputPath, inputFileName);

		// Step 2: 

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

					long time1 = System.nanoTime();

					/*Set<NameFingerprintPair> searchResult = PlainNDD
							.searchOnPlaintext(fingerprints.get(queryIndex - 1)
									.getValue(), fingerprints, 8);*/

					long time2 = System.nanoTime();

					System.out.println("Cost " + (time2 - time1) + " ns.");

					/*if (searchResult != null && !searchResult.isEmpty()) {

						PrintTool.println(PrintTool.OUT, "there are "
								+ searchResult.size() + " near-duplicates: \n");

						// create an iterator
						Iterator<NameFingerprintPair> iterator = searchResult
								.iterator();

						int num = 1;
						// check values
						while (iterator.hasNext()) {

							NameFingerprintPair temp = iterator.next();

							System.out
									.println((num++) + " : " + temp.getName());
						}
					} else {
						System.out.println("No similar item!!!");
					}*/
				}
			}

		}
	}
}
