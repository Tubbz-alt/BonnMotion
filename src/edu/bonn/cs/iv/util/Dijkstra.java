/*
 * Created on Oct 28, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

import java.util.BitSet;
import java.util.Random;

import edu.bonn.cs.iv.graph.Graph;
import edu.bonn.cs.iv.graph.GraphUtils;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Dijkstra {

	static public int[][][] giveMeThePathLengthsPerm(final Graph G, final int infinity, final long seed) {
		
		Random rng;
		if (seed > 0) {
			rng = new Random(seed);
		} else {
			rng = new Random();			
		}
		
		final int numNodes = G.nodeCount();
		final int numNodesMinusOne = numNodes - 1;
		int[][][] pathLengths = new int[numNodes][numNodes][2];

		BitSet[] succSets = new BitSet[numNodes];
		for (int i = numNodesMinusOne; i >= 0; i--) {
			succSets[i] = GraphUtils.getSuccSet(G, i);
		}

		// Set up the initial path lengths.
		for (int i = numNodesMinusOne; i >= 0; i--) {
			pathLengths[i][i][0] = 0;
			for (int j = numNodesMinusOne; j > i; j--) {
				pathLengths[i][j][0] = infinity;
				pathLengths[j][i][0] = infinity;
				//pathLengths[i][j][1] = i;
				//pathLengths[j][i][1] = j;
			}
		}

		// Now for every node do a Dijkstra.
		int[] perm = new int[numNodes];
		int[] index = new int[numNodes];
		
		for (int i = numNodesMinusOne; i >= 0; i--) {
			perm[i] = i;
			index[i] = i;
		}
		BitSet Done = new BitSet(numNodes);
		final int infinityAndOne = infinity + 1;
		BitSet succs;
		for (int src = numNodesMinusOne; src >= 0; src--) {
			Done.clear();
			// Calculate a permutation of all nodes for randomizing
			for (int i = numNodesMinusOne; i >= 0; i--) {
				int where = (int) (rng.nextDouble() * numNodes);
				//int where = (int) (Math.random() * numNodes);
				int help = perm[where];
				perm[where] = perm[i];
				index[perm[i]] = where;
				perm[i] = help;
				index[help] = i;
			}
			// flip the src back
			index[perm[src]] = index[src];
			perm[index[src]] = perm[src];
			perm[src] = src;
			index[src] = src;

			for (int chosen = numNodesMinusOne; chosen >= 0; chosen--) {
				int min = infinityAndOne;
				// w will hold the index of the nearest reachable undone node
				int w = Done.nextClearBit(0);
				for (int undone = Done.nextClearBit(0); (undone >= 0) && (undone < numNodes); undone = Done.nextClearBit(undone + 1)) {
					if (pathLengths[src][perm[undone]][0] < min) {
						min = pathLengths[src][perm[undone]][0];
						w = undone;
					}
				}
				Done.set(w);
				int node = perm[w];
				succs = succSets[node];
				// now update the rest. u is a successor of w in the real node number set (not the index set)
				for (int u = succs.nextSetBit(0); u >= 0; u = succs.nextSetBit(u + 1)) {
					if (!Done.get(index[u])) {
						if (pathLengths[src][node][0] + 1 < pathLengths[src][u][0]) {
							pathLengths[src][u][0] = pathLengths[src][node][0] + 1;
							pathLengths[src][u][1] = node;
						}
					}
				}
			}
		}
		/*for (int i = numNodesMinusOne; i >= 0; i--) {
			for (int j = numNodesMinusOne; j > i; j--) {
				if (pathLengths[i][j][0] < numNodes) {
					System.out.print("Reverse Path from " + i + " to " + j + ": " + j + " ");
					int node = j;
					while (node != i) {
						int nodePredec = pathLengths[i][node][1];
						System.out.print(nodePredec + " ");
						node = nodePredec;
					}
					System.out.println();
				} else {
					System.out.println("No path from " + i + " to " + j);		
				}
			}
		}*/
		return pathLengths;
	}

	static public short[][][] giveMeThePathLengthsPermShort(final Graph G, final short infinity) {
		final short numNodes = (short)G.nodeCount();
		final short numNodesMinusOne = (short)(numNodes - 1);
		short[][][] pathLengths = new short[numNodes][numNodes][2];

		BitSet[] succSets = new BitSet[numNodes];
		for (short i = numNodesMinusOne; i >= 0; i--) {
			succSets[i] = GraphUtils.getSuccSet(G, i);
		}

		// Set up the initial path lengths.
		for (short i = numNodesMinusOne; i >= 0; i--) {
			pathLengths[i][i][0] = 0;
			for (short j = numNodesMinusOne; j > i; j--) {
				pathLengths[i][j][0] = infinity;
				pathLengths[j][i][0] = infinity;
				//pathLengths[i][j][1] = i;
				//pathLengths[j][i][1] = j;
			}
		}

		// Now for every node do a Dijkstra.
		short[] perm = new short[numNodes];
		short[] index = new short[numNodes];
		
		for (short i = numNodesMinusOne; i >= 0; i--) {
			perm[i] = i;
			index[i] = i;
		}
		BitSet Done = new BitSet(numNodes);
		final short infinityAndOne = (short)(infinity + 1);
		BitSet succs;
		for (short src = numNodesMinusOne; src >= 0; src--) {
			Done.clear();
			// Calculate a permutation of all nodes for randomizing
			for (short i = numNodesMinusOne; i >= 0; i--) {
				short where = (short) (Math.random() * numNodes);
				short help = perm[where];
				perm[where] = perm[i];
				index[perm[i]] = where;
				perm[i] = help;
				index[help] = i;
			}
			// flip the src back
			index[perm[src]] = index[src];
			perm[index[src]] = perm[src];
			perm[src] = src;
			index[src] = src;

			for (short chosen = numNodesMinusOne; chosen >= 0; chosen--) {
				short min = infinityAndOne;
				// w will hold the index of the nearest reachable undone node
				short w = (short)Done.nextClearBit(0);
				for (short undone = (short)Done.nextClearBit(0); (undone >= 0) && (undone < numNodes); undone = (short)Done.nextClearBit(undone + 1)) {
					if (pathLengths[src][perm[undone]][0] < min) {
						min = pathLengths[src][perm[undone]][0];
						w = undone;
					}
				}
				Done.set(w);
				short node = perm[w];
				succs = succSets[node];
				// now update the rest. u is a successor of w in the real node number set (not the index set)
				for (short u = (short)succs.nextSetBit(0); u >= 0; u = (short)succs.nextSetBit(u + 1)) {
					if (!Done.get(index[u])) {
						if (pathLengths[src][node][0] + 1 < pathLengths[src][u][0]) {
							pathLengths[src][u][0] = (short)(pathLengths[src][node][0] + 1);
							pathLengths[src][u][1] = node;
						}
					}
				}
			}
		}
		/*for (int i = numNodesMinusOne; i >= 0; i--) {
			for (int j = numNodesMinusOne; j > i; j--) {
				if (pathLengths[i][j][0] < numNodes) {
					System.out.print("Reverse Path from " + i + " to " + j + ": " + j + " ");
					int node = j;
					while (node != i) {
						int nodePredec = pathLengths[i][node][1];
						System.out.print(nodePredec + " ");
						node = nodePredec;
					}
					System.out.println();
				} else {
					System.out.println("No path from " + i + " to " + j);		
				}
			}
		}*/
		return pathLengths;
	}


	static public int[][][] giveMeThePathLengths(Graph G, int infinity) {
		int numNodes = G.nodeCount();
		int[][][] pathLengths = new int[numNodes][numNodes][2];

		BitSet[] succSets = new BitSet[numNodes];
		for (int i = 0; i < numNodes; i++) {
			succSets[i] = GraphUtils.getSuccSet(G, i);
		}

		// Set up the initial path lengths.
		for (int i = 0; i < numNodes; i++) {
			pathLengths[i][i][0] = 0;
			for (int j = i + 1; j < numNodes; j++) {
				pathLengths[i][j][0] = infinity;
				pathLengths[j][i][0] = infinity;
				//pathLengths[i][j][1] = i;
				//pathLengths[j][i][1] = j;
			}
		}
		// Now for every node do a Dijkstra.
		BitSet Done;
		for (int src = 0; src < numNodes; src++) {
			Done = new BitSet(numNodes);
			for (int chosen = 0; chosen < numNodes; chosen++) {
				int min = infinity + 1;
				int w = Done.nextClearBit(0);
				for (int undone = Done.nextClearBit(0);(undone >= 0) && (undone < numNodes); undone = Done.nextClearBit(undone + 1)) {
					if (pathLengths[src][undone][0] < min) {
						min = pathLengths[src][undone][0];
						w = undone;
					} else {
						// A little randomizing
						if ((pathLengths[src][undone][0] == min) && (Math.random() < 0.5)) {
							w = undone;
						}
					}
				}
				Done.set(w);
				for (int u = succSets[w].nextSetBit(0);(u >= 0) && (u < numNodes); u = succSets[w].nextSetBit(u + 1)) {
					if (!Done.get(u)) {
						if (pathLengths[src][u][0] > pathLengths[src][w][0] + 1) {
							pathLengths[src][u][0] = pathLengths[src][w][0] + 1;
							pathLengths[src][u][1] = w;
						} else {
							// A little more randomizing
							if ((pathLengths[src][u][0] == pathLengths[src][w][0] + 1) && (Math.random() < 0.5)) {
								pathLengths[src][u][1] = w;
							}
						}
					}
				}
			}
		}

		/*for (int i = numNodes-1; i >= 0; i--) {
					for (int j = numNodes-1; j > i; j--) {
						if (pathLengths[i][j][0] < numNodes) {
							System.out.print("Reverse Path from " + i + " to " + j + ": " + j + " ");
							int node = j;
							while (node != i) {
								int nodePredec = pathLengths[i][node][1];
								System.out.print(nodePredec + " ");
								node = nodePredec;
							}
							System.out.println();
						} else {
							System.out.println("No path from " + i + " to " + j);		
						}
					}
				}*/

		return pathLengths;
	}

}