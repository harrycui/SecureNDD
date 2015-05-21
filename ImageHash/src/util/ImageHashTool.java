package util;

import local.Fingerprint;

import org.opencv.core.Mat;

public interface ImageHashTool {

	public Fingerprint genImageHash(Mat src, int length);
}
