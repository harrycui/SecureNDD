package cloudV2;

import java.util.List;
import java.util.Map;

import cloud.MyCountDown;
import secure.PRF;

public class SingleRepoSearchThreadV2 extends Thread {

	private MyCountDown threadCounter;

	private String atInL;

	private Map<Long, Integer> secureTokensInL;

	private Map<String, Integer> assistMapsInL;
	
	private List<Integer> resultInL;

	public SingleRepoSearchThreadV2(String threadName, MyCountDown threadCounter,
			String atInL, Map<Long,Integer> secureTokensInL, Map<String,Integer> assistMapsInL, List<Integer> resultInL) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.atInL = atInL;
		this.secureTokensInL = secureTokensInL;
		this.assistMapsInL = assistMapsInL;
		this.resultInL = resultInL;
	}

	public void run() {

		//System.out.println(getName() + " is running!");
		
		if (assistMapsInL.containsKey(atInL)) {
			
			int r = assistMapsInL.get(atInL);
			
			for (int i = 1; i <= r; i++) {
				
				long h = PRF.HMACSHA1ToUnsignedInt(atInL, Integer.toString(r));
				
				if (secureTokensInL.containsKey(h)) {
					
					int rdId = secureTokensInL.get(h);
					
					resultInL.add(rdId);
				}
			}
		}

		//System.out.println(getName() + " is finished! Number of candidate: " + resultInL.size());
		threadCounter.countDown();
	}
}
