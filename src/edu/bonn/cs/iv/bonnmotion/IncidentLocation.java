package edu.bonn.cs.iv.bonnmotion;

public class IncidentLocation extends CatastropheArea {
	private static final long serialVersionUID = -3626112368856477535L;

	protected IncidentLocation(double[] Positions) {
		super(Positions);
		if (debug) System.out.println ("AreaType: IncidentLocation");
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