package base;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPowPreProcessing;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class Parameters {
	
	public Pairing pairing;
	public Element h1;
	public ElementPowPreProcessing h1Pre;
	//public Element h2;
	public Element g1;
	public Element g2;
	public Element Z;
	
	public int lshL;
	
	public int lshDimension;
	
	public int lshK;
	
	public int bitLength;
	
	public int certainty;
	
	public HammingLSH lsh;

	public Parameters(String settingPath, int lshL, int lshDimension, int lshK, int bitLength, int certainty) {

		this.pairing = PairingFactory
				.getPairing(settingPath);

		// constant element of H1, H2 function
		this.h1 = pairing.getG1().newRandomElement().getImmutable();
		
		this.h1Pre = h1.getElementPowPreProcessing();

		//this.h2 = pairing.getG2().newRandomElement().getImmutable();

		// constant element g for system
		this.g1 = pairing.getG1().newRandomElement().getImmutable();

		this.g2 = pairing.getG2().newRandomElement().getImmutable();

		this.Z = pairing.pairing(g1, g2).getImmutable();
		
		this.lshL = lshL;
		
		this.lshDimension = lshDimension;
		
		this.lshK = lshK;
		
		this.bitLength = bitLength;
		
		this.certainty = certainty;
		
		this.lsh = new HammingLSH(lshDimension, lshL, lshK);
	}
	
	public Parameters(Parameters params) {

		this.pairing = params.pairing;

		// constant element of H1, H2 function
		this.h1 = params.h1.duplicate();
		
		this.h1Pre = this.h1.getElementPowPreProcessing();

		//this.h2 = params.h2.duplicate();

		// constant element g for system
		this.g1 = params.g1.duplicate();

		this.g2 = params.g2.duplicate();

		this.Z = params.Z.duplicate();
		
		this.lshL = params.lshL;
		
		this.lshDimension = params.lshDimension;
		
		this.lshK = params.lshK;
		
		this.bitLength = params.bitLength;
		
		this.certainty = params.certainty;
		
		this.lsh = new HammingLSH(params.lshDimension, params.lshL, params.lshK);
	}
}
