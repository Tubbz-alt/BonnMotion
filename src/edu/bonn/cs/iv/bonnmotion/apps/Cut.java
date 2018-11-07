package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.App;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;

/** Application to save a certain timeframe from one scenario into a new file. */

public class Cut extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("Cut");
        info.description = "Application to save a certain timeframe from one scenario into a new file";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 269 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("University of Bonn");
		info.affiliation = ModuleInfo.UNIVERSITY_OF_BONN;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	protected double begin = 0.0;
	protected double end = 0.0;
	protected String source = null;
	protected String destination = null;

	public Cut(String[] args) throws FileNotFoundException, IOException {
		go( args );
	}

	public void go( String[] args ) throws FileNotFoundException, IOException {
		parse(args);
		if ((source == null) || (destination == null) || (end == 0.0)) {
			printHelp();
			System.exit(0);
		}
	
		Scenario s = Scenario.getScenario(source); 

		s.cut(begin, end);
		s.write(destination, new String[0]);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'b': // "begin"
				begin = Double.parseDouble(val);
				return true;
			case 'd':
				destination = val;
				return true;
			case 'e': // "end"
				end = Double.parseDouble(val);
				return true;
			case 's': // "source"
				source = val;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
        System.out.println(getInfo().toDetailString());
		App.printHelp();
		System.out.println("Cut:");
		System.out.println("\t-b <beginning of timeframe>");
		System.out.println("\t-d <destination file name>");
		System.out.println("\t-e <end of timeframe>");
		System.out.println("\t-s <source file name>");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		new Cut(args);
	}
}
