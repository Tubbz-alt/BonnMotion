/*
 * Created on Oct 7, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

import edu.bonn.cs.iv.graph.Graph;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Floyd {
	
	static public int[][][] giveMeThePathLengths(Graph G, int infinity) {
		int numNodes = G.nodeCount();
		int[][][] pathLengths = new int[numNodes][numNodes][2];
		for (int i = 0; i < numNodes; i++) {
			for (int j = i + 1; j < numNodes; j++) {
				if (G.getNode(i).getSucc(j) != null) {
					pathLengths[i][j][0] = 1;
				} else {
					pathLengths[i][j][0] = infinity;
				}
				if (G.getNode(i).getPredec(j) != null) {
					pathLengths[j][i][0] = 1;
				} else {
					pathLengths[j][i][0] = infinity;
				}
				pathLengths[i][j][1] = i;
				pathLengths[j][i][1] = j;
			}
		}
		for (int k = 0; k < numNodes; k++) {
			for (int i = 0; i < numNodes; i++) {
				for (int j = 0; j < numNodes; j++) {
					if (pathLengths[i][j][0] > pathLengths[i][k][0] + pathLengths[k][j][0]) {
						pathLengths[i][j][0] = pathLengths[i][k][0] + pathLengths[k][j][0];
						pathLengths[i][j][1] = k;
					}
				}
			}
		}
		return pathLengths;
	}
}
