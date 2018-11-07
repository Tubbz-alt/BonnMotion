package edu.bonn.cs.iv.bonnmotion.models.slaw;

public final class Cluster {
	public ClusterMember[] members;
	public int index = -1;
	
	public Cluster(int _index) {
		index = _index;
	}
	
	public Cluster(int _index, ClusterMember[] _members) {
		index = _index;
		members = _members;
	}
}