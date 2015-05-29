package base;

import java.util.Random;

import secure.PRF;

public class HammingLSH {

	// the dimension of input data
	private int dimension;
	
	// the number of LSH function
	// increase l can reduce the number of false positive
	private int l;
	
	// the number of sampling bits
	// the smaller the number is, the more items can be located, but contains more false positive
	// if the number equals the dimension of the input, the function would likely become equality testing
	private int k;
	
	// store the random index for sampling
	private int[][] hashFamily;
	
	public HammingLSH(int dimension, int l, int k) {
		
		this.dimension = dimension;
		this.l = l;
		this.k = k;
		this.hashFamily = new int[l][k];
		
		initialHashFamily();
	}
	
	private void initialHashFamily() {
		
		// the seed is used to ensure consistency in each round
		Random rand = new Random(2015);
		
		for (int i = 0; i < this.l; i++) {
			for (int j = 0; j < this.k; j++) {
				
				this.hashFamily[i][j] = rand.nextInt(dimension);
			}
		}
	}
	
	private long generateHashKey(int[] vector, int lIndex) {
		
		StringBuilder sb = new StringBuilder();
		
		int[] hashedData = new int[this.k];
		
		for (int i = 0; i < this.k; i++) {
			
			hashedData[i] = vector[hashFamily[lIndex][i]];
			sb.append(hashedData[i]);
		}
		
		return PRF.SHA256ToUnsignedInt(sb.toString());
	}
	
	/**
	 * 
	 * @param vector each element in this vector is 0 or 1
	 * @return
	 */
	public long[] computeLSH(int[] vector) {
		
		long[] lsh = new long[this.l];
		
		for (int i = 0; i < this.l; i++) {
			
			lsh[i] = generateHashKey(vector, i);
		}
		
		return lsh;
	}
}
