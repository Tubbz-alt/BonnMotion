package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.*;

/** Application that dumps the link durations in a movement scenario to the standard output. */

public class LinkDump extends App {
	protected String name = null;
	protected double radius = 0.0;
	protected double begin = 0.0;
	protected double end = Double.MAX_VALUE;
	protected boolean donly = false;
	protected boolean all = true;

	protected double duration = 0;
	protected MobileNode node[] = null;

	public LinkDump(String[] args) throws FileNotFoundException, IOException {
		go( args );
	}

	public void go( String[] args ) throws FileNotFoundException, IOException {
		parse(args);
		if ((name == null) || (radius == 0.0)) {
			printHelp();
			System.exit(0);
		}

		Scenario s = new Scenario(name);
		// get my args
		duration = s.getDuration();
		node = s.getNode();

		if (duration < end)
			end = duration;

		for (int j = 0; j < node.length; j++) {
			for (int k = j+1; k < node.length; k++) {
				double[] lsc = MobileNode.pairStatistics(node[j], node[k], 0.0, duration, radius, false, s.getBuilding());
				boolean first = true;
				for (int l = 1; l < lsc.length; l += 2) {
					double linkUp = lsc[l];
					double linkDown = (l+1 < lsc.length) ? lsc[l+1] : end;
					if ((all && (linkUp <= end) && (linkDown >= begin)) || ((! all) && (linkUp > begin) && (linkDown < end))) {
						if (all) {
							if (linkUp < begin)
								linkUp = begin;
							if (linkDown > end)
								linkDown = end;
						}
						if (donly) {
							System.out.println(linkDown - linkUp);
						} else {
							if (first) {
								System.out.print(j + " " + k);
								first = false;
							}
							System.out.print(" " + linkUp + "-" + linkDown);
						}
					}
				}
				if (! first)
					System.out.println("");
			}
		}
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'b':
				begin = Double.parseDouble(val);
				return true;
			case 'd':
				donly = true;
				return true;
			case 'e':
				end = Double.parseDouble(val);
				return true;
			case 'f':
				name = val;
				return true;
			case 'r':
				radius = Double.parseDouble(val);
				return true;
			case 'w':
				all = false;
				return true;
			default:
				return super.parseArg(key, val);
		}
	}

	public static  void printHelp() {
		App.printHelp();
		System.out.println("LinkDump:");
		System.out.println("\t-b <begin of time span>");
		System.out.println("\t-d [print link durations only]");
		System.out.println("\t-b <end of time span>");
		System.out.println("\t-f <scenario name>");
		System.out.println("\t-r <transmission range>");
		System.out.println("\t-w [print only links that go up and down after begin and before end of time span]");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new LinkDump(args);
	}
}
