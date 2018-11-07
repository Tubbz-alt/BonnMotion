package edu.bonn.cs.iv.bonnmotion.models;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.RandomSpeedBase;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Application to construct boundless mobility scenarios. */

public class Boundless extends RandomSpeedBase {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("Boundless");
        info.description = "Application to construct Boundless mobility scenarios";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 428 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Chris Walsh");

		info.affiliation = ModuleInfo.TOILERS;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }

	protected double deltaT = 0.1;
	protected double accelMax = 1.5;
	protected double alpha = Math.PI/2;
	HashMap<Integer, LinkedList<Double>> statuschanges = new HashMap<Integer, LinkedList<Double>>();

	public Boundless(int nodes, double x, double y, double duration, double ignore, long randomSeed, 
			double minspeed, double maxspeed, double maxpause, double deltaT, double accelMax, double alpha) {
		super(nodes, x, y, duration, ignore, randomSeed, minspeed, maxspeed, maxpause);
		this.deltaT = deltaT;
		this.accelMax = accelMax;
		this.alpha = alpha;
		generate();
	}
	
	public Boundless(String[] args) {
		go(args);
	}

	public void go(String[] args) {
		super.go(args);
		generate();
	}

	public Boundless(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transition is needed
		predecessorScenario = _pre;
		transitionMode = _transitionMode.intValue();
		isTransition = true;
		go(args);
	}
	
	public void generate() {
		preGeneration();

		for (int i = 0; i < node.length; i++) {
			LinkedList<Double> statuschangetimes = new LinkedList<Double>();
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
			
			//give our nodes a random starting direction and speed
			double theta = randomNextDouble() * 2 * Math.PI;
			double speed = (maxspeed - minspeed) * randomNextDouble() + minspeed;
			int status = 0;
		
			while (t < duration) {
				Position dst;
				
				if (!node[i].add(t, src))
					throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (1)");
				
				//set our node status back to 'no status change'
				status = 0;
				double deltaSpeed = (randomNextDouble()*2*(accelMax*deltaT)) - (accelMax*deltaT); 
				double deltaTheta = (randomNextDouble()*2*(alpha*deltaT)) - (alpha*deltaT);	
				
				theta = theta + deltaTheta;
				speed = Math.min(Math.max(speed + deltaSpeed, 0), maxspeed);
				
				double newX = src.x + speed*Math.cos(theta);
				double newY = src.y + speed*Math.sin(theta);
				
				//Check to see if the node hits the boarders at all
				// if so set status to 'OFF / leaves scenario'
				if (newX > x) {
					newX = 0;
					status = 2;
					statuschangetimes.add(t);
				} else if (newX < 0) {
					newX = x;
					status = 2;
					statuschangetimes.add(t);
				}
				
				if (newY > y) {
					newY = 0;
					status = 2;
					statuschangetimes.add(t);
				} else if (newY < 0) {
					newY = y;
					status = 2;
					statuschangetimes.add(t);
				}
				
				dst = new Position(newX, newY, status);
				
				t += deltaT;
				if (!node[i].add(t, dst))
					throw new RuntimeException(getInfo().name + ".go: error while adding waypoint (2)");
				
				//Check to see if the node's status is 2
				// if so set status to 'ON / arrives in scenario'
				if(status == 2){
					statuschanges.put(i, statuschangetimes);
					status = 1;
				}
				
				src = new Position(dst.x, dst.y, status);
			}
		}

		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		if (key.equals("deltaT")) {
			deltaT = Double.parseDouble(value);
			return true;
		}
		else if (key.equals("accelMax")) {
			accelMax = Double.parseDouble(value);
			return true;
		}
		else if (key.equals("alpha")) {
			alpha = Double.parseDouble(value);
			return true;
		} 
		else return super.parseArg(key, value);
	}

	public void write(String _name) throws FileNotFoundException, IOException {
		String[] p = new String[3];
		p[0] = "deltaT=" + deltaT;
		p[1] = "accelMax=" + accelMax;
		p[2] = "alpha=" + alpha;
		
		// not sure this is working according to the specifications of the .changes file
		// this is directly copy pasted from DistasterArea.java
		// TODO: check this code such that it conforms to the .changes file specifications

		PrintWriter changewriter = new PrintWriter(new BufferedWriter(new FileWriter(_name + ".changes")));
		for (Integer i : statuschanges.keySet())
		{
			changewriter.write(i.toString());
			changewriter.write(" ");
			LinkedList<Double> list = statuschanges.get(i);
			changewriter.write(list.toString());
		}
		changewriter.close();
		
		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 't': // "delta time"
				deltaT = Double.parseDouble(val);
				return true;
			case 'm': // "max acceleration"
				accelMax = Double.parseDouble(val);
				return true;
			case 's': // "max angular change"
				alpha = Double.parseDouble(val);
				return true;
			default:
				return super.parseArg(key, val);
		}
	}
	
	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		RandomSpeedBase.printHelp();
		System.out.println( getInfo().name + ":");
		System.out.println("\t-t <time step>");
		System.out.println("\t-m <max acceleration change>");
		System.out.println("\t-s <alpha: max angular change>");
	}
	
	/* (non-Javadoc)
	 * @see edu.bonn.cs.iv.bonnmotion.Scenario#postGeneration()
	 */
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