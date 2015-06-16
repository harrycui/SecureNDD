package imageHash;

import local.Fingerprint;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class PHash implements ImageHashTool {

	@Override
	public Fingerprint genImageHash(Mat src, int id, int length) {
		
		// TODO: hard code the length = 64
		assert(length == 64);
		
		Fingerprint fingerprint = null;
		
		byte[] hashValue = new byte[8];

		// step 1: covert to gray scale
		
		Imgproc.resize(src, src, new Size(32, 32));
		
		Mat gray = new Mat();
		
		Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2GRAY);
		
		src.convertTo(gray, CvType.CV_32FC1);
		
		Mat dst = new Mat(gray.rows(), gray.cols(), CvType.CV_32FC1);
		Core.dct(gray, dst);
		
		//System.out.println(dst);
		
		int avgOverall = 0;
		
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {

				//System.out.println(dst.get(i, j)[0] + " ");
				avgOverall += dst.get(i, j)[0];
			}
		}
		
		avgOverall = avgOverall / 64;

		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {

				if (dst.get(y, x)[0] > avgOverall) {
					int tmp = 1 << (7 - x);
					hashValue[y] = (byte) (hashValue[y] | tmp);
				}
			}
		}

		fingerprint = new Fingerprint(id, "AHash", length, hashValue);
		
		// generate fingerprint in BigInteger format
		fingerprint.genValue();
		
		// TODO: double check this statement
		dst.release();
		src.release();
		
		return fingerprint;
	}

}
