package edu.bonn.cs.iv.bonnmotion.run;

import edu.bonn.cs.iv.bonnmotion.App;
import edu.bonn.cs.iv.bonnmotion.Model;

/** Frontend for all applications and scenario generators. */

public class BM {

	private final static String PROG_NAME = "BonnMotion";
	private final static String PROG_VER = "1.5";
	private final static String MODELS_PACK = "edu.bonn.cs.iv.bonnmotion.models";
	private final static String MODELS[] =
		{
			"ChainScenario", "Links different scenarios",
			"Column", "Column mobility model",
			"DisasterArea", "Extended Catastrophe scenario model",
			"GaussMarkov", "Gauss-Markov model",
			"ManhattanGrid", "Manhattan Grid model",
			"Nomadic","Nomadic community model",
			"OriginalGaussMarkov", "The original Gauss-Markov model",
            "ProbRandomWalk","Probabilistic random walk",
            "Pursue", "Pursue mobility model",
            "RandomDirection","Random direction model",
            "RandomStreet", "Random Street model",
            "RandomWalk","Random walk model",
			"RandomWaypoint", "Random Waypoint model",
			"RPGM", "Reference Point Group Mobility model",
			"Static", "Static network (no movement at all)" 
		};

	private final static String APPS_PACK = "edu.bonn.cs.iv.bonnmotion.apps";
	private final static String APPS[] =
		{
			"Cut", "Extract certain time span from scenario",
			"Dwelltime", "Analyse scenario according to Bettstetter",
			"GlomoFile", "Create scenario files for Glomosim and Qualnet",
			"IntervalFormat","Convert scenario file in interval format",
			"LinkDump", "Dump information about links",
			"NSFile", "Create scenario files for ns-2",
			"SPPXml", "Create motion file according to Horst Hellbruecks XML schema",
			"Statistics", "Analyse scenario",
			"TheONEFile", "Create scenario files for The ONE simulator",
			"Visplot", "Visualise node movements",
			"WiseML", "Converts Bonnmotion format to WiseML"
		};

	private String fSettings = null;
	private String fSaveScenario = null;

	/**
	 * Converts a classname into a Class object
	 * @return class object
	 */
	public static Class<?> str2class(String _class) {
		Class<?> result = null;
		
		try {
			for (int i = 0; i < MODELS.length; i += 2)
				if (MODELS[i].equals(_class)) 
					result = Class.forName(MODELS_PACK + "." + _class);
			for (int i = 0; i < APPS.length; i += 2)
				if (APPS[i].equals(_class))
					result = Class.forName(APPS_PACK + "." + _class);
			
			if (result == null) 
				throw new RuntimeException("Unknown Module " + _class);
		} catch (Exception e) {
			App.exceptionHandler("Error in BM ", e);
		}
		
		return result;
	}

	private void printHeader() {
		System.out.println(PROG_NAME + " " + PROG_VER + "\n");
	}

	private void printModules() {
		System.out.println("Available models: ");
		for (int i = 0; i < MODELS.length; i += 2)
			System.out.println("+ " + MODELS[i] + " - " + MODELS[i + 1]);
	}

	private void printApps() {
		System.out.println("Available apps: ");
		for (int i = 0; i < APPS.length; i += 2)
			System.out.println("+ " + APPS[i] + " - " + APPS[i + 1]);
	}

	private void printSpecificHelp(String _m) {
		Class<?> c = str2class(_m);
		
		try {
			c.getMethod("printHelp", (Class[])null).invoke(null, (Object[])null);
		} catch (Exception e) {
			App.exceptionHandler( "could not print help to "+c, e);
		}
	}

	public static void printHelp() {
		System.out.println("Help:");
		System.out.println("  -h                    	Print this help");
		System.out.println("");
		System.out.println("Scenario generation:");
		System.out.println("  -f <scenario name> [-I <parameter file>] <model name> [model options]");
		System.out.println("  -hm                           Print available models");
		System.out.println("  -hm <module name>             Print help to specific model");
		System.out.println("");
		System.out.println("Application:");
		System.out.println("  <application name> [Application-Options]");
		System.out.println("  -ha                           Print available applications");
		System.out.println("  -ha <application name>        Print help to specific application");
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'I' :
				fSettings = val;
				return true;
			case 'f' :
				fSaveScenario = val;
				return true;
			default :
				return false;
		}
	}

	/**
	 * Starts the magic.
	 * Determines the class to run specified on the command line and passes the parameters
	 * to the application or model.
	 */
	public void go(String[] _args) throws Throwable {
		printHeader();

		if (_args.length == 0) {
			printHelp();
		} else {

			// Get options: Is help needed?
			if (_args[0].equals("-h"))
				printHelp();
			else if (_args[0].equals("-hm"))
				if (_args.length > 1)
					printSpecificHelp(_args[1]);
				else
					printModules();
			else if (_args[0].equals("-ha"))
				if (_args.length > 1)
					printSpecificHelp(_args[1]);
				else
					printApps();
			else {

				int pos = 0;
				while ((_args[pos].charAt(0) == '-') || (pos == _args.length)) {
					char key = _args[pos].charAt(1);
					String value;
					if (_args[pos].length() > 2)
						value = _args[pos].substring(2);
					else
						value = _args[++pos];
					if (!parseArg(key, value)) {
						System.out.println("warning: ignoring unknown key " + key);
					}
					pos++;
				}

				System.out.println("Starting " + _args[pos] + " ...");
				
				Class<?> c = str2class(_args[pos]);
				
				try {
					if (c.getPackage().getName().startsWith(MODELS_PACK)) {
						if (fSaveScenario == null) {
							System.out.println("Refusing to create a scenario which will not be saved anyhow. (Use -f.)");
							System.exit(0);
						}
						String[] args = removeFirstElements(_args, pos);
						args[0] = fSettings;
						Class<?>[] cType = {String[].class};
						Object[] cParam = {args};
						((Model)c.getConstructor(cType).newInstance(cParam)).write(fSaveScenario);
					} else {
						String[] args = removeFirstElements(_args, pos + 1);
						Class<?>[] cType = {String[].class};
						Object[] cParam = {args};
						c.getConstructor(cType).newInstance(cParam);
					}
				} catch (ClassCastException e1) {
					System.out.println("ClassCastException");
				} catch (Exception e) {
						e.printStackTrace();
						App.exceptionHandler("Error in "+_args[pos], e );
				} 

				System.out.println(_args[pos] + " done.");
			}
		}
	}

	public static String[] removeFirstElements(String[] array, int n) {
		String[] r = new String[array.length - n];
		System.arraycopy(array, n, r, 0, r.length);
		return r;
	}

	public static void main(String[] args) throws Throwable {
		new BM().go(args);
	}

} // BM
