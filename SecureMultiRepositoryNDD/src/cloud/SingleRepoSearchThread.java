package cloud;

import java.util.List;
import java.util.Map;

import secure.PRF;

public class SingleRepoSearchThread extends Thread {

	private MyCountDown threadCounter;
	
	private String adjustedQueryInL;

	private Map<Integer, SecureToken> secureTokensInL;
	
	private List<Integer> resultInL;

	public SingleRepoSearchThread(String threadName, MyCountDown threadCounter,
			String adjustedQueryInL, Map<Integer, SecureToken> secureTokensInL, List<Integer> resultInL) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.adjustedQueryInL = adjustedQueryInL;
		this.secureTokensInL = secureTokensInL;
		this.resultInL = resultInL;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		for (Map.Entry<Integer, SecureToken> entry : secureTokensInL.entrySet()) {

			Integer rdId = entry.getKey();
			SecureToken secureToken = entry.getValue();
			
			long c = PRF.HMACSHA1ToUnsignedInt(adjustedQueryInL, secureToken.getR());
			
			if (secureToken.getC() == c) {
				
				resultInL.add(rdId);
			}
		}

		System.out.println(getName() + " is finished! Number of candidate: " + resultInL.size());
		threadCounter.countDown();
	}
}
