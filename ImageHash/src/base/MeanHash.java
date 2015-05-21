package base;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MeanHash extends ImageHash {
	
	
	public MeanHash() {
	}


	public static byte[] generateImageHash(Mat src) {
		
		byte[] hashValue = new byte[8];
		
		// step 1: covert to gray scale
		Mat dst = new Mat();
		
		Imgproc.resize(src, dst, new Size(256, 256));
		
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_RGB2GRAY);
		
		
		
		// step 2: compute average of each 8*8 block
		
		int avgOverall = 0;
		int[][] avgBlocks = new int[8][8];
		int[][] counterBlocks = new int[8][8];
		
		int stepX = dst.cols()/8;
		int stepY = dst.rows()/8;
		
		for (int i = 0; i < dst.rows(); i++) {
			for (int j = 0; j < dst.cols(); j++) {
				
				int innerX = j % stepX;
				int innerY = i % stepY;
				
				if (innerX > 7) {
					innerX = 7;
				}
				if (innerY > 7) {
					innerY = 7;
				}
				
				avgBlocks[innerY][innerX] += dst.get(i, j)[0];
				counterBlocks[innerY][innerX]++;
			}
		}
		
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				avgOverall += avgBlocks[y][x];
				
				avgBlocks[y][x] = avgBlocks[y][x] / counterBlocks[y][x];
			}
		}
		
		avgOverall = avgOverall / (dst.rows() * dst.cols());
		
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 8; x++) {
				
				if (avgBlocks[y][x] > avgOverall) {
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
