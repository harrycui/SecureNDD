package base;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class Parameters {
	
	public Pairing pairing;
	public Element h1;
	public Element h2;
	public Element g1;
	public Element g2;
	public Element Z;
	
	public int lshL;

	public Parameters(String settingPath, int lshL) {

		this.pairing = PairingFactory
				.getPairing(settingPath);

		// constant element of H1, H2 function
		this.h1 = pairing.getG1().newRandomElement().getImmutable();

		this.h2 = pairing.getG2().newRandomElement().getImmutable();

		// constant element g for system
		this.g1 = pairing.getG1().newRandomElement().getImmutable();

		this.g2 = pairing.getG2().newRandomElement().getImmutable();

		this.Z = pairing.pairing(g1, g2).getImmutable();
		
		this.lshL = lshL;
	}
}
