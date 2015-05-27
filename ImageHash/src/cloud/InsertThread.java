package cloud;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import util.PRF;
import util.Paillier;
import base.Parameters;

public class InsertThread extends Thread {

	private MyCountDown threadCounter;

	private Repository repo;

	private List<RawRecord> rawRecords;

	public InsertThread(String threadName, MyCountDown threadCounter,
			Repository repo, List<RawRecord> rawRecords) {

		super(threadName);

		this.threadCounter = threadCounter;
		this.repo = repo;
		this.rawRecords = rawRecords;
	}

	public void run() {

		System.out.println(getName() + " is running!");
		
		Parameters params = this.repo.getParams();
		
		for (int i = 0; i < rawRecords.size(); i++) {
			
			RawRecord rd = rawRecords.get(i);

			// Step 1: compute LSH vector
			// TODO: implement the LSH functions: input-BigInteger output-long[]
			long[] lsh = new long[params.lshL];
			
			for (int j = 0; j < lsh.length; j++) {
				lsh[j] = PRF.HMACSHA1ToUnsignedInt(rd.getValue().toString(), String.valueOf(j));
			}
			
			// Step 2: encrypt the LSH vector
			
			List<SecureToken> secureTokens = new ArrayList<SecureToken>(params.lshL);
			
			for (int j = 0; j < lsh.length; j++) {
				
				Element r = params.pairing.getGT().newRandomElement().getImmutable();
				
				String strR = r.toString();
				
				//System.out.println(strR);
				
				String t = (params.pairing.pairing(params.h1Pre.pow(BigInteger.valueOf(lsh[j])), params.g2)).powZn(this.repo.getKeyV()).toString();
				
				//System.out.println(t);
				
				long c = PRF.HMACSHA1ToUnsignedInt(t, strR);
				
				//System.out.println("c = " + c);
				
				SecureToken seT = new SecureToken(strR, c);
				
				secureTokens.add(seT);
			}
			
			SecureRecord secureRecord = new SecureRecord(rd.getId(), secureTokens);
			
			this.repo.getSecureRecords().add(secureRecord);
			
			// Step 3: encrypt fingerprint
			BigInteger cipherFP = Paillier.Enc(rd.getValue(), this.repo.getKeyF());
			
			this.repo.getEncryptedFingerprints().put(rd.getId(), new EncryptedFingerprint(rd.getName(), cipherFP));
		}
		

		System.out.println(getName() + " is finished!");
		
		threadCounter.countDown();
	}
}
