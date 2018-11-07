package edu.bonn.cs.iv.bonnmotion;

/** The event of a link going up or down at a certain point in time. */

public class LinkStatusChange {
	/** Time of link status change. */
	public final double time;
	/** Link source. */
	public final int src;
	/** Link destination. */
	public final int dst;
	/** True, if the link is going up, false, if it is going down. */
	public final boolean up;
	
	public LinkStatusChange(double time, int src, int dst, boolean up) {
		this.time = time;
		this.src = src;
		this.dst = dst;
		this.up = up;
	}
}
