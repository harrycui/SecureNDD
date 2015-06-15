package test;

import imageHash.ImageHashTool;

import java.io.File;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import local.Document;
import local.Image;
import local.NameFingerprintPair;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import util.ConfigParser;
import util.FileTool;
import util.PrintTool;

public class BatchGenFingerprint {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 1) {

			PrintTool.println(PrintTool.ERROR,
					"please check the argument list!");

			return;
		}

		ConfigParser config = new ConfigParser(args[0]);

		String inputPath = config.getString("inputPath");
		String outputPath = config.getString("outputPath");
		String outFileName = config.getString("outFileName");
		int numOfLimit = config.getInt("numOfLimit");

		List<String> filesList = new ArrayList<String>(numOfLimit);

		File[] files = new File(inputPath).listFiles();

		if (files != null) {

			File dir = new File(outputPath);
			dir.mkdirs();

			for (File file : files) {

				if (file.isFile()) {

					filesList.add(file.getName());
				}
			}
		} else {
			System.err.println("The folder is empty!");
			return;
		}

		System.out.println("\nThere are totally " + filesList.size()
				+ " images under this folder. Now, start processing ...\n");

		if (filesList.size() > 0) {

			int totalSize = (filesList.size() < numOfLimit ? filesList.size()
					: numOfLimit);

			List<Document> docs = new ArrayList<Document>(totalSize);

			for (int i = 0; i < totalSize; ++i) {

				Mat imageData = Highgui.imread(inputPath + filesList.get(i));

				Document doc = new Image(i + 1, filesList.get(i), imageData);

				doc.generate(64, ImageHashTool.HASH_TYPE_A);

				docs.add(doc);

				System.out.format(
						"Processing %3d%%  ------ Image <%s> has been read!\n",
						(int) ((double) (i + 1) / totalSize * 100),
						filesList.get(i));
			}

			PrintTool.println(PrintTool.OUT, "starting write to file >>> "
					+ outputPath + outFileName);

			FileTool.writeFingerprint2File(outputPath,
					outFileName, docs, false);
		}

		System.out.println("\nAll done!");
	}
}
