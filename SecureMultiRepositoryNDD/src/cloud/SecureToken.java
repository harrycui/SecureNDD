package cloud;


public class SecureToken {

	private String r;
	
	private long c;

	public SecureToken(String r, long c) {
		
		this.r = r;
		this.c = c;
	}

	public String getR() {
		return r;
	}

	public void setR(String r) {
		this.r = r;
	}

	public long getC() {
		return c;
	}

	public void setC(long c) {
		this.c = c;
	}
}
