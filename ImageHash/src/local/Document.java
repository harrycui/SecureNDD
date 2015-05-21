package local;

public abstract class Document {

	private int id;
	
	private Fingerprint fingerprint;
	
	public abstract void generate(int length, int type);
	
	public Document(int id) {
		super();
		this.id = id;
	}

	public Document(int id, Fingerprint fingerprint) {
		super();
		this.id = id;
		this.fingerprint = fingerprint;
	}



	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Fingerprint getFingerprint() {
		return fingerprint;
	}

	public void setFingerprint(Fingerprint fingerprint) {
		this.fingerprint = fingerprint;
	}
}
