package cloud;

public class DataRecord {

	private int id;
	
	private long[] secureValues;
	
	public DataRecord() {
		
	}
	
	public DataRecord(int id, long[] secureValues) {
		
		this.id = id;
		this.secureValues = secureValues;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long[] getSecureValues() {
		return secureValues;
	}

	public void setSecureValues(long[] secureValues) {
		this.secureValues = secureValues;
	}
}
