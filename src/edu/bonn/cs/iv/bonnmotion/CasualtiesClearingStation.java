package edu.bonn.cs.iv.bonnmotion;

public class CasualtiesClearingStation extends CatastropheArea {
	private static final long serialVersionUID = 632345523282177640L;

	protected CasualtiesClearingStation(double[] Positions) {
		super(Positions);
		if (debug) System.out.println ("AreaType: CasualtiesClearingStation");
	}
	
	protected void SetDefaultValues() {
		this.groupsize[0] = 1;
		this.groupsize[1] = 1;
		this.minspeed[0] = 1.0;
		this.maxspeed[0] = 2.0;
		this.minspeed[1] = 1.0;
		this.maxspeed[1] = 2.0;

	}
}