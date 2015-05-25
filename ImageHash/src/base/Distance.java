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

	public static int getHammingDistanceV1(BigInteger v1, BigInteger v2) {

		int dist = 0;
		
		String sv1 = bigInteger2String(v1, 9);
		String sv2 = bigInteger2String(v2, 9);

		/*System.out.println(sv1.length() + " : " + sv1.toString());
		System.out.println(sv2.length() + " : " + sv2.toString());*/

		dist = getHammingDistance(sv1, sv2);

		//System.out.println(dist);

		return dist;
	}

	public static int getHammingDistanceV2(BigInteger v1, BigInteger v2) {

		int dist = v1.xor(v2).bitCount();

		return dist;
	}

	public static String bigInteger2String(BigInteger bi, int length) {

		String ZERO = "00000000";
		byte[] data = bi.toByteArray();

		StringBuffer sb = new StringBuffer();

		if (data.length < length) {
			int paddingNum = length - data.length;

			for (int i = 0; i < paddingNum; i++) {
				sb.append(ZERO);
			}
		}

		for (int i = 0; i < data.length; i++) {
			String s = Integer.toBinaryString(data[i]);
			if (s.length() > 8) {
				s = s.substring(s.length() - 8);
			} else if (s.length() < 8) {
				s = ZERO.substring(s.length()) + s;
			}

			sb.append(s);
		}

		return sb.toString();
	}
}
