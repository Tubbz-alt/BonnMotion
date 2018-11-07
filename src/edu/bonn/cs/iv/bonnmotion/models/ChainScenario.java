package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Model;
import edu.bonn.cs.iv.bonnmotion.ModuleInfo;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.run.BM;

public class ChainScenario implements Model {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("ChainScenario");
        info.description = "Application which links different scenarios";
        
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

	protected Vector<Scenario> scenarios = new Vector<Scenario>();
	protected int mode = 0;
	protected boolean writeParts = false;

	public static void printHelp() {
	    System.out.println(getInfo().toDetailString());
		System.out.println(getInfo().name + ":");
		System.out.println("\t-m <mode>");
		System.out.println("\t  0: Fast mode (default)");
		System.out.println("\t  1: Move mode - nodes get 2 sec. to get to their new pos");
		System.out.println("\t-P - if set, writes the link scenarios too");
		System.out.println("\t<model> [model] [...]");
	}

	public static String[] stringArrayCut(String[] _src, int _s, int _c) {
		String[] dst = new String[_src.length - _c];
		System.arraycopy(_src, 0, dst, 0, _s);
		System.arraycopy(_src, _s + _c, dst, _s, _src.length - (_s + _c));
		return dst;
	}

	public ChainScenario(String[] args) {
		go(parseArgs(args));
	}

	public String[] parseArgs(String[] _args) {
		for (int i = 0; i < _args.length; i++) {
			if (_args[i] != null) {
				char key = _args[i].charAt(1);
				switch (key) {
					case 'm' :
						mode = Integer.parseInt(_args[i + 1]);
						_args = stringArrayCut(_args, i, 2);
						i=0;
						System.out.println("Mode: " + mode);
						break;
					case 'P' :
						writeParts = true;
						_args = stringArrayCut(_args, i, 1);
						System.out.println("Will write parts.");
						i=0;
						break;
					default :
						break;
				}
			}
		}
		return _args;
	}

	private void go(String[] args) {
		args = BM.removeFirstElements(args, 1);
		Scenario s = null;
		Scenario s_old = null;
		for (int i = 0; i < args.length; i++) {
			String[] sArgs = args[i].split(" ");
			System.out.println("  Starting Model(" + i + "): " + sArgs[0]);
			Class<?> c = BM.str2class(sArgs[0]);

			sArgs[0] = null;
			Class<?>[] cType = { String[].class, Scenario.class, Integer.class };
			Object[] cParam = { sArgs, s_old, new Integer(mode)};
			try {
				s = (Scenario) c.getConstructor(cType).newInstance(cParam);
				scenarios.add(s);
				s_old = s;
			} catch (Exception e) {
				System.out.println("Error: " + e);
				e.printStackTrace();
				System.exit(-1);
			}
			System.out.println("  done.");
		}
	}

	public void write(String _basename) throws FileNotFoundException, IOException {
		PrintWriter info = new PrintWriter(new FileOutputStream(_basename + ".params"));
		info.println("model=ChainScenario");

		for (int i = 0; i < scenarios.size(); i++) {
			Model m = (Model) scenarios.get(i);
			String modelBasename = _basename + "-" + m.getModelName() + "-" + i;
			info.println("linkModelParams=" + modelBasename);
			if (writeParts)
				m.write(modelBasename);
		}

		String modelAllBasename = _basename + "-ALL";
		info.println("modeAll=" + modelAllBasename);
		makeMixedScenario().write(modelAllBasename);

		info.close();
	}

	public String getModelName() {
		return getInfo().name;
	}

	public Scenario makeMixedScenario() {
		double x=0, y=0; // final scenario size
		Scenario s = (Scenario) scenarios.get(0);
		MobileNode[] mixedSmn = new MobileNode[s.getNode().length];
		double duration = 0;

		for (int i = 0; i < s.getNode().length; i++)
			mixedSmn[i] = new MobileNode();

		for (int i = 0; i < scenarios.size(); i++) {
			s = (Scenario) scenarios.get(i);
			x = Math.max(x, s.getX() );
			y = Math.max(y, s.getY() );
			duration += s.getDuration();
			MobileNode[] mn = s.getNode();
			for (int j = 0; j < mn.length; j++) {
				mixedSmn[j].addWaypointsOfOtherNode(mn[j]);
			}
		}

		Scenario mixedS = new Scenario(s.getNode().length, x, y, duration, s.getIgnore(), s.getRandomSeed());
		mixedS.setModelName(getInfo().name);
		mixedS.setNode(mixedSmn);
		return mixedS;
	}
}
