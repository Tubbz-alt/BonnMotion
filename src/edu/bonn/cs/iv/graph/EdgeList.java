package edu.bonn.cs.iv.graph;

import edu.bonn.cs.iv.util.*;

/** This class saves adjacency lists. */

public class EdgeList {
	class EdgeWrap implements Sortable {
		public final Edge e;
		public final int key;
		public EdgeWrap(Edge e, int key) {
			this.e = e;
			this.key = key;
		}
		public int getKey() {
			return key;
		}
	}
	
	protected SortedList list = new SortedList();
	
	protected boolean keyIsSrcNode;
    
	/**
 * 	@param keyIsSrcNode If this is true, the internal list is sorted by source nodes, else by destination nodes. Naturally, the prior is used for a list of incoming links and the latter for a list of outgoing links. */
	public EdgeList(boolean keyIsSrcNode) {
		this.keyIsSrcNode = keyIsSrcNode;
	}

	protected Edge unwrap(Sortable s) {
		if (s == null)
			return null;
		else {
			EdgeWrap w = (EdgeWrap)s;
			return w.e;
		}
	}

	/** See SortedList. */
    public int add(Edge e) {
		return (list.add(new EdgeWrap(e, keyIsSrcNode ? e.srcNode().getKey() : e.dstNode().getKey())));
    }

	/** See SortedList. */
    public Edge delete(int key) {
		return unwrap(list.delete(key));
    }
	
	/** See SortedList. */
	public Edge deleteElementAt(int p) {
		return unwrap(list.deleteElementAt(p));
	}

	/** See SortedList. */
	public Edge elementAt(int p) {
		return unwrap(list.elementAt(p));
	}

	/** See SortedList. */
	public Edge get(int key) {
		Sortable s = list.get(key);
		return unwrap(s);
	}

	/** See SortedList. */
	public int indexOf(int key) {
		return list.indexOf(key);
	}

	/** See SortedList. */
	public int size() {
		return list.size();
	}
}
