package test;

public class TestFunction {

	public static void main(String[] args) {
		
		String ZERO="00000000";
		
		byte[] data = new byte[8];
		
		data[0] = 1;
		
		StringBuffer sb = new StringBuffer();
		
		for   (int i = 0;i<data.length;i++)   {
			String   s   =   Integer.toBinaryString(data[i]);
			if   (s.length()   >
			8)   {
				s   =   s.substring(s.length()   -   8);
			}   else   if   (s.length()  
					<   8)   {
				s   =   ZERO.substring(s.length())   +   s;
			}
			//System.out.println(s);
			sb.append(s);
		}
		
		System.out.println(sb);
	}
}
