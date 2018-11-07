package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.GroupNode;
import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Scenario;

/** Application to create movement scenarios according to the Pursue Mobility model. */

public class Pursue extends Scenario {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("Pursue");
        info.description = "Application to create movement scenarios according to the Pursue Mobility model";
        
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

	protected double maxspeed = 1.5;
	protected double minspeed = 0.5;
	protected double aggressiveness = 0.5;
	protected double pursueRandomnessMagnitude = 0.5;

	public Pursue(int nodes, double x, double y, double duration, double ignore, long randomSeed, 
			double minspeed, double maxspeed, double aggressiveness, double pursueRandomnessMagnitude) {
		super(nodes, x, y, duration, ignore, randomSeed);
		this.minspeed = minspeed;
		this.maxspeed = maxspeed;
		this.aggressiveness = aggressiveness;
		this.pursueRandomnessMagnitude = pursueRandomnessMagnitude; 
		
		if (aggressiveness < 0 || aggressiveness > 1) {
			throw new RuntimeException(getInfo().name+ ".go: Error: aggressiveness must be between 0 and 1");
		}
		
		if (pursueRandomnessMagnitude < 0 || pursueRandomnessMagnitude > 1) {
			throw new RuntimeException(getInfo().name+ ".go: Error: pursueRandomnessMagnitude must be between 0 and 1");
		}
		
		if (minspeed > maxspeed) {
			throw new RuntimeException(getInfo().name+ ".go: Error: minspeed must not be greater than maxspeed");
		}
		
		generate();
	}

	public Pursue(String[] args) {
		go(args);
	}

	public void go(String args[]) {
		super.go(args);
		generate();
	}

	public void generate() {
		preGeneration();

		GroupNode[] node = new GroupNode[this.node.length];
		MobileNode ref = new MobileNode();
		double t = 0.0;
		
		// groups move in a random waypoint manner
		Position src = new Position(x * randomNextDouble(), y * randomNextDouble());
		
		if (!ref.add(0.0, src)) {
			System.out.println(getInfo().name + ".generate: error while adding group movement (1)");
			System.exit(0);
		}
		
		while (t < duration) {
			
			Position dst = new Position(x * randomNextDouble(), y * randomNextDouble());
			double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
			t += src.distance(dst) / speed;
			
			if (!ref.add(t, dst)) {
				System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
				System.exit(0);
			}
			
			src = dst;
		}

		for (int i = 0; i < node.length; i++) {
			// nodes follow their reference points:
			node[i] = new GroupNode(ref);
			MobileNode group = node[i].group();
			t = 0.0;
			src = randomNextPosition();
			
			if (!node[i].add(0.0, src)) {
				System.out.println(getInfo().name + ".generate: error while adding node movement (1)");
				System.exit(0);
			}
			
			double[] gm = group.changeTimes();
			
			while (t < duration) {
				
				int gmi = 0;
				while ((gmi < gm.length) && (gm[gmi] <= t)) gmi++;
				
				double next = (gmi < gm.length) ? gm[gmi] : duration;
			
				double newX = src.x + (aggressiveness * (group.positionAt(t).x - src.x)) + (((2 * randomNextDouble()) - 1) * pursueRandomnessMagnitude);
				double newY = src.y + (aggressiveness * (group.positionAt(t).y - src.y)) + (((2 * randomNextDouble()) - 1) * pursueRandomnessMagnitude);
				
				if (newX > x) newX = x;
				else if(newX < 0) newX = 0;
				
				if (newY > y) newY = y;
				else if(newY < 0) newY = 0;
				
				Position dst = new Position(newX, newY);

				double speed = src.distance(dst) / (next - t);
				
				if (speed > maxspeed) {
					double c_dst = ((maxspeed - minspeed) * randomNextDouble() + minspeed) / speed;
					double c_src = 1 - c_dst;
					dst = new Position(c_src * src.x + c_dst * dst.x, c_src * src.y + c_dst * dst.y);
					t = next;
					
					if (!node[i].add(t, dst)) {
						System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
						System.exit(0);
					}
				}
				else {
					speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
					t += src.distance(dst) / speed;
					
					if (!node[i].add(t, dst)) {
						System.out.println(getInfo().name + ".generate: error while adding group movement (2)");
						System.exit(0);
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
		if (key.equals("maxspeed")) {
			maxspeed = Double.parseDouble(value);
			return true;
		} else if (key.equals("minspeed")) {
			minspeed = Double.parseDouble(value);
			return true;
		} else if (key.equals("aggressiveness")) {
			aggressiveness = Double.parseDouble(value);
			return true;
		} else if (key.equals("pursueRandomnessMagnitude")) {
			pursueRandomnessMagnitude = Double.parseDouble(value);
			return true;
		} else return super.parseArg(key, value);
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[4];

		p[0] = "maxspeed=" + maxspeed;
		p[1] = "minspeed=" + minspeed;
		p[2] = "aggressiveness=" + aggressiveness;
		p[3] = "pursueRandomnessMagnitude=" + pursueRandomnessMagnitude;

		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
		case 'o':
			maxspeed = Double.parseDouble(val);
			return true;
		case 'p': 
			minspeed = Double.parseDouble(val);
			return true;
		case 'k':
			aggressiveness = Double.parseDouble(val);
			return true;
		case 'm':
			pursueRandomnessMagnitude = Double.parseDouble(val);
			return true;
		default:
			return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		Scenario.printHelp();
		System.out.println( getInfo().name + ":" );
		System.out.println("\t-o <maxspeed>");
		System.out.println("\t-p <minspeed>");
		System.out.println("\t-k <aggressiveness (0-1)>");
		System.out.println("\t-m <pursueRandomnessMagnitude (0-1)>");
	}
}
