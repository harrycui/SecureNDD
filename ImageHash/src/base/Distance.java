package base;

public class Distance {

	public static int getHammingDistance(String v1, String v2) {
		
		if (v1.length() != v2.length())
        {
            return -1;
        }
 
        int counter = 0;
 
        for (int i = 0; i < v1.length(); i++)
        {
            if (v1.charAt(i) != v2.charAt(i)) counter++;
        }
 
        return counter;
	}
}
