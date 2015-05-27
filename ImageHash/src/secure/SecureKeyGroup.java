package secure;

import it.unisa.dia.gas.jpbc.Element;

public class SecureKeyGroup {

	private Element keyV;
	
	private PaillierPublicKey keyPublic;
	
	private PaillierPrivateKey keyPrivate;
	
	public SecureKeyGroup(Element keyV, PaillierPublicKey keyPublic, PaillierPrivateKey keyPrivate) {
		
		this. keyV = keyV;
		this.keyPublic = keyPublic;
		this.keyPrivate = keyPrivate;
	}

	public Element getKeyV() {
		return keyV;
	}

	public void setKeyV(Element keyV) {
		this.keyV = keyV;
	}

	public PaillierPublicKey getKeyPublic() {
		return keyPublic;
	}

	public void setKeyPublic(PaillierPublicKey keyPublic) {
		this.keyPublic = keyPublic;
	}

	public PaillierPrivateKey getKeyPrivate() {
		return keyPrivate;
	}

	public void setKeyPrivate(PaillierPrivateKey keyPrivate) {
		this.keyPrivate = keyPrivate;
	}
}
