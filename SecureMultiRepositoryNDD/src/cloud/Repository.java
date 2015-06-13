package cloud;

import it.unisa.dia.gas.jpbc.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import secure.PRF;
import secure.PaillierPublicKey;
import base.Parameters;

public class Repository {

	private int id;

	private Parameters params;
	
	// key to encrypt each LSH keyword (a.k.a. token)
	private Element keyV;
	
	// key to encrypt each fingerprint
	private PaillierPublicKey keyF;
	
	//private List<SecureRecord> secureRecords;
	// In this version, each "l" is grouped together (in one Map), secureRecords.size() = l
	private List<Map<Integer, SecureToken>> secureRecords;
	
	private Map<Integer, Element> deltas;
	
	//private Map<Integer, NameFingerprintPair> rawRecord;
	
	private Map<Integer, EncryptedFingerprint> encryptedFingerprints;
	
	public Repository() {
		
	}
	
	public Repository(int id, Parameters params, Element keyV, PaillierPublicKey keyF) {
		
		this.id = id;
		this.params = new Parameters(params);
		//this.params = params;
		
		this.keyV = keyV;
		
		this.keyF = keyF;
		
		this.secureRecords = new ArrayList<Map<Integer, SecureToken>>(params.lshL);
		
		for (int i = 0; i < params.lshL; i++) {
			this.secureRecords.add(new HashMap<Integer, SecureToken>());
		}
		
		this.deltas = new HashMap<Integer, Element>();
		this.encryptedFingerprints = new HashMap<Integer, EncryptedFingerprint>();
		//this.rawRecord = new HashMap<Integer, NameFingerprintPair>();
	}
	
	/**
	 * Authorize a legal user
	 * 
	 * @param id
	 */
	public void addDelta(int id, Element delta) {
		
		this.deltas.put(id, delta);
	}
	
	/**
	 * insert secure record
	 * 
	 * @param secureRecord
	 */
	public void insert(Map<Integer, SecureToken> mapOfL, int id, SecureToken token) {
		
		//this.secureRecords.add(secureRecord);
		mapOfL.put(id, token);
	}
	
	
	public Map<Integer, Integer> secureSearch(int uid, List<Element> query) {
		
		Map<Integer, Integer> searchResult = new HashMap<Integer, Integer>();
		
		if (!this.deltas.containsKey(uid)) {
			
			System.out.println("This user has not been authorized in repository (id = " + this.id + ")!");
			
			return null;
		} else {
			
			Element delta = this.deltas.get(uid);
			
			String[] adjustedQuery = new String[query.size()];
			
			// adjust the query tokens
			for (int i = 0; i < query.size(); i++) {
				
				adjustedQuery[i] = params.pairing.pairing(query.get(i), delta)
				.toString();
			}
			
			// linear scan the secure tokens in repo
	        List<List<Integer>> tempResults = new ArrayList<>(this.params.lshL);
	        
	        for (int i = 0; i < this.params.lshL; i++) {
				tempResults.add(new ArrayList<>());
			}
	        
	        
	        
	        //multiple threads
	        MyCountDown threadCounter2 = new MyCountDown(this.params.lshL);
	        for (int i = 0; i < this.params.lshL; i++) {
	        	
	        	SingleRepoSearchThread t = new SingleRepoSearchThread("Thread " + i, threadCounter2, adjustedQuery[i], secureRecords.get(i), tempResults.get(i));

	            t.start();
	        }

	        // wait for all threads done
	        while (true) {
	            if (!threadCounter2.hasNext())
	                break;
	        }
			
			
			for (int i = 0; i < this.params.lshL; i++) {
				
				for (int j = 0; j < tempResults.get(i).size(); j++) {
					
					int id = tempResults.get(i).get(j);
					
					if (searchResult.containsKey(id)) {
						
						searchResult.put(id, searchResult.get(id) + 1);
					} else {
						searchResult.put(id, 1);
					}
				}
			}
		}
		
		return searchResult;
	}
	
	public boolean checkMatch(List<Element> query, SecureRecord secureRecord, Element delta) {
		
		boolean result = false;
		
		List<SecureToken> tokens = secureRecord.getSecureTokens();
		
		assert(query.size() == tokens.size());
		
		for (int i = 0; i < query.size(); i++) {
			
			String at = params.pairing.pairing(query.get(i), delta).toString();
			
			long c = PRF.HMACSHA1ToUnsignedInt(at, tokens.get(i).getR());
			
			if (tokens.get(i).getC() == c) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Element getKeyV() {
		return keyV;
	}

	public void setKeyV(Element keyV) {
		this.keyV = keyV;
	}

	public Map<Integer, Element> getDeltas() {
		return deltas;
	}

	public void setDeltas(Map<Integer, Element> deltas) {
		this.deltas = deltas;
	}

	public Map<Integer, EncryptedFingerprint> getEncryptedFingerprints() {
		return encryptedFingerprints;
	}

	public void setEncryptedFingerprints(
			Map<Integer, EncryptedFingerprint> encryptedFingerprints) {
		this.encryptedFingerprints = encryptedFingerprints;
	}

	public Parameters getParams() {
		return params;
	}

	public void setParams(Parameters params) {
		this.params = params;
	}

	public PaillierPublicKey getKeyF() {
		return keyF;
	}

	public void setKeyF(PaillierPublicKey keyF) {
		this.keyF = keyF;
	}

	public List<Map<Integer, SecureToken>> getSecureRecords() {
		return secureRecords;
	}

	public void setSecureRecords(List<Map<Integer, SecureToken>> secureRecords) {
		this.secureRecords = secureRecords;
	}
}
