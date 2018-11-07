package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.*;

/** Application that creates a movement file for ns-2. */
public class NSFile_Standard extends App {
	/** Add border around the scenario to prevent ns-2 from crashing. */
	public static double border = NSFile.border;

	//String routingprotocol = new String(); //ToDo: Can we remove this?
	//int[] mcastgroups = null; //ToDo: Can we remove this?

	protected String name = null;

	public NSFile_Standard(String[] args) {
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

		MobileNode[] node = s.getNode();
		System.out.println("movement string " + node[0].movementString());

		PrintWriter settings = openPrintWriter(name + ".ns_params");
		settings.println("set val(x) " + (s.getX() + 2 * border));
		settings.println("set val(y) " + (s.getY() + 2 * border));
        if (s instanceof Scenario3D) {
            settings.println("set val(z) " + (((Scenario3D)s).getZ() + 2 * border));
        }
		settings.println("set val(nn) " + node.length);
		settings.println("set val(duration) " + s.getDuration());
		settings.close();

		PrintWriter movements_ns = openPrintWriter(name + ".ns_movements");
		for (int i = 0; i < node.length; i++) {
			String[] m = node[i].movementStringNS("$node_(" + i + ")", border);
			for (int j = 0; j < m.length; j++){
				movements_ns.println(m[j]);
				System.out.println(m[j]);
			}
		}
		movements_ns.close();
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'f':
				name = val;
				return true;
			case 'b':
				border = 0;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
	}
}
