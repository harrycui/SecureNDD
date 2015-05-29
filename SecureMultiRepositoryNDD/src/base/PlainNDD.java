package base;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import local.NameFingerprintPair;

public class PlainNDD {

	public static Set<NameFingerprintPair> searchOnPlaintext(BigInteger biQ, List<NameFingerprintPair> dataset, int epsilon) {
		
		Set<NameFingerprintPair> results = new HashSet<NameFingerprintPair>();
		
		if (epsilon <= 0) {
			epsilon = 10; // default value
		}
		
		for (NameFingerprintPair item : dataset) {
			
			if (Distance.getHammingDistanceV2(biQ, item.getValue()) <= epsilon) {
				results.add(item);
			}
		}
		
		return results;
	}
}
