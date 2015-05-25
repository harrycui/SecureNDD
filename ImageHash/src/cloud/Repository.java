package cloud;

import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import local.NameFingerprintPair;

public class Repository {

	private int id;
	
	private List<DataRecord> secureRecords;
	
	private long keyV;
	
	private Map<Integer, Long> deltas;
	
	private Map<Integer, NameFingerprintPair> rawRecord;
	
	public Repository() {
		
	}
	
	public Repository(int id, long keyV) {
		
		this.id = id;
		this.keyV = keyV;
		
		this.secureRecords = new ArrayList<DataRecord>();
		this.deltas = new HashMap<Integer, Long>();
		this.rawRecord = new HashMap<Integer, NameFingerprintPair>();
	}
	
	public void addDelta(int id, long delta) {
		this.deltas.put(id, delta);
	}
	
	public void insert(DataRecord record) {
		
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
		
		
		// Step 3: encrypt the LSH vector
		
		long[] secureVector = new long[4];
		
		
		
		
		DataRecord record = new DataRecord(id, secureVector);
		
		this.secureRecords.add(record);
	}
	
	
	public List<Integer> secureSearch(int id, long[] query) {
		
		if (!this.deltas.containsKey(id)) {
			
			System.out.println("This user has not been authorized in repository (id = " + this.id + ")!");
			
			return null;
		} else {
			
			long delta = this.deltas.get(id);
			
			
		}
		
		List<Integer> results = new ArrayList<Integer>();
		
		return results;
	}
	

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<DataRecord> getSecureRecords() {
		return secureRecords;
	}

	public void setSecureRecords(List<DataRecord> secureRecords) {
		this.secureRecords = secureRecords;
	}

	public long getKeyV() {
		return keyV;
	}

	public void setKeyV(long keyV) {
		this.keyV = keyV;
	}

	public Map<Integer, Long> getDeltas() {
		return deltas;
	}

	public void setDeltas(Map<Integer, Long> deltas) {
		this.deltas = deltas;
	}

	public Map<Integer, NameFingerprintPair> getRawRecord() {
		return rawRecord;
	}

	public void setRawRecord(Map<Integer, NameFingerprintPair> rawRecord) {
		this.rawRecord = rawRecord;
	}
}
