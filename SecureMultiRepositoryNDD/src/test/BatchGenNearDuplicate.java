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
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import util.ConfigParser;
import util.FileTool;
import util.PrintTool;

public class BatchGenNearDuplicate {

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
		int numOfLimit = config.getInt("numOfLimit");
		
		int [] ratio = {90,80,70,60,50,40,30,20,10};

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

			for (int i = 0; i < totalSize; ++i) {

				Mat imageData = Highgui.imread(inputPath + filesList.get(i));
				
				Highgui.imwrite(outputPath + filesList.get(i), imageData);

				for (int j = 0; j < 9; j++) {
					
					Mat newImg = new Mat();

					Imgproc.resize(imageData, newImg, new Size(imageData.cols()*ratio[j]/100.0, imageData.rows()*ratio[j]/100.0));
					
					Highgui.imwrite(outputPath + "scale_" + ratio[j] + "_" + filesList.get(i), newImg);
					
					newImg.release();
				}

				System.out.format(
						"Processing %3d%%  ------ Image <%s> has been read!\n",
						(int) ((double) (i + 1) / totalSize * 100),
						filesList.get(i));
			}
		}

		System.out.println("\nAll done!");
	}
}
