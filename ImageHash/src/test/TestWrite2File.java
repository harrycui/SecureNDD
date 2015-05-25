package test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import local.Document;
import local.Image;
import local.NameFingerprintPair;
import util.FileTool;
import util.ImageHashTool;

public class TestWrite2File {

	
	public static void main(String[] args) {
		
		List<Document> docs = new ArrayList<Document>();
		
		for (int i = 0; i < 1000; i++) {
			
			Document doc = new Image(i+1, null, null);
			
			doc.generate(64, ImageHashTool.HASH_FAKE);
			
			docs.add(doc);
		}
		
		FileTool.writeFingerprint2File("/home/ubuntu/infocom2016/", "testout.txt", docs, false);
		
		Map<Integer, NameFingerprintPair> fingerprints = FileTool.readFingerprintFromFile("/home/ubuntu/", "testout.txt", false);
		
		for (int i = 0; i < fingerprints.size(); i++) {
			
			BigInteger bi = fingerprints.get(i+1).getValue();
			
			if (bi.longValue() > Long.MAX_VALUE || bi.longValue() < Long.MIN_VALUE) {
				System.out.println((i+1) + "::" + bi);
			}
		}
	}
}
