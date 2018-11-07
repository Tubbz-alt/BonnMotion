package edu.bonn.cs.iv.bonnmotion.apps;

import edu.bonn.cs.iv.bonnmotion.*;

import java.io.*;

/** Application that calculates various statistics for movement scenarios. */

public class Visplot extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("Visplot");
        info.description = "Application that visualises node movements";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 269 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Elmar Gerhards-Padilla");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	protected String name = null;
	protected int idx = 0;

	public Visplot(String[] args) throws FileNotFoundException, IOException {
		go( args );
	}

	public void go( String[] args ) throws FileNotFoundException, IOException  {
		parse(args);
		if (name == null) {
			printHelp();
			System.exit(0);
		}

		Scenario s = Scenario.getScenario(name);
		double duration = s.getDuration();
		MobileNode node = s.getNode(idx);

		PrintWriter gp = new PrintWriter(new FileOutputStream(name + ".visplot" + idx));
		double[] ct = node.changeTimes();
		
		if (s instanceof Scenario3D) {
            Position3D p = (Position3D)node.positionAt(0.0);
            gp.println("" + p.x + " " + p.y + " " + p.z);
            for (int i = 0; i < ct.length; i++) {
                p = (Position3D)node.positionAt(ct[i]);
                gp.println("" + p.x + " " + p.y + " " + p.z);
            }
            p = (Position3D)node.positionAt(duration);
            gp.println("" + p.x + " " + p.y + " " + p.z);		    
		} else {
    		Position p = node.positionAt(0.0);
    		gp.println("" + p.x + " " + p.y);
    		for (int i = 0; i < ct.length; i++) {
    			p = node.positionAt(ct[i]);
    			gp.println("" + p.x + " " + p.y);
    		}
    		p = node.positionAt(duration);
    		gp.println("" + p.x + " " + p.y);
    	}
		
		gp.close();
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'f':
				name = val;
				return true;
			case 'i':
				idx = Integer.parseInt(val);
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
        System.out.println(getInfo().toDetailString());
		App.printHelp();
		System.out.println("Visplot:");
		System.out.println("\t-f <scenario name>");
		System.out.println("\t-i <node index>");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new Visplot(args);
	}
}
