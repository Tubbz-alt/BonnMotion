package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.GroupNode;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;

/** Application to create movement scenarios according to the Nomadic Mobility model. */

public class Nomadic extends RandomSpeedBase {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("Nomadic");
        info.description = "Application to create movement scenarios according to the Nomadic community Mobility model";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 252 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Chris Walsh");
        
        info.affiliation = ModuleInfo.TOILERS;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	/** Maximum deviation from group center [m]. */
	protected double maxdist = 2.5;
	/** Average nodes per group. */
	protected double avgMobileNodesPerGroup = 3.0;
	/** Standard deviation of nodes per group. */
	protected double groupSizeDeviation = 2.0;
	/** Reference point max pause */
	protected double refmaxpause = 60.0;

	public Nomadic(int nodes, double x, double y, double duration, double ignore, long randomSeed, double minspeed, double maxspeed, double nodemaxpause, double refmaxpause, double maxdist, double avgMobileNodesPerGroup, double groupSizeDeviation) {
		super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed, nodemaxpause);
		this.maxdist = maxdist;
		this.avgMobileNodesPerGroup = avgMobileNodesPerGroup;
		this.groupSizeDeviation = groupSizeDeviation;
		this.refmaxpause = refmaxpause;
		generate();
	}

	public Nomadic(String[] args) {
		go(args);
	}

	public void go(String args[]) {
		super.go(args);
		generate();
	}

	public void generate() {
		preGeneration();

		GroupNode[] node = new GroupNode[this.node.length];

		// groups move in a random waypoint manner:
		int nodesRemaining = node.length;
		int offset = 0;
		int size;
		
		while (nodesRemaining > 0) {
			MobileNode ref = new MobileNode();
			double t = 0.0;
			Position src = new Position((x - 2 * maxdist) * randomNextDouble() + maxdist, (y - 2 * maxdist) * randomNextDouble() + maxdist);
			
			if (!ref.add(0.0, src)) {
				System.out.println(getInfo().name + ".generate: error while adding group movement (1)");
				System.exit(0);
			}

			while (t < duration) {
				Position dst = new Position((x - 2 * maxdist) * randomNextDouble() + maxdist, (y - 2 * maxdist) * randomNextDouble() + maxdist);
				double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
				t += src.distance(dst) / speed;
				
				if (!ref.add(t, dst)) {
					System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
					System.exit(0);
				}
				if ((t < duration) && (refmaxpause > 0.0)) {
					double pause = refmaxpause * randomNextDouble();
					if (pause > 0.0) {
						t += pause;
						
						if (!ref.add(t, dst)) {
							System.out.println(getInfo().name + ".main: error while adding node movement (3)");
							System.exit(0);
						}
					}
				}
				src = dst;
			}

			// define group size:
			while ((size = (int)Math.round(randomNextGaussian() * groupSizeDeviation + avgMobileNodesPerGroup)) < 1);

			if (size > nodesRemaining) size = nodesRemaining;
			
			nodesRemaining -= size;
			offset += size;
			
			for (int i = offset - size; i < offset; i++) node[i] = new GroupNode(ref);
		}
		
		// nodes follow their reference points:
		for (int i = 0; i < node.length; i++) {
			double t = 0.0;
			MobileNode group = node[i].group();

			Position src = group.positionAt(t).rndprox(maxdist, randomNextDouble(), randomNextDouble());
			
			if (!node[i].add(0.0, src)) {
				System.out.println(getInfo().name + ".main: error while adding node movement (1)");
				System.exit(0);
			}
			
			double[] gm = group.changeTimes();
			
			while (t < duration) {
				
				int gmi = 0;
				while ((gmi < gm.length) && (gm[gmi] <= t)) gmi++;
				
				/* next absolute time a change happens or the simulation time is over */
				double next = (gmi < gm.length) ? gm[gmi] : duration;

				Position dst = group.positionAt(next).rndprox(maxdist, randomNextDouble(), randomNextDouble());
				double speed = src.distance(dst) / (next - t);

				if (speed > maxspeed) {
					double c_dst = ((maxspeed - minspeed) * randomNextDouble() + minspeed) / speed;
					double c_src = 1 - c_dst;

					dst = new Position(c_src * src.x + c_dst * dst.x, c_src * src.y + c_dst * dst.y);
					
					// ref point isn't pausing, we need to move with it
					if (refmaxpause == 0.0) {
						t = next;
						
						if (!node[i].add(t, dst)) {
							System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
							System.exit(0);
						}
					} else {      //ref pt is pausing, we don't have to time = next
						speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
						t += src.distance(dst) / speed;
						
						if (!node[i].add(t, dst)) {
							System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
							System.exit(0);
						}

						if ((t < duration) && (maxpause > 0.0)) {
							double nodePause = maxpause * randomNextDouble();
							
							if (nodePause > 0.0) {
								// check if our pause time is larger than when our ref pt changes
								if (nodePause + t > next) t = next;
                                else t += nodePause;
								
								if (!node[i].add(t, dst)) {
									System.out.println(getInfo().name + ".main: error while adding node movement (3)");
									System.exit(0);
								}
							}
						}
					}
				} else {
					// there's time to take a pause, push the time ahead: movement time + pause time
					speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
					t += src.distance(dst) / speed;
					
					if (!node[i].add(t, dst)) {
						System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
						System.exit(0);
					}

					if ((t < duration) && (maxpause > 0.0)) {
						double nodePause = maxpause * randomNextDouble();
						
						if (nodePause > 0.0) {
							// check if our pause time is larger than when our ref pt changes
							if (nodePause + t > next) t = next;
							else t += nodePause;
							
							if (!node[i].add(t, dst)) {
								System.out.println(getInfo().name + ".main: error while adding node movement (3)");
								System.exit(0);
							}
						}
					}
				}
				
				src = dst;
			}
		}

		// write the nodes into our base
		this.node = node;

		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		if (key.equals("avgMobileNodesPerGroup")) {
			avgMobileNodesPerGroup = Double.parseDouble(value);
			return true;
		} else if (key.equals("groupSizeDeviation")) {
			groupSizeDeviation = Double.parseDouble(value);
			return true;
		} else if (key.equals("maxdist")) {
			maxdist = Double.parseDouble(value);
			return true;
		} else if (key.equals("refmaxpause")) {
			refmaxpause = Double.parseDouble(value);
			return true;
		} else return super.parseArg(key, value);
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[4];

		p[0] = "avgMobileNodesPerGroup=" + avgMobileNodesPerGroup;
		p[1] = "groupSizeDeviation=" + groupSizeDeviation;
		p[2] = "maxdist=" + maxdist;
		p[3] = "refmaxpause=" + refmaxpause;

		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
		case 'a': // Average nodes per group.
			avgMobileNodesPerGroup = Double.parseDouble(val);
			return true;
		case 'r': // Maximum deviation from group center [m].
			maxdist = Double.parseDouble(val);
			return true;
		case 's': // Standard deviation of nodes per group
			groupSizeDeviation = Double.parseDouble(val);
			return true;
		case 'c': // Reference point max pause
			refmaxpause = Double.parseDouble(val);
			return true;
		default:
			return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		RandomSpeedBase.printHelp();
		System.out.println( getInfo().name + ":" );
		System.out.println("\t-a <average no. of nodes per group>");
		System.out.println("\t-r <max. distance to group center [m]>");
		System.out.println("\t-s <group size standard deviation>");
		System.out.println("\t-c <reference point max. pause>");
	}
}
