package edu.bonn.cs.iv.bonnmotion.apps;

import java.io.*;

import edu.bonn.cs.iv.bonnmotion.*;

/** Application that dumps the link durations in a movement scenario to the standard output. */

public class LinkDump extends App {
    private static ModuleInfo info;
    
    static {
        info = new ModuleInfo("LinkDump");
        info.description = "Application that dumps informations about the links";
        
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
    
	protected String name = null;
	protected double radius = 0.0;
	protected double begin = 0.0;
	protected double end = Double.MAX_VALUE;
	protected boolean donly = false;
	protected boolean all = true;
	protected boolean inter_contact = false;
	protected boolean inter_contact_only = false;

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

		Scenario s = Scenario.getScenario(name);
		duration = s.getDuration();
		node = s.getNode();

		if (duration < end)
			end = duration;
		
		PrintWriter fICT = null;
		if (inter_contact_only) {
		    fICT = new PrintWriter(new FileOutputStream(name + ".ict_" + radius));
		}

		PrintWriter fLD = null;
		if (donly) {
		    fLD = new PrintWriter(new FileOutputStream(name + ".ld_" + radius));
		}

		for (int j = 0; j < node.length; j++) {
			for (int k = j+1; k < node.length; k++) {
			    double[] lsc = null;
			    
			    if (s instanceof Scenario3D) {
			        lsc = MobileNode3D.pairStatistics(node[j], node[k], 0.0, duration, radius, false, ((Scenario3D)s).getBuilding());
			    } else {
			        lsc = MobileNode.pairStatistics(node[j], node[k], 0.0, duration, radius, false, s.getBuilding());
			    }
			    
				boolean first = true;
				boolean first_inter_contact = true;
				boolean first_inter_contact_print = true;
				double last_linkDown = 0.0;
				for (int l = 6; l < lsc.length; l += 2) {
					double linkUp = lsc[l];
					double linkDown = (l+1 < lsc.length) ? lsc[l+1] : end;
					if ((all && (linkUp <= end) && (linkDown >= begin)) || ((! all) && (linkUp > begin) && (linkDown < end))) {
						if (inter_contact) {						    
						    if (first_inter_contact) {
					    		last_linkDown = linkDown; first_inter_contact = false;
				    		} else {
								if (linkUp - last_linkDown > 0.0) {
									if (inter_contact_only) {
										fICT.println(linkUp - last_linkDown);
								    } else {
								    	if (first_inter_contact_print & !inter_contact_only) {
								    		System.out.println("");
								    		System.out.print(j + " " + k);
								    		first_inter_contact_print = false;
										}
										System.out.print(" " + (linkUp - last_linkDown));
								    }
								    last_linkDown = linkDown;
								}
						    }
						    if (donly) {
						    	fLD.println(linkDown - linkUp);
						    }
						} else {
							if (all) {
								if (linkUp < begin)
									linkUp = begin;
								if (linkDown > end)
									linkDown = end;
							}
							if (donly) {
								fLD.println(linkDown - linkUp);
							} else {
								if (first) {
									System.out.print(j + " " + k);
									first = false;
								}
								System.out.print(" " + linkUp + "-" + linkDown);
							}
						}
					}
				}
				if (!first)
					System.out.println("");
			}
		}
		if (inter_contact) 
		    System.out.println(""); 
		if (fICT != null)
			fICT.close();
		if (fLD != null)
			fLD.close();
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
			case 'i':
				inter_contact = true;
				return true;
			case 'j':
				inter_contact = true;
				inter_contact_only = true;
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
        System.out.println(getInfo().toDetailString());
		App.printHelp();
		System.out.println("LinkDump:");
		System.out.println("\t-b <begin of time span>");
		System.out.println("\t-d [print link durations only]");
		System.out.println("\t-e <end of time span>");
		System.out.println("\t-f <scenario name>");
		System.out.println("\t-i [print intercontact times]");
		System.out.println("\t-j [print intercontact times only]");
		System.out.println("\t-r <transmission range>");
		System.out.println("\t-w [print only links that go up and down after begin and before end of time span]");
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		new LinkDump(args);
	}
}
