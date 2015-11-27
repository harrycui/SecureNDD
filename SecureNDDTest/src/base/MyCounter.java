package base;

public class MyCounter {

	private Long ctr;
	
	public MyCounter() {
		
		ctr = 0L;
	}

	public Long getCtr() {
		return ctr;
	}

	public void setCtr(Long ctr) {
		this.ctr = ctr;
	}
	
	public void add1() {
		this.ctr++;
	}
}
