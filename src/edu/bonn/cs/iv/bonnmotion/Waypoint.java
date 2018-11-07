package edu.bonn.cs.iv.bonnmotion;

public class Waypoint {
	public final double time;
	public final Position pos;

	public Waypoint(double time, Position pos) {
		this.time = time;
		this.pos = pos;
	}
	
	public String getMovementStringPart() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.time);
		sb.append(" ");
		sb.append(this.pos.getMovementStringPart());
		
		return sb.toString();
	}
}
