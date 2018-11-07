/*
 * Created on Oct 28, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.graph;

import java.util.BitSet;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GraphUtils {

	public static BitSet getPredSet(Graph G, int node) {
		int numNodes = G.nodeCount();
		BitSet bitset = new BitSet(numNodes);
		for (int i = numNodes - 1; i >= 0; i--) {
			if (isPredec(G, node, i))
				bitset.set(i);
		}
		return bitset;
	}

	public static BitSet getSuccSet(Graph G, int node) {
		int numNodes = G.nodeCount();
		BitSet bitset = new BitSet(numNodes);
		for (int i = numNodes - 1; i >= 0; i--) {
			if (isSucc(G, node, i))
				bitset.set(i);
		}
		return bitset;
	}

	public static BitSet getPredAndSuccSet(Graph G, int node) {
		int numNodes = G.nodeCount();
		BitSet bitset = new BitSet(numNodes);
		for (int i = numNodes - 1; i >= 0; i--) {
			if (isSucc(G, node, i) || isPredec(G, node, i))
				bitset.set(i);
		}
		return bitset;
	}

	public static boolean[][] getPredAndSuccSetArrays(Graph G) {
		int numNodes = G.nodeCount();
		int numNodesMinusOne = numNodes - 1;
		boolean[][] nodes = new boolean[numNodes][numNodes];
		for (int node = numNodesMinusOne; node >= 0; node--) {
			for (int i = numNodesMinusOne; i >= 0; i--) {
				if (isSucc(G, node, i) || isPredec(G, node, i))
					nodes[node][i] = true;
			}
		}
		return nodes;
	}

	/**
	 * Shortcut for checking if a node is a successor of another node.
	 * 
	 * @param G a Graph
	 * @param aNode a node
	 * @param anotherNode the node for which to check if it is a successor of aNode
	 * @return true, if anotherNode is a successor of aNode
	 */
	public static boolean isSucc(Graph G, int aNode, int anotherNode) {
		return (G.getNode(aNode).getSucc(anotherNode) != null);
	}

	/**
	 * Shortcut for checking if a node is a successor of another node.
	 * 
	 * @param G a Graph
	 * @param aNode a node
	 * @param anotherNode the node for which to check if it is a predecessor of aNode
	 * @return true, if anotherNode is a predecessor of aNode
	 */
	public static boolean isPredec(Graph G, int aNode, int anotherNode) {
		return (G.getNode(aNode).getPredec(anotherNode) != null);
	}
}
