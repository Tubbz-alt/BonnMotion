package edu.bonn.cs.iv.bonnmotion;

public class AmbulanceParkingPoint extends CatastropheArea {
	private static final long serialVersionUID = 3251513172589859967L;

	protected AmbulanceParkingPoint(double[] Positions) {
		super(Positions);
		if (debug) System.out.println ("AreaType: AmbulanceParkingPoint");
	}
	
	protected void InitializeSpecificValues(double[] Positions) {
		if (Positions.length < 11) {
			System.out.println("Please specify more positions for area!\naborting...");
			System.exit(0);
		}
		//ambulance parking point additionally has borderentry and borderexit
		corners = new Position[(Positions.length - 11)/2];
		for(int i = 0; i < Positions.length - 11; i = i+2){
			this.addPoint((int)Positions[i], (int)Positions[i+1]);
			corners[i/2] = new Position((int)Positions[i], (int)Positions[i+1]);
			if (debug) System.out.println ("("+(int)Positions[i]+";"+(int)Positions[i+1]+")");
		}
		this.borderentry = new Position(Positions[Positions.length - 11], Positions[Positions.length - 10]);
		this.borderexit = new Position(Positions[Positions.length - 9], Positions[Positions.length - 8]);
	}
	
	protected void SetDefaultValues() {
		this.groupsize[0] = 1;
		this.groupsize[1] = 1;	
		this.minspeed[0] = 5.0;
		this.maxspeed[0] = 12.0;
		this.minspeed[1] = 1.0;
		this.maxspeed[1] = 2.0;
	}
	
	public void print() {
		for (int i = 0; i < Positions.length - 11; i++) {
			System.out.print(Positions[i] + ",");
			if (i == (Positions.length - 12)) {
				System.out.println();
			}
		}
		System.out.println("borderentry " + borderentry + " borderexit " + borderexit + " entry " + entry.x + " " + entry.y + " exit " + exit.x + " " + exit.y + " type " + type + " wanted groups " + wantedgroups + " groupsize " + groupsize + " minspeed " + minspeed + " maxspeed " + maxspeed);
	}
	
	public String VerticesToString() {
		String represent = new String();
		for (int i = 0; i < Positions.length - 11; i++){
			Double help = new Double(Positions[i]);
			represent = represent + " " + help.toString();
		}
		return represent;
	}
	
	public boolean equals(CatastropheArea other) {
		if(type != other.type) {
			return false;
		}
		for(int i = 0; i < Positions.length - 11; i++) {
			if(Positions[i] != other.Positions[i]){
				return false;
			}
		}
		return true;
	}
}