package base;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class DHash extends ImageHash {

	public DHash() {

	}

	public static byte[] generateImageHash(Mat src) {

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

		return hashValue;
	}

	public static String byteArray2String(byte[] hashValue) {

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < hashValue.length; i++) {

			for (int j = 0; j < 8; j++) {

				sb.append(hashValue[i] >> (7 - j) & 1);
			}
		}

		return sb.toString();
	}
}
