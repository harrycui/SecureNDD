package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import local.Document;

public class FileTool {

	/**
	 * 
	 * Format:
	 * 
	 * id<int>::fingerprint<long>
	 * 
	 * or
	 * 
	 * id<4 byte>fingerprint<8 byte>
	 * 
	 * E.g.,
	 * 
	 * 1357::65535
	 * 
	 * @param outPath
	 * @param fileName
	 * @param docs
	 * @param type boolean: true - binary format; false - plain format
	 */
	public static void writeFingerprint2File(String outPath, String fileName,
			List<Document> docs, boolean type) {

		String filePath = outPath + fileName;

		if (docs == null || docs.isEmpty()) {

			PrintTool.println(PrintTool.WARNING, "input files are empty!");
		} else {

			// type == true : binary format
			// type == false : plain format
			if (type) {

				File file = new File(filePath);

				if (!file.getParentFile().exists()) {

					file.getParentFile().mkdirs();
				}

				DataOutputStream dos = null;

				try {

					dos = new DataOutputStream(new FileOutputStream(file));

					for (int i = 0; i < docs.size(); i++) {

						Document tmp = docs.get(i);

						dos.writeInt(tmp.getId());
						dos.write(tmp.getFingerprint().getRaw());
					}

				} catch (IOException e) {

					e.printStackTrace();

				} finally {
					try {

						if (dos != null) {
							dos.close();
						}

					} catch (final IOException e) {

						PrintTool.println(PrintTool.ERROR,
								"fail to wrtie to file!");
					}
				}
			} else {

				BufferedWriter bw = null;

				try {
					
					bw = new BufferedWriter(new FileWriter(filePath, false));
					
					for (int i = 0; i < docs.size(); i++) {

						Document tmp = docs.get(i);
						
						bw.write(tmp.getId() + "::" + tmp.getFingerprint().getValue() + "\n");
					}

				} catch (IOException e) {
					
					e.printStackTrace();
				} finally {

					if (bw != null) {
						try {
							bw.close();
						} catch (IOException e1) {

							PrintTool.println(PrintTool.ERROR,
									"fail to wrtie to file!");
						}
					}
				}
			}
		}
	}
	
	/**
	 * type == true : binary format
	 * type == false : plain format
	 * 
	 * @param inPath
	 * @param fileName
	 * @param type
	 * @return
	 */
	public static Map<Integer, BigInteger> readFingerprintFromFile(String inPath, String fileName, boolean type) {
		
		Map<Integer, BigInteger> result = new HashMap<Integer, BigInteger>();
		
		if (type) {
			
		} else {

			InputStreamReader reader = null;
			BufferedReader br = null;
			
			try {
				
				reader = new InputStreamReader(new FileInputStream(inPath + fileName));
				
	            br = new BufferedReader(reader);
	            
	            int numOfItem = 0;
	            
	            String line = br.readLine();
	            
	            while (line != null) {
					
	            	++numOfItem;
	            	
	            	StringTokenizer st = new StringTokenizer(line.replace("\n", ""), "::");
	            	
	            	result.put(Integer.valueOf(st.nextToken()), new BigInteger(st.nextToken()));
	            	
	            	line = br.readLine();
				}
	            
	            PrintTool.println(PrintTool.OUT, "Successfully read " + numOfItem + " items from " + inPath + fileName);

			} catch (IOException e) {

				e.printStackTrace();

			} finally {
				try {
					
					if (reader != null) {
						reader.close();
					}
					
					if (br != null) {
						br.close();
					}

				} catch (final IOException e) {
					PrintTool.println(PrintTool.ERROR,
							"fail to read the file!");
				}
			}
		}
		
		return result;
	}
}
