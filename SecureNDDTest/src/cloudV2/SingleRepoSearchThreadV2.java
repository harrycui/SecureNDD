package cloudV2;

import java.util.List;
import java.util.Map;

import cloud.MyCountDown;
import secure.PRF;
import secure.AESCoder;

public class SingleRepoSearchThreadV2 extends Thread {

	private MyCountDown threadCounter;

	private String at1InL;
	
	private String at2InL;

	private Map<Long, Integer> hIndexInL;

	private Map<String, String> aIndexInL;
	
	private List<Integer> resultInL;
	
	private Map<String, List<Integer>> invertedIndexInL;

	public SingleRepoSearchThreadV2(String threadName, MyCountDown threadCounter,
			String at1InL, String at2InL, Map<Long,Integer> hIndexInL, Map<String,String> aIndexInL, List<Integer> resultInL, Map<String, List<Integer>> invertedIndexInL) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.at1InL = at1InL;
		this.at2InL = at2InL;
		this.hIndexInL = hIndexInL;
		this.aIndexInL = aIndexInL;
		this.resultInL = resultInL;
		this.invertedIndexInL = invertedIndexInL;
		
	}

	public void run() {

		//System.out.println(getName() + " is running!");
		
		if (aIndexInL.containsKey(at1InL)) {
			
			String rCipher = aIndexInL.get(at1InL);
			
			int rMax;
			
			try {
				
				rMax = Integer.parseInt(new String(AESCoder.decrypt(rCipher.getBytes(), AESCoder.toKey(PRF.SHA256(at2InL, 64).getBytes()))));
				
				for (int i = 1; i <= rMax; i++) {
					
					long h = PRF.HMACSHA1ToUnsignedInt(at1InL, Integer.toString(i));
					
					if (hIndexInL.containsKey(h)) {
						
						int rdId = hIndexInL.get(h);
						
						resultInL.add(rdId);
					}
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//System.out.println(getName() + " is finished! Number of candidate: " + resultInL.size());
		threadCounter.countDown();
	}
}
