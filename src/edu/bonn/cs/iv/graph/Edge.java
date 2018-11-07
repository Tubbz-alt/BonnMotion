package edu.bonn.cs.iv.graph;

/** This class represents an edge. */

import java.util.Hashtable;

public class Edge {
	protected Node source;
	protected Node dest;
	/** For practical reasons, this variable (saving the weight of the edge) is public. There is not much you can do wrong here, just watch out when using a weight of 0! */
	public int weight;
	
	protected Hashtable<Object,Object> label = null;
	
	public Edge(Node src, Node dst, int weight) {
		if (src.homeGraph() != dst.homeGraph())
			throw new RuntimeException("no intergraph edges!");
		if (src.homeGraph() == null)
			throw new RuntimeException("nodes must belong to a graph!");
		source = src;
		dest = dst;
		this.weight = weight;
	}
	
	public Node srcNode() {
		return source;
	}
	
	public Node dstNode() {
		return dest;
	}
	
	public Object getLabel(Object key) {
		if (label == null)
			return null;
		else
			return label.get(key);
	}
	
	public void setLabel(Object key, Object value) {
		if (label == null)
			label = new Hashtable<Object,Object>();
		label.put(key, value);
	}
	
	public Object removeLabel(Object key) {
		if (label == null)
			return null;
		else
			return label.remove(key);
	}

	public String toString() {
		return source.toString() + " " + dest.toString() + " " + weight;
	}
}
