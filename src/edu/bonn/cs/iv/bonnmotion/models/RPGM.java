package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import edu.bonn.cs.iv.bonnmotion.GroupNode;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;

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
		Vector<MobileNode> rpoints = new Vector<MobileNode>();

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
					//Position xdst = dst;
					dst = new Position(c_src * src.x + c_dst * dst.x, c_src * src.y + c_dst * dst.y);
				}
				if (pGroupChange > 0.0) {
					// create dummy with current src and dst for easier paramerer passing
					MobileNode dummy = new MobileNode();
					if (!dummy.add(t, src)) {
						System.out.println(MODEL_NAME + ".main: error while adding node movement (2)");
						System.exit(0);
					}
					if (!dummy.add(next, dst)) {
						System.out.println(MODEL_NAME + ".main: error while adding node movement (3)");
						System.exit(0);
					}
					/** group to change to, null if group is not changed */
					MobileNode nRef = null;
					/** time when the link between ref and dummy gets up */
					double linkUp = duration;
					/** time when the link between ref and dummy gets down */
					double linkDown = 0.0;
					/** time when the group is changed */
					Double nNext = null;
					Double nLinkUp = duration;
					// check all reference points if currently a groupchange should happen
					for (MobileNode ref : rpoints) {
						if (ref != group) {
							// create pairStatistics
							double[] ct = MobileNode.pairStatistics(dummy, ref, t, next, maxdist, false);
							// check if the link comes up before any other link to a ref by now
							//if (ct[1] < linkUp) {
							if (ct.length > 2 && ct[2] < linkUp) {
								if (randomNextDouble() < pGroupChange) {
									linkDown = next;
									nLinkUp = ct[2];
									if (ct.length > 3) {
										linkDown = ct[3];
									}
									// change group at time tmpnext
									double tmpnext = nLinkUp + randomNextDouble() * (linkDown - nLinkUp);
									//double tmpnext = t + randomNextDouble() * (next - t);
									
									// check if group change is possible at this time
									if (this.groupChangePossible(tmpnext, ref, dummy)) {
										nNext = tmpnext;
										nRef = ref;
										linkUp = nLinkUp;
									}
								}
							}
						}
					}
					if (nRef != null) {
						// change group to nRef at time nNext
						group = nRef;
						next = nNext;
						dst = dummy.positionAt(next);
						node[i].setgroup(nRef);
					}
				}
				if (!node[i].add(next, dst)) {
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

	/**
	 * Checks if the groupchange into the given group 
	 * is currently possible for the given point
	 * depending on the calculation of speed and next 
	 * position of the group.
	 * 
	 * @param t current time
	 * @param group group node
	 * @param node the node that should change its group
	 * @return true if groupchange is currently possible, false otherwise
	 */
	private boolean groupChangePossible(final Double t, 
			final MobileNode group, final MobileNode node) {

		/* idea: build a line through the given points,
		 * walk maxdist - threshold in the other direction
		 * and check if this position can be reached by maxspeed */

		final Position refPos = group.positionAt(t);
		final Position nodePos = node.positionAt(t);

		final double threshold = 0.1;  
		final double distance = refPos.distance(nodePos);

		final double scaledDistanceToWalk = (maxdist - threshold) / distance;

		// get position of the point with max distance
		final double x = refPos.x - scaledDistanceToWalk * nodePos.x;
		final double y = refPos.y - scaledDistanceToWalk * nodePos.y;
		final Position src = new Position(x, y);

		// get time of next position of group
		final double[] groupChangeTimes = group.changeTimes();
		int currentGroupChangeTimeIndex = 0;
		while ((currentGroupChangeTimeIndex < groupChangeTimes.length) 
				&& (groupChangeTimes[currentGroupChangeTimeIndex] <= t)) {
			++currentGroupChangeTimeIndex;
		}
		if (currentGroupChangeTimeIndex >= groupChangeTimes.length) {
			return false;
		}
		// check for pause, there speed is calculated differently and may be > maxspeed
		boolean pause = (currentGroupChangeTimeIndex == 0);
		if (!pause) {
			Position pos1 = group.positionAt(groupChangeTimes[currentGroupChangeTimeIndex - 1]);
			Position pos2 = group.positionAt(groupChangeTimes[currentGroupChangeTimeIndex]);
			pause = pos1.equals(pos2);
		}
		if (pause) {
			return true;
		}
		
		final double next;
		if (currentGroupChangeTimeIndex < groupChangeTimes.length) {
			next = groupChangeTimes[currentGroupChangeTimeIndex];
		} else {
			next = duration;
		}

		// check if the calculated needed speed is <= maxspeed
		final double speed = src.distance(nodePos) / (next - t);
		return speed <= maxspeed;
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
