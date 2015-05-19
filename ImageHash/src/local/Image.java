package local;

import org.opencv.core.Mat;

import util.AHash;
import util.FakeHash;
import util.ImageHashTool;

public class Image extends Document {

	private Mat data;
	
	public Image(int id, Mat src) {
		super(id);
		this.data = src;
	}

	@Override
	public void generate(int length, int type) {
		
		ImageHashTool hashTool = null;
		
		if (type == ImageHashTool.HASH_TYPE_A) {
			
			hashTool = new AHash();
		} else if (type == ImageHashTool.HASH_TYPE_P) {
			
			hashTool = new AHash();
		} else if (type == ImageHashTool.HASH_FAKE) {
			
			hashTool = new FakeHash();
		} else {
			
			System.err.println("ERROR: Please check the image hash type!");
		}
		
		if (hashTool != null) {
			
			setFingerprint(hashTool.genImageHash(this.data, length));
		}
		
		if (getFingerprint() != null) {
			
			System.out.println("ID: " + getId() + " >>> generates fingerprint successfully.");
		}
	}

	public Mat getData() {
		return data;
	}

	public void setData(Mat data) {
		this.data = data;
	}
}
