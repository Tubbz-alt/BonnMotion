package edu.bonn.cs.iv.bonnmotion;

/** Catastrophe node. */

public class CatastropheNode extends MobileNode {
	public int belongsto;
	public int type;
	public Position start;
	public Position end;
	public CatastropheNode group;
	
	public CatastropheNode(int belongsto, int type, Position start, Position end) {
		super();
		this.belongsto = belongsto;
		this.type = type;
		this.start = start;
		this.end = end;
	}
	
	public CatastropheNode(CatastropheNode group) {
		super();
		this.belongsto = group.belongsto;
		this.type = group.type;
		this.start = group.start;
		this.end = group.end;
		this.group = group;
	}
	
	public void add(Position start, Position end) {
		this.start = start;
		this.end = end;
	}
	
	public CatastropheNode group() {
		return this.group;
	}
	
	public void print() {
		System.out.println("Knoten " + belongsto + " start " + start.toString() + " end " + end.toString());
	}

	public String movementString() {
		StringBuffer sb = new StringBuffer(140*waypoints.size());
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint w = (Waypoint) waypoints.elementAt(i);
			sb.append("\n");
			sb.append(w.time);
			sb.append("\n");
			sb.append(w.pos.x);
			sb.append("\n");
			sb.append(w.pos.y);
			sb.append("\n");
			sb.append(w.pos.status);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

}
