package edu.bonn.cs.iv.bonnmotion;

/** Mobile node belonging to a group (which itself is represented by a mobile node). */

public class GroupNode extends MobileNode {
	protected MobileNode group;

	public GroupNode(MobileNode group) {
		setgroup(group);
	}

	public MobileNode group() {
		return group;
	}

	public void setgroup(MobileNode group) {
		this.group = group;
	}
}
