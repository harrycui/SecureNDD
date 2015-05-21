package test;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import base.AHash;
import base.DHash;
import base.Distance;
import base.MeanHash;

public class TestMeanHash {

	public static void main(String[] args) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		if (args.length < 1) {

			System.err.println("Error: please check the argument list!");

			return;
		}
		
		Mat srcImage = Highgui.imread(args[0]);
		Mat srcImage2 = Highgui.imread(args[1]);
		
		byte[] hashValue = MeanHash.generateImageHash(srcImage);
		byte[] hashValue2 = MeanHash.generateImageHash(srcImage2);
		
		byte[] ah1 = AHash.generateImageHash(srcImage);
		byte[] ah2 = AHash.generateImageHash(srcImage2);
		
		byte[] dh1 = DHash.generateImageHash(srcImage);
		byte[] dh2 = DHash.generateImageHash(srcImage2);
		
		System.out.println(MeanHash.byteArray2String(hashValue));
		System.out.println(MeanHash.byteArray2String(hashValue2));
		
		System.out.println(MeanHash.byteArray2String(ah1));
		System.out.println(MeanHash.byteArray2String(ah2));
		
		System.out.println(MeanHash.byteArray2String(dh1));
		System.out.println(MeanHash.byteArray2String(dh2));
		
		printDistance("Mean Hash", MeanHash.byteArray2String(hashValue), MeanHash.byteArray2String(hashValue2));
		printDistance("AHash", AHash.byteArray2String(ah1), AHash.byteArray2String(ah2));
		printDistance("DHash", DHash.byteArray2String(dh1), DHash.byteArray2String(dh2));
	}
	
	public static void printDistance(String title, String h1, String h2) {
		
		System.out.println("Hamming distance of " + title + ": " + Distance.getHammingDistance(h1, h2));
	}
}
