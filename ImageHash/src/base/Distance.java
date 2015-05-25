package base;

import java.math.BigInteger;

public class Distance {

	public static int getHammingDistance(String v1, String v2) {

		if (v1.length() != v2.length()) {
			return -1;
		}

		int counter = 0;

		for (int i = 0; i < v1.length(); i++) {
			if (v1.charAt(i) != v2.charAt(i))
				counter++;
		}

		return counter;
	}

	public static int getHammingDistance(BigInteger v1, BigInteger v2) {

		int dist = 0;
		String ZERO = "00000000";
		byte[] data1 = v1.toByteArray();
		byte[] data2 = v2.toByteArray();
		
		/*BigInteger x = v1.xor(v2);
		byte[] datax = x.toByteArray();
		
		StringBuffer sbx = new StringBuffer();

		for (int i = 0; i < datax.length; i++) {
			String s = Integer.toBinaryString(datax[i]);
			if (s.length() > 8) {
				s = s.substring(s.length() - 8);
			} else if (s.length() < 8) {
				s = ZERO.substring(s.length()) + s;
			}
			// System.out.println(s);
			sbx.append(s);
		}*/

		StringBuffer sb1 = new StringBuffer();
		
		if (data1.length < 8) {
			int paddingNum = 8 - data1.length;
			
			for (int i = 0; i < paddingNum; i++) {
				sb1.append(ZERO);
			}
		}

		for (int i = 0; i < data1.length; i++) {
			String s = Integer.toBinaryString(data1[i]);
			if (s.length() > 8) {
				s = s.substring(s.length() - 8);
			} else if (s.length() < 8) {
				s = ZERO.substring(s.length()) + s;
			}
			// System.out.println(s);
			sb1.append(s);
		}

		StringBuffer sb2 = new StringBuffer();
		
		if (data2.length < 8) {
			int paddingNum = 8 - data2.length;
			
			for (int i = 0; i < paddingNum; i++) {
				sb2.append(ZERO);
			}
		}

		for (int i = 0; i < data2.length; i++) {
			String s = Integer.toBinaryString(data2[i]);
			if (s.length() > 8) {
				s = s.substring(s.length() - 8);
			} else if (s.length() < 8) {
				s = ZERO.substring(s.length()) + s;
			}
			// System.out.println(s);
			sb2.append(s);
		}

		System.out.println(sb1.toString().length() + " : " + sb1.toString());
		System.out.println(sb2.toString().length() + " : " + sb2.toString());
		//System.out.println(sbx.toString().length() + " : " + sbx.toString());
		
		dist = getHammingDistance(sb1.toString(), sb2.toString());
		
		System.out.println(dist);
		/*
		 * BigInteger x = v1.xor(v2);
		 * 
		 * while (x.signum() != 0) { dist += 1; x = x.and(x.subtract(new
		 * BigInteger("1"))); }
		 */

		return dist;
	}
}
