package util;

import local.Fingerprint;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class DHash implements ImageHashTool {

	@Override
	public Fingerprint genImageHash(Mat src, int id, int length) {
		
		// TODO: hard code the length = 64
		assert(length == 64);
		
		Fingerprint fingerprint = null;
		
		byte[] hashValue = new byte[8];

		// step 1: covert to gray scale
		Mat dst = new Mat();

		Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2GRAY);

		Imgproc.resize(dst, dst, new Size(9, 8));

		// step 2: compute difference

		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {

				if (dst.get(y, x+1)[0] > dst.get(y, x)[0]) {
					int tmp = 1 << (7 - x);
					hashValue[y] = (byte) (hashValue[y] | tmp);
				}
			}
		}

		fingerprint = new Fingerprint(id, "AHash", length, hashValue);
		
		// generate fingerprint in BigInteger format
		fingerprint.genValue();
		
		return fingerprint;
	}

}
