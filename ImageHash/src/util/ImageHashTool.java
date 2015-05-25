package util;

import local.Fingerprint;

import org.opencv.core.Mat;

public interface ImageHashTool {
	
	public static final int HASH_FAKE = 0;
	
	public static final int HASH_TYPE_A = 1;
	
	public static final int HASH_TYPE_P = 2;
	
	
	public Fingerprint genImageHash(Mat src, int id, int length);
}
