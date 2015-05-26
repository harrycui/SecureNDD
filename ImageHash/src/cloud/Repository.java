package cloud;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import local.NameFingerprintPair;
import util.PRF;
import base.Parameters;

public class Repository {

	private int id;
	
	private List<SecureRecord> secureRecords;
	
	private Element keyV;
	
	private Map<Integer, Element> deltas;
	
	private Map<Integer, NameFingerprintPair> rawRecord;
	
	private Parameters params;
	
	public Repository() {
		
	}
	
	public Repository(int id, Parameters params) {
		
		this.id = id;
		this.params = params;
		
		this.keyV = params.pairing.getZr().newRandomElement().getImmutable();
		
		this.secureRecords = new ArrayList<SecureRecord>();
		this.deltas = new HashMap<Integer, Element>();
		this.rawRecord = new HashMap<Integer, NameFingerprintPair>();
	}
	
	/**
	 * Authorize a legal user
	 * 
	 * @param id
	 */
	public void addDelta(int id, Element delta) {
		
		this.deltas.put(id, delta);
	}
	
	public void insert(SecureRecord record) {
		
		this.secureRecords.add(record);
	}
	
	/**
	 * Read data record as a String
	 * 
	 * @param strRecord [id]::[name]::[fingerprint]
	 */
	public void insert(String strRecord) {
		
		// Step 1: convert the string into id + name + fingerprint
		
		StringTokenizer st = new StringTokenizer(strRecord.replace("\n", ""), "::");

		int id = Integer.valueOf(st.nextToken());
		String name = st.nextToken();
		BigInteger value = new BigInteger(st.nextToken());
		
		this.rawRecord.put(id, new NameFingerprintPair(name, value));
		
		// Step 2: compute LSH vector
		// TODO: implement the LSH functions: input-BigInteger output-long[]
		long[] lsh = new long[params.lshL];
		for (int i = 0; i < lsh.length; i++) {
			lsh[i] = PRF.HMACSHA1ToUnsignedInt(value.toString(), String.valueOf(i));
		}
		
		// Step 3: encrypt the LSH vector
		
		List<SecureToken> secureTokens = new ArrayList<SecureToken>(params.lshL);
		
		for (int i = 0; i < lsh.length; i++) {
			
			Element r = params.pairing.getGT().newRandomElement().getImmutable();
			
			String strR = r.toString();
			
			//System.out.println(strR);
			
			String t = (params.pairing.pairing(params.h1.pow(BigInteger.valueOf(lsh[i])), params.g2)).powZn(keyV).toString();
			
			//System.out.println(t);
			
			long c = PRF.HMACSHA1ToUnsignedInt(t, strR);
			
			//System.out.println("c = " + c);
			
			SecureToken seT = new SecureToken(strR, c);
			
			secureTokens.add(seT);
		}
		
		SecureRecord record = new SecureRecord(id, secureTokens);
		
		this.secureRecords.add(record);
	}
	
	
	public List<Integer> secureSearch(int id, List<Element> query) {
		
		List<Integer> results = new ArrayList<Integer>();
		
		if (!this.deltas.containsKey(id)) {
			
			System.out.println("This user has not been authorized in repository (id = " + this.id + ")!");
			
			return null;
		} else {
			
			Element delta = this.deltas.get(id);
			
			// linear scan the secure tokens in repo
			for (int i = 0; i < this.secureRecords.size(); i++) {
				
				SecureRecord secureRecord = this.secureRecords.get(i);
				
				boolean isMatch = this.checkMatch(query, secureRecord, delta);
				
				if (isMatch) {
					results.add(secureRecord.getId());
				}
			}
		}
		
		return results;
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

	public List<SecureRecord> getSecureRecords() {
		return secureRecords;
	}

	public void setSecureRecords(List<SecureRecord> secureRecords) {
		this.secureRecords = secureRecords;
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

	public Map<Integer, NameFingerprintPair> getRawRecord() {
		return rawRecord;
	}

	public void setRawRecord(Map<Integer, NameFingerprintPair> rawRecord) {
		this.rawRecord = rawRecord;
	}
}
