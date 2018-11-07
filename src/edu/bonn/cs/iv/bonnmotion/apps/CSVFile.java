package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.*;

/** Application that creates a simple CSV file for processing in 3rd-party tools. */
public class CSVFile extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("CSVFile");
        info.description = "Application that creates a simple CSV file for processing in 3rd-party tools";
        
        info.major = 1;
        info.minor = 0;
        info.revision = ModuleInfo.getSVNRevisionStringValue("$LastChangedRevision: 428 $");
        
        info.contacts.add(ModuleInfo.BM_MAILINGLIST);
        info.authors.add("Matthias Schwamborn");
		info.affiliation = ModuleInfo.UOS_SYS;
    }
    
    public static ModuleInfo getInfo() {
        return info;
    }
    
	private String delimiter = " ";
	private boolean printHeader = false;

	private String name = null;

	public CSVFile(String[] args) {
		go(args);
	}

	public void go(String[] args) {
		parse(args);

		Scenario s = null;
		if (name == null) {
			printHelp();
			System.exit(0);
		}

		try {
			s = Scenario.getScenario(name);
		} catch (Exception e) {
			App.exceptionHandler("Error reading file", e);
		}

		MobileNode[] node = s.getNode();
		PrintWriter movements_csv = openPrintWriter(name + ".csv");
		
		if (printHeader) {
			movements_csv.print("node");
			movements_csv.print(delimiter);
			movements_csv.print("time");
			movements_csv.print(delimiter);
			movements_csv.print("x");
			movements_csv.print(delimiter);
			movements_csv.print("y");
			if(s instanceof Scenario3D) {
				movements_csv.print(delimiter);
				movements_csv.print("z");
			}
			movements_csv.print("\n");
		}
		
		for (int i = 0; i < node.length; i++) {
			int numwp = node[i].getNumWaypoints();
			
			for (int j = 0; j < numwp; j++) {
				Waypoint wp = node[i].getWaypoint(j);
				
				movements_csv.print(i);
				movements_csv.print(delimiter);
				movements_csv.print(wp.time);
				movements_csv.print(delimiter);
				movements_csv.print(wp.pos.x);
				movements_csv.print(delimiter);
				movements_csv.print(wp.pos.y);
				if(s instanceof Scenario3D) {
					movements_csv.print(delimiter);
					movements_csv.print(((Position3D)wp.pos).z);
				}
				movements_csv.print("\n");
			}
		}
		
		movements_csv.close();
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'd':
				delimiter = val;
				return true;
			case 'h':
				printHeader = true;
				return true;
			case 'f':
				name = val;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
        System.out.println(getInfo().toDetailString());
		App.printHelp();
		System.out.println("CSVFile:");
		System.out.println("\t-f <filename>");
		System.out.println("\t[-d <delimiter>]\tSpecify delimiter (default: \" \")");
		System.out.println("\t[-h] \t\t\tPrint header (default: no header)");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new CSVFile(args);
	}
}
