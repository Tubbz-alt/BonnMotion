package edu.bonn.cs.iv.bonnmotion.models.slaw;

import edu.bonn.cs.iv.bonnmotion.Position;

public final class ClusterMember {
	public int cluster_index = -1;
	public Position pos;
	public boolean is_visited = false;
	
	public ClusterMember(int _cluster_index, Position _pos, boolean _is_visited){
		this.cluster_index = _cluster_index;
		this.is_visited = _is_visited;
		this.pos = _pos;
	}
	
	public ClusterMember clone() {
        return new ClusterMember(this.cluster_index, this.pos, this.is_visited);
	}
}