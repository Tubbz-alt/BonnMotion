package edu.bonn.cs.iv.bonnmotion.models;

import java.io.*;
import java.util.ArrayList;

import edu.bonn.cs.iv.bonnmotion.*;

/** Application to construct static scenarios with a drift. */

public class StaticDrift extends Scenario {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("StaticDrift");
        info.description = "Application to construct static scenarios with a drift";
        
        info.major = 1;
        info.minor = 1;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 291 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Raphael Ernst");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }

    private static final double DEFAULT_X = 0;
    private static final double DEFAULT_Y = DEFAULT_X;
	private static final double DEFAULT_INTERVAL_LEN = 0;

	protected double deltaX = DEFAULT_X;
	protected double deltaY = DEFAULT_X;
	protected String input_filename = "";
	protected double interval_len = DEFAULT_INTERVAL_LEN;

	public StaticDrift(int nodes, double x, double y, double duration, double ignore, long randomSeed, double deltaX,
			double deltaY) {
		super(nodes, x, y, duration, ignore, randomSeed);
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		generate();
	}

	public StaticDrift(String[] args) {
		go(args);
	}

	public void go(String[] args) {
		node = new MobileNode[0]; // to hide warning that number of nodes should be defined
		super.go(args);
		generate();
	}

	public void generate() {
		if (input_filename.isEmpty()) {
                    throw new RuntimeException("you have to define a filename (-f)");
                }

		preGeneration();

		ArrayList<Position> inputPositions = getPositionsFromFile(input_filename);
		node = new MobileNode[inputPositions.size()];
		for (int i = 0; i < node.length; i++) {
			node[i] = new MobileNode();
		}
		
		for (int i = 0; i < node.length; i++) {
			Position tmp = inputPositions.get(i);
			
			double time = 0;
			while (time < duration) {
				double newX = (tmp.x - deltaX) + randomNextDouble() * 2 * deltaX;
				double newY = (tmp.y - deltaY) + randomNextDouble() * 2 * deltaY;
				
				if (!(node[i].add(time, new Position(newX, newY)))) {
					throw new RuntimeException(getInfo().name + ".generate: error while adding waypoint");
				}
				
				if(interval_len > 0) {
					time += interval_len;
				}
				else {
					time = duration;
				}
			}
		}
			
		postGeneration();
	}

	/**
	 * reads positions from a file. 
	 * @param filename 
	 * @return list of the read positions
	 */
	protected static ArrayList<Position> getPositionsFromFile(String filename) {
		BufferedReader reader = null;
		ArrayList<Position> result = new ArrayList<Position>();

		try {
			reader = new BufferedReader(new FileReader(filename));

			for (String c; (c = reader.readLine()) != null;) {
				String[] tmp = c.split(" ");
				result.add(new Position(Double.parseDouble(tmp[0]), Double.parseDouble(tmp[1])));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(getInfo().name + ".getPositionsFromFile: error while reading file");
		}
		finally {
			try {
				reader.close();
			}
			catch (Exception e) {
			}
		}
		return result;
	}

	protected boolean parseArg(String key, String val) {
		if (key.equals("deltaX")) {
			deltaX = Double.parseDouble(val);
		}
		else if (key.equals("deltaY")) {
			deltaY = Double.parseDouble(val);
		}
		else if (key.equals("input_filename")) {
			input_filename = val;
		}
		else
			return super.parseArg(key, val);
		return true;
	}

	public void write(String _name) throws FileNotFoundException, IOException {
		String[] p = new String[3];
		p[0] = "deltaX=" + deltaX;
		p[1] = "deltaY=" + deltaY;
		p[2] = "input_filename=" + input_filename;
		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'X':
				deltaX = Double.parseDouble(val);
				return true;
			case 'Y':
				deltaY = Double.parseDouble(val);
				return true;
			case 'B':
				deltaX = Double.parseDouble(val);
				deltaY = Double.parseDouble(val);
				return true;
			case 'T':
				interval_len = Double.parseDouble(val);
				return true;
			case 'f':
				input_filename = val;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		Scenario.printHelp();
		System.out.println(getInfo().name + ":");
		System.out.println("\t-X <delta X>\t(Default: " + DEFAULT_X + ")");
		System.out.println("\t-Y <delta Y>\t(Default: " + DEFAULT_Y + ")");
		System.out.println("\t-B <set delta X and delta Y to the same value>");
		System.out.println("\t-T <N>\tRecalculate the position each N seconds. Set to <= 0 to disable. (Default: " + DEFAULT_INTERVAL_LEN + ")");
		System.out.println("\t-f filename");
                System.out.println();
                System.out.println("Warning: Random behaviour if -X and -Y are combined with -B.");
	}
}
