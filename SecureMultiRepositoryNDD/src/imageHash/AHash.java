package imageHash;

import local.Fingerprint;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class AHash implements ImageHashTool {

	@Override
	public Fingerprint genImageHash(Mat src, int id, int length) {
		
		// TODO: hard code the length = 64
		assert(length == 64);
		
		Fingerprint fingerprint = null;
		
		byte[] hashValue = new byte[8];

		// step 1: covert to gray scale
		Mat dst = new Mat();

		Imgproc.resize(src, dst, new Size(8, 8));
		
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2GRAY);

		// step 2: compute average of each 8*8 block

		int avgOverall = 0;

		for (int i = 0; i < dst.rows(); i++) {
			for (int j = 0; j < dst.cols(); j++) {

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
