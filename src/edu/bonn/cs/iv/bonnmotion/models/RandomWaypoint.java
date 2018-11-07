package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Application to construct Randomwaypoint mobility scenarios. */

public class RandomWaypoint extends RandomSpeedBase {

	private static final String MODEL_NAME = "RandomWaypoint";

	/** Restrict the mobiles' movements: 1 . */
	protected int dim = 3;

	public RandomWaypoint(int nodes, double x, double y, double duration, double ignore, long randomSeed, double minspeed, double maxspeed, double maxpause, int dim) {
		super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed, maxpause);
		this.dim = dim;
		generate();
	}
	
	public RandomWaypoint( String[] args ) {
		go( args );
	}

	public void go( String[] args ) {
		super.go(args);
		generate();
	}

	public RandomWaypoint(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transtion is needed
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;
		go(args);
	}
	
	public void generate() {
		preGeneration();

		for (int i = 0; i < node.length; i++) {
			node[i] = new MobileNode();
			double t = 0.0;
			Position src = null;
			if (isTransition) {
				try {
					Waypoint lastW = transition(predecessorScenario, transitionMode, i);
					src = lastW.pos;
					t = lastW.time;
				} catch (ScenarioLinkException e) {
					e.printStackTrace();
				}
			} else {
				src = randomNextPosition();
			}
			while (t < duration) {
				Position dst;
				if (!node[i].add(t, src))
					throw new RuntimeException(MODEL_NAME + ".go: error while adding waypoint (1)");
				switch (dim) {
					case 1 :
						dst = randomNextPosition(-1., src.y);
						break;
					case 2 :
						switch ((int) (randomNextDouble() * 2.0)) {
							case 0 :
								dst = randomNextPosition(-1., src.y);
								break;
							case 1 :
								dst = randomNextPosition(src.x, -1.);
								break;
							default :
								throw new RuntimeException(
									MODEL_NAME + ".go: This is impossible - how can (int)(randomNextDouble() * 2.0) be something other than 0 or 1?!");
						}
						break;
					case 3 :
						dst = randomNextPosition();
						break;
					default :
						throw new RuntimeException(MODEL_NAME + ".go: dimension may only be of value 1, 2 or 3.");
				}
				double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
				t += src.distance(dst) / speed;
				if (!node[i].add(t, dst))
					throw new RuntimeException(MODEL_NAME + ".go: error while adding waypoint (2)");
				if ((t < duration) && (maxpause > 0.0)) {
					double pause = maxpause * randomNextDouble();
					t += pause;
				}
				src = dst;
			}
		}

		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		if (key.equals("dim")) {
			dim = Integer.parseInt(value);
			return true;
		} else return super.parseArg(key, value);
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[1];
		p[0] = "dim=" + dim;
		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'o': // "dimensiOn"
				dim = Integer.parseInt(val);
				if ((dim < 1) || (dim > 3)) {
					System.out.println("dimension must be between 1 and 3");
					System.exit(0);
				}
				if ((aFieldParams != null) && (dim != 3))
					System.out.println("warning: attractor field not used if dim != 3");
				return true;
			default:
				return super.parseArg(key, val);
		}
	}
	
	public static void printHelp() {
		RandomSpeedBase.printHelp();
		System.out.println( MODEL_NAME + ":");
		System.out.println("\t-o <dimension: 1: x only, 2: x or y, 3: x and y>");
	}
	

	/* (non-Javadoc)
	 * @see edu.bonn.cs.iv.bonnmotion.Scenario#postGeneration()
	 */
	protected void postGeneration() {
		for ( int i = 0; i < node.length; i++ ) {
			/* AAAAAAARG! nodes walk beyond the border
			
			MobileNode n = node[i];
			
			Waypoint w2 = node[i].getWaypoint(node[i].numWaypoints() - 1);
			Waypoint w1 = node[i].getWaypoint(node[i].numWaypoints() - 2);

			// Gradlinige Bewegung, konstante Geschw.,  v-> = v_x-> + v_y->
			double t_d = w2.time - w1.time;
			double v_x = (w2.pos.x-w1.pos.x) / t_d;
			double v_y = (w2.pos.y-w1.pos.y) / t_d;
			double p_x = (duration-w1.time) * v_x + w1.pos.x;
			double p_y = (duration-w1.time) * v_y + w1.pos.y;
			Position p = new Position( p_x, p_y );

			node[i].removeLastElement();
			node[i].add(duration, p); */
			
			Waypoint l = node[i].lastElement();
			if (l.time > duration) {
				Position p = node[i].positionAt(duration);
				node[i].removeLastElement();
				node[i].add(duration, p);
			}
		}
		super.postGeneration();
	}

}
