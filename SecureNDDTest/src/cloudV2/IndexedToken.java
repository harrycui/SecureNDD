package cloudV2;

import it.unisa.dia.gas.jpbc.Element;

public class IndexedToken {

	private Element t1;
	
	private Element t2;
	
	public IndexedToken(Element t1, Element t2) {
		
		this.t1 = t1;
		this.t2 = t2;
	}

	public Element getT1() {
		return t1;
	}

	public void setT1(Element t1) {
		this.t1 = t1;
	}

	public Element getT2() {
		return t2;
	}

	public void setT2(Element t2) {
		this.t2 = t2;
	}
}
