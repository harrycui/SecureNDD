package cloudWithIndex;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import cloud.MyCountDown;
import secure.PRF;
import secure.AESCoder;
import base.Parameters;

public class SingleRepoInsertThreadV2 extends Thread {

	private MyCountDown threadCounter;
	
	private Element keyV;

	private Parameters params;

	private Map<Long,Integer> hIndexInL;
	
	private Map<String,String> aIndexInL;
	
	// this is used to map the "at" and "at'"
	private Map<String,String> tmpAtMap;
	
	private Map<String,Integer> tmpRMap;

	private Map<Integer, Long> lshValuesInL;

	public SingleRepoInsertThreadV2(String threadName, MyCountDown threadCounter,
			Element keyV, Map<Integer, Long> lshValuesInL,
			Map<Long,Integer> hIndexInL, Map<String,String> aIndexInL, Parameters params) {

		super(threadName);

		this.params = new Parameters(params);
		this.threadCounter = threadCounter;

		this.keyV = keyV;
		this.lshValuesInL = lshValuesInL;
		this.aIndexInL = aIndexInL;
		this.hIndexInL = hIndexInL;
		
		this.tmpAtMap = new HashMap<String, String>();
		this.tmpRMap = new HashMap<String,Integer>();
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
			
			String at2 = (params.pairing.pairing(
					params.h11Pre.pow(BigInteger.valueOf(lshValue)), params.g2))
					.powZn(this.keyV).toString();
			
			int r = 1;
			
			if (aIndexInL.containsKey(at)) {
				r = tmpRMap.get(at);
				++r;
			} else {
				r = 1;
			}
			
			//assistMapsInL.put(at, r);

			// H2()
			long h = PRF.HMACSHA1ToUnsignedInt(at, Integer.toString(r));

			// Step 2: insert to the dict
			while (hIndexInL.containsKey(h)) {
				++r;
				h = PRF.HMACSHA1ToUnsignedInt(at, Integer.toString(r));
			}
			
			tmpAtMap.put(at, at2);
			
			tmpRMap.put(at, r);
			
			hIndexInL.put(h, rdId);
		}
		
		for (Map.Entry<String, Integer> entry : tmpRMap.entrySet()) {

			String at = entry.getKey();
			Integer rMax = entry.getValue();
			
			String at2 = tmpAtMap.get(at);
			
			try {
				String rCipher = AESCoder.encrypt(rMax.toString().getBytes(), AESCoder.toKey(PRF.SHA256(at2, 64).getBytes())).toString();
				
				aIndexInL.put(at, rCipher);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println(getName() + " is finished!");

		threadCounter.countDown();
	}
}
