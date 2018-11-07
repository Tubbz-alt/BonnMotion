package edu.bonn.cs.iv.bonnmotion;

public interface ScenarioLink {

	public static final int LINKMODE_FAST=0;
	public static final int LINKMODE_MOVE=1;

	/** @return the last Waypoint of transition */
	public Waypoint transition(Scenario _pre, int _mode, int _nn ) throws ScenarioLinkException;
	public Waypoint transitionWaypointFast( Waypoint _w );
	public Waypoint transitionWaypointMove( Waypoint _w, int _nn );
}
