package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.*;
import edu.bonn.cs.iv.bonnmotion.models.DisasterArea;

/** Application that creates a movement file for ns-2. */
public class NSFile extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("NSFile");
        info.description = "Application that creates movement files for ns-2";
        
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
    
	/** Add border around the scenario to prevent ns-2 from crashing. */
	public final static double border = 10.0;
	private static boolean useDefaultNSConverter = false;

	//String routingprotocol = new String(); //ToDo: Can we remove this?
	//int[] mcastgroups = null; //ToDo: Can we remove this?

	protected String name = null;

	public NSFile(String[] args) {
		go( args );
	}

	public void go( String[] args ) {
		parse(args);

		Scenario s = null;
		if ( name == null ) {
			printHelp();
			System.exit(0);
		}

		try {
			s = Scenario.getScenario(name);
		} catch (Exception e) {
			App.exceptionHandler( "Error reading file", e);
		}

		if(useDefaultNSConverter) {
			new NSFile_Standard(args);
		}
		else if(s.getModelName().equals(DisasterArea.getInfo().name)) {
			new NSFile_DisasterArea(args);
		}
		else {
			new NSFile_Standard(args);
		}
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'd':
				useDefaultNSConverter = true;
				return true;
			case 'f':
				name = val;
				return true;
			case 'b':
				/* Ignore, this is read by the called NSFile_<class> */
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
        System.out.println(getInfo().toDetailString());
		App.printHelp();
		System.out.println("NSFile:");
		System.out.println("\t[-d]\tOverride module specific convertes and use the standard converter");
		System.out.println("\t[-b]\tDisable additional margin");
		System.out.println("\t-f <filename>");
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new NSFile(args);
	}
}
