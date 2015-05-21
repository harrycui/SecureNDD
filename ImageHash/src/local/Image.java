package local;

import org.opencv.core.Mat;

import util.AHash;
import util.ImageHashTool;
import base.SysConstant;

public class Image extends Document {

	private Mat data;
	
	public Image(int id, Mat src) {
		super(id);
		this.data = src;
	}

	@Override
	public void generate(int length, int type) {
		
		ImageHashTool hashTool = null;
		
		if (type == SysConstant.HASH_TYPE_A) {
			
			hashTool = new AHash();
		} else if (type == SysConstant.HASH_TYPE_P) {
			
			hashTool = new AHash();
		} else {
			
			System.err.println("ERROR: Please check the image hash type!");
		}
		
		if (hashTool != null) {
			
			setFingerprint(hashTool.genImageHash(this.data, length));
		}
		
		if (getFingerprint() != null) {
			
			System.out.println("ID: " + getId() + " generates fingerpritn successfully.");
		}
	}

	public Mat getData() {
		return data;
	}

	public void setData(Mat data) {
		this.data = data;
	}
}
