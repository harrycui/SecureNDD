package util;

import java.math.BigInteger;

public class Converter {

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
