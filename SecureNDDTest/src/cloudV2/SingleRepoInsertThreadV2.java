package cloudV2;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.Map;

import cloud.MyCountDown;
import secure.PRF;
import base.Parameters;

public class SingleRepoInsertThreadV2 extends Thread {

	private MyCountDown threadCounter;

	//private Repository repo;
	private Element keyV;

	private Parameters params;

	private Map<Long,Integer> secureTokensInL;
	
	private Map<String,Integer> assistMapsInL;

	private Map<Integer, Long> lshValuesInL;

	public SingleRepoInsertThreadV2(String threadName, MyCountDown threadCounter,
			Element keyV, Map<Integer, Long> lshValuesInL,
			Map<Long,Integer> secureTokensInL, Map<String,Integer> assistMapsInL, Parameters params) {

		super(threadName);

		this.params = new Parameters(params);
		this.threadCounter = threadCounter;

		this.keyV = keyV;
		this.lshValuesInL = lshValuesInL;
		this.assistMapsInL = assistMapsInL;
		this.secureTokensInL = secureTokensInL;
	}

	public void run() {

		System.out.println(getName() + " is running!");

		for (Map.Entry<Integer, Long> entry : lshValuesInL.entrySet()) {

			Integer rdId = entry.getKey();
			long lshValue = entry.getValue();

			// Step 1: encrypt each LSH value
			// pairing + H1()
			String at = (params.pairing.pairing(
					params.h1Pre.pow(BigInteger.valueOf(lshValue)), params.g2))
					.powZn(this.keyV).toString();
			
			int r = 1;
			
			if (assistMapsInL.containsKey(at)) {
				r = assistMapsInL.get(at);
				++r;
			} else {
				r = 1;
			}
			
			//assistMapsInL.put(at, r);

			// H2()
			long h = PRF.HMACSHA1ToUnsignedInt(at, Integer.toString(r));

			// Step 2: insert to the dict
			while (secureTokensInL.containsKey(h)) {
				++r;
				h = PRF.HMACSHA1ToUnsignedInt(at, Integer.toString(r));
			}
			
			assistMapsInL.put(at, r);
			
			secureTokensInL.put(h, rdId);
		}

		System.out.println(getName() + " is finished!");

		threadCounter.countDown();
	}
}
