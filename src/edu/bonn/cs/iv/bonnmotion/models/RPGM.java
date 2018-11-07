/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2005 University of Bonn                                **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import edu.bonn.cs.iv.bonnmotion.GroupNode;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Application to create movement scenarios according to the Reference Point Group Mobility model. */

public class RPGM extends RandomSpeedBase {

	private static final String MODEL_NAME = "RPGM";

	/** Maximum deviation from group center [m]. */
	protected double maxdist = 2.5;
	/** Average nodes per group. */
	protected double avgMobileNodesPerGroup = 3.0;
	/** Standard deviation of nodes per group. */
	protected double groupSizeDeviation = 2.0;
	/** The probability for a node to change to a new group when moving into it's range. */
	protected double pGroupChange = 0.01;
	/** Number of groups (not an input parameter!). */
	protected int groups = 0;
	/** Size of largest group (not an input parameter!). */
	protected int maxGroupSize = 0;
	
	public RPGM(int nodes, double x, double y, double duration, double ignore, long randomSeed, double minspeed, double maxspeed, double maxpause, double maxdist, double avgMobileNodesPerGroup, double groupSizeDeviation, double pGroupChange) {
		super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed, maxpause);
		this.maxdist = maxdist;
		this.avgMobileNodesPerGroup = avgMobileNodesPerGroup;
		this.groupSizeDeviation = groupSizeDeviation;
		this.pGroupChange = pGroupChange;
		generate();
	}

	public RPGM( String[] args ) {
			go( args );
	}

	public void go( String args[] ) {
		super.go(args);
		generate();
	}



	public void generate() {
		preGeneration();
		
		GroupNode[] node = new GroupNode[this.node.length];
		Vector rpoints = new Vector();
		
		// groups move in a random waypoint manner:

		int nodesRemaining = node.length;
		int offset = 0;
		while (nodesRemaining > 0) {
//			System.out.println("go: reference points. nodes remaining: " + nodesRemaining);
			MobileNode ref = new MobileNode();
			rpoints.addElement(ref);
			double t = 0.0;
			Position src = new Position((x - 2 * maxdist) * randomNextDouble() + maxdist, (y - 2 * maxdist) * randomNextDouble() + maxdist);
			if (! ref.add(0.0, src)) {
				System.out.println("RPGM.main: error while adding group movement (1)");
				System.exit(0);
			}
			while (t < duration) {
				Position dst = new Position((x - 2 * maxdist) * randomNextDouble() + maxdist, (y - 2 * maxdist) * randomNextDouble() + maxdist);
				double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
				t += src.distance(dst) / speed;
				if (! ref.add(t, dst)) {
					System.out.println("RPGM.main: error while adding group movement (2)");
					System.exit(0);
				}
				if ((t < duration) && (maxpause > 0.0)) {
					double pause = maxpause * randomNextDouble();
					if (pause > 0.0) {
						t += pause;
						if (! ref.add(t, dst)) {
							System.out.println(MODEL_NAME + ".main: error while adding node movement (3)");
							System.exit(0);
						}
					}
				}
				src = dst;
			}

			// define group size:

//			System.out.println("go: group size?");
			int size;
			while ((size = (int)Math.round(randomNextGaussian() * groupSizeDeviation + avgMobileNodesPerGroup)) < 1);
//			System.out.println("go: group size: " + size);
			if (size > nodesRemaining)
				size = nodesRemaining;
			if (size > maxGroupSize)
				maxGroupSize = size;
			nodesRemaining -= size;
			offset += size;
			for (int i = offset - size; i < offset; i++)
				node[i] = new GroupNode(ref);
			groups++;
		}

		// nodes follow their reference points:

		for (int i = 0; i < node.length; i++) {
//			System.out.println("go: node " + (i + 1) + "/" + node.length);
			double t = 0.0;
			MobileNode group = node[i].group();
			
			Position src = group.positionAt(t).rndprox(maxdist, randomNextDouble(), randomNextDouble());
			if (! node[i].add(0.0, src)) {
				System.out.println(MODEL_NAME + ".main: error while adding node movement (1)");
				System.exit(0);
			}
			while (t < duration) {
				double[] gm = group.changeTimes();
				int gmi = 0;
				while ((gmi < gm.length) && (gm[gmi] <= t))
					gmi++;
				boolean pause = (gmi == 0);
				if (! pause) {
					Position pos1 = group.positionAt(gm[gmi-1]);
					Position pos2 = group.positionAt(gm[gmi]);
					pause = pos1.equals(pos2);
				}
				double next = (gmi < gm.length) ? gm[gmi] : duration;
				Position dst; double speed;
				do {
					dst = group.positionAt(next).rndprox(maxdist, randomNextDouble(), randomNextDouble());
					speed = src.distance(dst) / (next - t);
				} while ((! pause) && (speed > maxspeed));
				if (speed > maxspeed) {
					double c_dst = ((maxspeed - minspeed) * randomNextDouble() + minspeed) / speed;
					double c_src = 1 - c_dst;
					Position xdst = dst;
					dst = new Position(c_src * src.x + c_dst * dst.x, c_src * src.y + c_dst * dst.y);
				}
				if (pGroupChange > 0.0) {
					MobileNode dummy = new MobileNode();
					if (! dummy.add(t, src)) {
						System.out.println(MODEL_NAME + ".main: error while adding node movement (2)");
						System.exit(0);
					}
					if (! dummy.add(next, dst)) {
						System.out.println(MODEL_NAME + ".main: error while adding node movement (3)");
						System.exit(0);
					}
					MobileNode nRef = null;
					double lUp = duration;
					double lDown = 0.0;
					for (int j = 0; j < rpoints.size(); j++) {
						MobileNode ref = (MobileNode)rpoints.elementAt(j);
						double[] ct = MobileNode.pairStatistics(dummy, ref, t, next, maxdist, false);
						if (ct.length > 1) {
							if ((ct[1] < lUp) && (randomNextDouble() < pGroupChange)) {
								nRef = ref;
								lUp = ct[1];
								lDown = next;
								if (ct.length > 2)
									lDown = ct[2];
							}
						}
					}
					if (nRef != null) { // change group
						next = lUp + randomNextDouble() * (lDown - lUp);
						dst = dummy.positionAt(next);
						node[i].setgroup(nRef);
					}
				}
				if (! node[i].add(next, dst)) {
					System.out.println(MODEL_NAME + ".main: error while adding node movement (4)");
					System.exit(0);
				}
				src = dst;
				t = next;
			}
		}

		// write the nodes into our base

		this.node = node;

		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
			if (	key.equals("groupsize_E") ) {
				avgMobileNodesPerGroup = Double.parseDouble(value);
				return true;
			} else if (	key.equals("groupsize_S") ) {
				groupSizeDeviation = Double.parseDouble(value);
				return true;
			} else if (	key.equals("pGroupChange") ) {
				pGroupChange = Double.parseDouble(value);
				return true;
			} else if (	key.equals("maxdist") ) {
				maxdist = Double.parseDouble(value);
				return true;
			} else return super.parseArg(key, value);
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[4];

		p[0] = "groupsize_E=" + avgMobileNodesPerGroup;
		p[1] = "groupsize_S=" + groupSizeDeviation;
		p[2] = "pGroupChange=" + pGroupChange;
		p[3] = "maxdist=" + maxdist;

		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'a': // "avgMobileNodesPerGroup"
				avgMobileNodesPerGroup = Double.parseDouble(val);
				return true;
			case 'c': // "change"
				pGroupChange = Double.parseDouble(val);
				return true;
			case 'r': // "random vector max length"
				maxdist = Double.parseDouble(val);
				return true;
			case 's': // "groupSizeDeviation"
				groupSizeDeviation = Double.parseDouble(val);
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		RandomSpeedBase.printHelp();
		System.out.println( MODEL_NAME + ":" );
		System.out.println("\t-a <average no. of nodes per group>");
		System.out.println("\t-c <group change probability>");
		System.out.println("\t-r <max. distance to group center>");
		System.out.println("\t-s <group size standard deviation>");
	}
}
