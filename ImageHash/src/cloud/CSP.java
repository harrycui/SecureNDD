package cloud;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import base.Parameters;
import secure.PaillierPrivateKey;
import secure.PaillierPublicKey;
import secure.SecureKeyGroup;
import util.Paillier;

public class CSP {

	private Parameters params;
	
	private Map<Integer, SecureKeyGroup> mapKeys;
	
	private int numOfUser;
	
	public CSP(Parameters params) {
		
		this.params = params;
		this.mapKeys = new HashMap<Integer, SecureKeyGroup>();
		this.numOfUser = 0;
	}
	
	public int register() {
		
		int uid = ++numOfUser;
		
		// generate keyV for multi-key search
		Element keyV = params.pairing.getZr().newRandomElement().getImmutable();
		
		// generate keyF for fingerprint encryption and its corresponding private key
		Paillier paillier = new Paillier(params.bitLength, params.certainty);
		
		PaillierPublicKey keyPublic = new PaillierPublicKey(paillier.getN(), paillier.getG(), paillier.getNsquare(), params.bitLength);
		
		BigInteger u = paillier.getG().modPow(paillier.getLambda(), paillier.getNsquare()).subtract(BigInteger.ONE)
				.divide(paillier.getN()).modInverse(paillier.getN());
		
		PaillierPrivateKey keyPrivate = new PaillierPrivateKey(paillier.getLambda(), u);
		
		mapKeys.put(uid, new SecureKeyGroup(keyV, keyPublic, keyPrivate));
		
		return uid;
	}
	
	/**
	 * UID1 gives UID2 the access right.
	 * 
	 * @param uid1 data owner
	 * @param uid2 data detector
	 * @return
	 */
	public Element authorize(int uid1, int uid2) {
		
		return params.g2.powZn(getKeyV(uid1).div(getKeyV(uid2)));
	}
	
	public Element getKeyV(int uid) {
		
		return mapKeys.get(uid).getKeyV();
	}
	
	public PaillierPublicKey getKeyPublic(int uid) {
		
		return mapKeys.get(uid).getKeyPublic();
	}
	
	public PaillierPrivateKey getKeyPrivate(int uid) {
		
		return mapKeys.get(uid).getKeyPrivate();
	}
}
