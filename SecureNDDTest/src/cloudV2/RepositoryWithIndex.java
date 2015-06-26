/*
 * Modified by CHU Yilei on 2015 June 25:
 * Original Map<id,encrypted c> becomes Map<h,fileId>. Here index h comes from <r,t> where r is the updating counter for t.
 * The input is not "a list at once" but "one t a time"
 */

package cloudV2;

import it.unisa.dia.gas.jpbc.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.EncryptedFingerprint;
import cloud.MyCountDown;
import cloud.SecureRecord;
import cloud.SecureToken;
import cloud.SingleRepoSearchThread;
import secure.PRF;
import secure.PaillierPublicKey;
import base.Parameters;

public class RepositoryWithIndex {

	private int id;

	private Parameters params;
	
	// key to encrypt each LSH keyword (a.k.a. token)
	private Element keyV;
	
	// key to encrypt each fingerprint
	private PaillierPublicKey keyF;
	
	//private List<SecureRecord> secureRecords;
	// In this version, each "l" is grouped together (in one Map), secureRecords.size() = l
	private List<Map<Long,Integer>> secureRecords;
	
	private Map<Integer, Element> deltas;
	
	private Map<Integer, EncryptedFingerprint> encryptedFingerprints;
	
	/*
	 * frequency counter 
	 */
	private List<Map<String,Integer>> assistMaps;
	
	public RepositoryWithIndex() {
		
	}
	
	public RepositoryWithIndex(int id, Parameters params, Element keyV, PaillierPublicKey keyF) {
		
		this.id = id;
		this.params = new Parameters(params);
		//this.params = params;
		
		this.keyV = keyV;
		
		this.keyF = keyF;
		
		this.secureRecords = new ArrayList<Map<Long,Integer>>(params.lshL);
		this.assistMaps = new ArrayList<Map<String,Integer>>(params.lshL);
		
		for (int i = 0; i < params.lshL; i++) {
			this.secureRecords.add(new HashMap<Long,Integer>());
			this.assistMaps.add(new HashMap<String,Integer>());
		}
		
		this.deltas = new HashMap<Integer, Element>();
		this.encryptedFingerprints = new HashMap<Integer, EncryptedFingerprint>();
	}
	
	/**
	 * Authorize a legal user
	 * frequency
	 * @param id
	 */
	public void addDelta(int id, Element delta) {
		
		this.deltas.put(id, delta);
	}
	
//	/**
//	 * insert secure record
//	 * 
//	 * @param secureRecord
//	 */
//	public void insert(Map<Integer, SecureToken> mapOfL, int id, SecureToken token) {
//		
//		//this.secureRecords.add(secureRecord);
//		mapOfL.put(id, token);
//	}
	

	public Map<Integer, Integer> secureSearch(int uid, List<Element> tArray) {
		
		Map<Integer, Integer> searchResult = new HashMap<Integer, Integer>();
		
		if (!this.deltas.containsKey(uid)) {
			
			System.out.println("This user has not been authorized in repository (id = " + this.id + ")!");
			
			return null;
		} else {
			
			Element delta = this.deltas.get(uid);
			
			String[] at = new String[tArray.size()];
			
			// adjust the query tokens
			for (int i = 0; i < tArray.size(); i++) {
				
				at[i] = params.pairing.pairing(tArray.get(i), delta)
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
	        	
	        	SingleRepoSearchThreadV2 t = new SingleRepoSearchThreadV2("Thread " + i, threadCounter2, at[i], secureRecords.get(i), assistMaps.get(i), tempResults.get(i));

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
			
			long h = PRF.HMACSHA1ToUnsignedInt(at, tokens.get(i).getR());
			
			if (tokens.get(i).getH() == h) {
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


	/*
	 * 2015 06 25
	 */
		
	public List<Map<Long,Integer>> getSecureRecords() {
		return secureRecords;
	}

	public void setSecureRecords(List<Map<Long,Integer>> secureRecords) {
		this.secureRecords = secureRecords;
	}

	public List<Map<String, Integer>> getAssistMaps() {
		return assistMaps;
	}

	public void setAssistMaps(List<Map<String, Integer>> assistMaps) {
		this.assistMaps = assistMaps;
	}
}
