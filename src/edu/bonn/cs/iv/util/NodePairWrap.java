/*
 * Created on 18.10.2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class NodePairWrap {
	int node1_;
	int node2_;

	public NodePairWrap(int node1, int node2) {
		node1_ = node1;
		node2_ = node2;
	}

	public int getNode1() {
		return node1_;
	}

	public int getNode2() {
		return node2_;
	}
}