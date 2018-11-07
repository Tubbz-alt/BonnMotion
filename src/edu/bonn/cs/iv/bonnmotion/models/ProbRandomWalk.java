package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Application to construct Probabilistic Random Walk mobility scenarios. */
/** 
 *  Chris Walsh
 *  June 2009
 * 
 *  
 */

public class ProbRandomWalk extends Scenario {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("ProbRandomWalk");
        info.description = "Application to construct Probabilistic Random Walk mobility scenarios";
        
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
	
	private double interval = 1;

	public ProbRandomWalk(int nodes, double x, double y, double duration, double ignore, long randomSeed, double interval) {
		super(nodes, x, y, duration, ignore, randomSeed);
		this.interval = interval;
		generate();
	}
	
	public ProbRandomWalk(String[] args) {
		go(args);
	}

	public void go(String[] args) {
		super.go(args);
		generate();
	}

	public ProbRandomWalk(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transition is needed
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;
		go(args);
	}
	
	public void generate() {
		double rand1, rand2, dX, dY, potentialX, potentialY;
		int xstate = 0, ystate = 0;
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
				} 
				catch (ScenarioLinkException e) {
					e.printStackTrace();
				}
			} 
			else src = randomNextPosition();
			
			if (!node[i].add(t, src))		// add source waypoint
				throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (1)");
			
			while (t < duration) {
				Position dst;

				rand1 = randomNextDouble();
				rand2 = randomNextDouble();
				dX = 0;
				dY = 0;
				
				if (xstate == 0) {
					if (rand1 < 0.5) {
						xstate = 2;
						dX += interval;
					} 
					else {
						xstate = 1;
						dX -= interval;
					}
				}
				else if (xstate == 1) {
					if (rand1 < 0.7) dX -= interval;
					else xstate=0;
				} 
				else if (xstate == 2) {
					if (rand1 < 0.7) dX += interval;
					else xstate=0;
				}

				if (ystate == 0) {
					if (rand2 < 0.5) {
						ystate = 2;
						dY += interval;
					} 
					else {
						ystate = 1;
						dY -= interval;
					}
				} 
				else if (ystate == 1) {
					if (rand2 < 0.7) dY -= interval;
					else ystate = 0;
				} 
				else if (ystate == 2) {
					if (rand2 < 0.7) dY += interval;
					else ystate = 0;
				}

				potentialX = src.x + dX;
			    potentialY = src.y + dY;
				
				if (potentialX >= x) potentialX = x;
				else if (potentialX <= 0) potentialX = 0;
				
				if (potentialY >= y) potentialY = y;
				else if (potentialY <= 0) potentialY = 0;
				
				dst = new Position(potentialX, potentialY);
				
				t += interval;
				
				if (!node[i].add(t, dst))
					throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (1)");
		
				src = dst;
			}
		}

		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		if (key.equals("interval")) {
			interval = Double.parseDouble(value);
			return true;
		} else return super.parseArg(key, value);
	}

	public void write( String _name ) throws FileNotFoundException, IOException {
		String[] p = new String[1];
		p[0] = "interval=" + interval;
		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 't': // interval
				interval = Double.parseDouble(val);
				return true;
			default:
				return super.parseArg(key, val);
		}
	}
	
	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		Scenario.printHelp();
		System.out.println( getInfo().name + ":");
		System.out.println("\t-t <time interval to advance by>");
	}
	
	protected void postGeneration() {
		for (int i = 0; i < node.length; i++) {
			Waypoint l = node[i].getLastWaypoint();
			if (l.time > duration) {
				Position p = node[i].positionAt(duration);
				node[i].removeLastElement();
				node[i].add(duration, p);
			}
		}
		super.postGeneration();
	}
}