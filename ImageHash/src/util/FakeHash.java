package util;

import java.util.Random;

import local.Fingerprint;

import org.opencv.core.Mat;

public class FakeHash implements ImageHashTool {

	private Random rand;
	
	public FakeHash() {
		
		rand = new Random();
	}
	
	@Override
	public Fingerprint genImageHash(Mat src, int id, int length) {
		
		Fingerprint fingerprint = new Fingerprint();
		
		byte[] fakeData = new byte[length/8];
		
		for (int i = 0; i < length/8; i++) {
			
			rand.nextBytes(fakeData);
		}
		
		fingerprint.setId(id);
		
		fingerprint.setRaw(fakeData);
		
		fingerprint.genValue();
		
		return fingerprint;
	}

}
