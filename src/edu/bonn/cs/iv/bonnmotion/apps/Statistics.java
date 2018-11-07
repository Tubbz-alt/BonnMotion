/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2005 University of Bonn                                **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.bonnmotion.apps;

import edu.bonn.cs.iv.bonnmotion.*;
import edu.bonn.cs.iv.graph.*;
import edu.bonn.cs.iv.util.*;

import java.io.*;
import java.util.Vector;

/** Application that calculates various statistics for movement scenarios. */

public class Statistics extends App {
	public static final int STATS_NODEDEG = 0x00000001;
	public static final int STATS_PARTITIONS = 0x00000002;
	public static final int STATS_MINCUT = 0x00000004;
	public static final int STATS_STABILITY = 0x00000008;
	public static final int STATS_UNIDIRECTIONAL = 0x00000010;
	public static final int STATS_PARTDEG = 0x00000020; // to what degree is the network partitioned?

	protected static double secP = 0;
	protected static double secM = 0;
	protected static double secN = 0;
	protected static double secS = 0;
	protected static double secU = 0;
	protected static double secG = 0;

	protected static boolean printTime = false;

	protected String name = null;
	protected double[] radius = null;
	protected int flags = 0;

	/*	protected double duration = 0;
		protected MobileNode node[] = null; */

	public Statistics(String[] args) throws FileNotFoundException, IOException {
		go(args);
	}

	public void go(String[] _args) throws FileNotFoundException, IOException {
		parse(_args);
		if ((name == null) || (radius == null)) {
			printHelp();
			System.exit(0);
		}

		Scenario s = new Scenario(name);
		// get my args
		/*		node = s.getNode();
				duration = s.getDuration(); */

		if (flags > 0)
			for (int i = 0; i < radius.length; i++) {
				Heap sched = new Heap();
				System.out.println("radius=" + radius[i]);
	//					System.out.println("scheduling...");
				schedule(s, sched, radius[i], false);
	//					System.out.println("calculating...");
				String basename = name + ".stats_" + radius[i];
				if (basename.endsWith(".0"))
					basename = basename.substring(0, basename.length() - 2);
				progressive(s.nodeCount(), s.getDuration(), sched, true, flags, basename);
			} else
				overall(s, radius, name);
	}

	/** Calculates statistics' devolution over time. */
	public static void progressive(
		int nodes, double duration,
//		Scenario s,
		Heap sched,
		boolean bidirectional,
		int which,
		String basename)
		throws FileNotFoundException, IOException {

//		MobileNode[] node = s.getNode();
//		double duration = s.getDuration();

		Graph topo = new Graph();
		for (int i = 0; i < nodes; i++)
			topo.checkNode(i);

		double time = 0.0;

		int unicnt = -1;
		int unisrc = -1;
		int unidst = -1;
		int mincut = -1;
		int stability = -1;
		int edges = -1;
		int part = -1;
		double partdeg = -1;

		double tNextDeg = 0.0;
		double tNextMinCut = 0.0;
		double tNextPart = 0.0;
		double tNextStability = 0.0;
		double tNextPartDeg = 0.0;
		double tNextUni = 0.0;
		
		// target files for stats output
		PrintWriter fDeg = null;
		if ((which & STATS_NODEDEG) > 0)
			fDeg = new PrintWriter(new FileOutputStream(basename + ".nodedeg"));

		PrintWriter fUni = null;
		if ((which & STATS_UNIDIRECTIONAL) > 0)
			fUni = new PrintWriter(new FileOutputStream(basename + ".uni"));

		PrintWriter fPart = null;
		if ((which & STATS_PARTITIONS) > 0)
			fPart = new PrintWriter(new FileOutputStream(basename + ".part"));

		PrintWriter fMinCut = null;
		if ((which & STATS_MINCUT) > 0)
			fMinCut = new PrintWriter(new FileOutputStream(basename + ".mincut"));

		PrintWriter fStability = null;
		if ((which & STATS_STABILITY) > 0)
			fStability = new PrintWriter(new FileOutputStream(basename + ".stability"));

		PrintWriter fPartDeg = null;
		if ((which & STATS_PARTDEG) > 0)
			fPartDeg = new PrintWriter(new FileOutputStream(basename + ".partdeg"));

//		double n1 = (double) (nodes * (nodes - 1));
		double n1 = (double)(nodes - 1);
		int[] uni = new int[4];
		int progress = -1;
		int done = 0;
		while (sched.size() > 0) {
			double ntime = sched.minLevel();
			int nProg = (int)(100.0 * (double)done / (double)(sched.size() + done) + 0.5);
			if (nProg > progress) {
				progress = nProg;
				System.out.print("calculating... " + progress + "% done.\r");
			}
			done++;
			if (ntime > time) {
				if (printTime)
					System.out.println("t=" + time);
				Graph g;
				if (bidirectional) {
					g = topo;
					if (((which & STATS_NODEDEG) > 0) && (time >= tNextDeg)) {
						tNextDeg += secN;

						int ne = 0;
						for (int i = 0; i < g.nodeCount(); i++) {
							Node n = g.nodeAt(i);
							ne += n.outDeg();
						}
						if (ne != edges) {
							edges = ne;
							fDeg.println(time + " " + ((double)edges / (double)g.nodeCount()));
						}
					}
				} else {
					g = (Graph) topo.clone();
					//					double[] hirbel = g.unidirRemove(uni);
					g.unidirRemove(uni);
					if (((which & STATS_UNIDIRECTIONAL) > 0) && (time >= tNextUni) ) {
						tNextUni += secU;

						if (uni[0] != unicnt) {
							unicnt = uni[0];
							fUni.println("unicnt " + time + " " + unicnt);
						}
						if (uni[1] != unisrc) {
							unisrc = uni[1];
							fUni.println("unisrc " + time + " " + unisrc);
						}
						if (uni[2] != unidst) {
							unidst = uni[2];
							fUni.println("unidst " + time + " " + unidst);
						}
					}
					if (((which & STATS_NODEDEG) > 0) && (time >= tNextDeg) && (uni[3] != edges)) {
						tNextDeg += secN;

						edges = uni[3];
						fDeg.println(time + " " + ((double) edges / n1));
					}
				}
				if (((which & STATS_PARTITIONS) > 0) && (time >= tNextPart)) {
					tNextPart += secP;
					
					int npart = g.partitions(0);
					if (part != npart) {
						part = npart;
						fPart.println(time + " " + part);
					}
				}
				if (((which & STATS_PARTDEG) > 0) && (time >= tNextPartDeg)) {
					tNextPartDeg += secG;
					
					double npartdeg = g.partdeg(0);
					if (partdeg != npartdeg) {
						partdeg = npartdeg;
						fPartDeg.println(time + " " + partdeg);
					}
				}
				if (((which & STATS_MINCUT) > 0) && (time >= tNextMinCut)) {
					tNextMinCut += secM;
					
					Graph h = Graph.buildSeperatorTree(g);
					Edge minedge = h.findMinEdge();
					int nmincut = 0;
					if (minedge != null)
						nmincut = minedge.weight;
					if (mincut != nmincut) {
						mincut = nmincut;
						fMinCut.println(time + " " + mincut);
					}
				}
				if (((which & STATS_STABILITY) > 0) && (time >= tNextStability )) {
					tNextStability += secS;
					
					int nstability = g.stability();
					if (stability != nstability) {
						stability = nstability;
						fStability.println(time + " " + stability);
					}
				}
			}
			time = ntime;
			IndexPair idx = (IndexPair) sched.deleteMin();
			if (idx.i >= 0) { // hack: stopper
				Node src = topo.getNode(idx.i);
				if (src.getSucc(idx.j) == null) {
					//				System.out.println("" + time + " +(" + idx.i + ", " + idx.j + ")");
					Node dst = topo.getNode(idx.j);
					src.addSucc(dst, 1); //.setLabel("time", new Double(time));
					if (bidirectional)
						dst.addSucc(src, 1); //.setLabel("time", new Double(time));
				} else {
					//				System.out.println("" + time + " -(" + idx.i + ", " + idx.j + ")");
					src.delSucc(idx.j);
					if (bidirectional)
						topo.getNode(idx.j).delSucc(idx.i);
				}
			}
		}
		System.out.println();

		if (fDeg != null)
			fDeg.close();
		if (fUni != null)
			fUni.close();
		if (fPart != null)
			fPart.close();
		if (fMinCut != null)
			fMinCut.close();
		if (fStability != null)
			fStability.close();
		if (fPartDeg != null)
			fPartDeg.close();
	}

	/** Put LinkStatusChange-events into a heap. */
	public static double schedule(
		Scenario s,
		Heap sched,
		double radius,
		boolean calculateMobility) {
		MobileNode[] node = s.getNode();
		double duration = s.getDuration();
		double mobility = 0.0;
		
		int total = (node.length - 1) * node.length / 2;
		int done = 0;
		
		int progress = -1;
		for (int i = 0; i < node.length; i++) {
			for (int j = i + 1; j < node.length; j++) {
				int nProg = (int)(100.0 * (double)done / (double)total + 0.5);
				if (nProg > progress) {
					progress = nProg;
					System.err.print("scheduling... " + progress + "% done.\r");
				}
				done++;
				IndexPair idx = new IndexPair(i, j);
				double[] linkStatusChanges =
					MobileNode.pairStatistics(
						node[i],
						node[j],
						0.0,
						duration,
						radius,
						calculateMobility);
				if (calculateMobility)
					mobility += linkStatusChanges[0];
				for (int l = 1; l < linkStatusChanges.length; l++)
					sched.add(idx, linkStatusChanges[l]);
				if ((linkStatusChanges.length & 1) == 0)
					// explicitely add "disconnect" at the end
					sched.add(idx, duration);
			}
		}
		System.err.println();
		return mobility;
	}

	/** Helper function for overall(), merge two partitions. */
	protected static void pmerge(int[] idx, int[] size, int i, int j) {
		int old = idx[j];
		for (int k = 0; k < idx.length; k++)
			if (idx[k] == old)
				idx[k] = idx[i];
		size[idx[i]] += size[old];
		size[old] = 0;
	}

	/** Calculate statistics averaged over the whole simulation time. */
	public static void overall(Scenario s, double[] radius, String basename)
		throws FileNotFoundException {
		MobileNode[] node = s.getNode();
		double duration = s.getDuration();

		double[][] ls = null;
		PrintWriter stats = new PrintWriter(new FileOutputStream(basename + ".stats"));
		int tEdges = (node.length * (node.length - 1)) / 2;
		double normFact = (double) tEdges * duration;
		Heap heap = null;
		int[] pIdx = null; // partition index
		int[] pSize = null; // partition sizes
		boolean[] isolation = null;

		ls = new double[node.length - 1][];
		for (int i = 0; i < ls.length; i++)
			ls[i] = new double[node.length - i - 1];
		heap = new Heap();
		pIdx = new int[node.length]; // partition index
		pSize = new int[node.length]; // partition sizes
		isolation = new boolean[node.length];
		for (int k = 0; k < radius.length; k++) {
			System.out.println("transmission range=" + radius[k]);

			int partitions = node.length;
			int partitionsOld = node.length;
			double pSince = 0.0;
			double avgPart = 0.0;

			double partDeg = 1.0;
			double partDegOld = 1.0;
			double pdSince = 0.0;
			double avgPartDeg = 0.0;

			int linkbreaks = 0;
			double timeToLinkBreak = 0.0;
			int links = 0;
			double linkDuration = 0.0;
			double mobility = 0.0;
			int connections = 0;
			Vector linkDurations = new Vector();

//			System.out.println("scheduling...");

			for (int i = 0; i < node.length; i++) {
				pIdx[i] = i;
				pSize[i] = 1;
				isolation[i] = true;
				for (int j = i + 1; j < node.length; j++)
					ls[i][j - i - 1] = -1.0;
			}

			mobility = schedule(s, heap, radius[k], (k == 0));

			if (k == 0) {
				stats.println("# mobility=" + (mobility / normFact));
				stats.println(
					"# \"tx range\" \"avg. degree\" \"partitions\" \"partitioning degree\" \"avg time to link break\" \"std deviation of time to link break\" \"link breaks\" \"avg link duration\" \"total links\"");
			}
//			System.out.println("calculating...");
			double tOld = 0.0;
			int progress = -1;
			int done = 0;
			while (heap.size() > 0) {
				int nProg = (int)(100.0 * (double)done / (double)(heap.size() + done) + 0.5);
				if (nProg > progress) {
					progress = nProg;
					System.err.print("calculating... " + progress + "% done.\r");
				}
				done++;
				double tNew = heap.minLevel();
//				System.out.println("tNew=" + tNew + " tOld=" + tOld);
				IndexPair idx = (IndexPair) heap.deleteMin();

				// uncomment the following code piece to print messages about nodes becoming isolated (meaning they have no links at all) and leaving their isolation again:

				/*				if (tNew > tOld)
									for (int i = 0; i < pSize.length; i++) {
										if ((pSize[i] == 1) && ((! isolation[i]) || (tOld == 0.0))) {
											isolation[i] = true;
											System.out.println("isolation " + i + " at " + tOld);
											// check if this is working correctly
											for (int j = 0; j < node.length; j++)
												if (i < j) {
													if (ls[i][j - i - 1] >= 0.0)
														System.out.println("! " + j);
												} else if (i > j) {
													if (ls[j][i - j - 1] >= 0.0)
														System.out.println("! " + j);
												}
										} else if (isolation[i] && (pSize[i] != 1)) {
											isolation[i] = false;
											if (tOld > 0.0)
												System.out.println("de-isolation " + i + " at " + tOld);
										}
									} */

				if (((tNew > tOld) && (partitions != partitionsOld)) || (heap.size() == 0)) {
					if (heap.size() != 0) {
						avgPart += (double)partitionsOld * (tOld - pSince);
//						System.out.println("#1: avgPart += " + partitionsOld + " * (" + tOld + " - " + pSince + ")");
					} else {
						avgPart += (double)partitions * (tNew - pSince);
//						System.out.println("#2: avgPart += " + partitions + " * (" + tNew + " - " + pSince + ")");
					}
					partitionsOld = partitions;
					pSince = tOld;
				}
				if (((tNew > tOld) && (partDeg != partDegOld)) || (heap.size() == 0)) {
					if (heap.size() != 0) {
						avgPartDeg += partDegOld * (tOld - pdSince);
//						System.out.println("#1: avgPartDeg += " + partDegOld + " * (" + tOld + " - " + pdSince + ")");
					} else {
						avgPartDeg += partDeg * (tNew - pdSince);
//						System.out.println("#2: avgPartDeg += " + partDeg + " * (" + tNew + " - " + pdSince + ")");
					}
					partDegOld = partDeg;
					pdSince = tOld;
				}
				if (ls[idx.i][idx.j - idx.i - 1] < 0.0) { // connect
					connections++;
					ls[idx.i][idx.j - idx.i - 1] = tNew;
					if (pIdx[idx.i] != pIdx[idx.j]) {
						partitions--;
						pmerge(pIdx, pSize, idx.i, idx.j);
					}
				} else { // disconnect
					connections--;
					double tUp = ls[idx.i][idx.j - idx.i - 1];
					double tConn = tNew - tUp;
					ls[idx.i][idx.j - idx.i - 1] = -1.0;
					linkDuration += tConn;
					links++;
					if ((tNew < duration) && (tUp > 0.0)) {
						linkDurations.addElement(new Double(tConn));
						timeToLinkBreak += tConn;
						linkbreaks++;
						// rebuild pIdx
						if (partitions == 1) {
							partitions = node.length;
							for (int i = 0; i < node.length; i++) {
								pIdx[i] = i;
								pSize[i] = 1;
							}
						} else {
							int split = pIdx[idx.i];
							for (int i = 0; i < node.length; i++)
								if (pIdx[i] == split) {
									partitions++;
									pIdx[i] = i;
									pSize[i] = 1;
								}
							partitions--;
						}
						for (int i = 0; i < ls.length; i++)
							for (int j = i + 1; j < node.length; j++)
								if ((pIdx[i] != pIdx[j]) && (ls[i][j - i - 1] >= 0.0)) {
									partitions--;
									pmerge(pIdx, pSize, i, j);
								}
					}
				}
				partDeg = 0.0;
				for (int i = 0; i < pSize.length; i++)
					if (pSize[i] > 0)
						partDeg += (double)(pSize[i] * (node.length - pSize[i]));
				tOld = tNew;
			}
			System.err.println();
			double expDuration = timeToLinkBreak / (double) linkbreaks;
			double varDuration = 0.0;
			for (int i = 0; i < linkbreaks; i++) {
				double tmp = ((Double) linkDurations.elementAt(i)).doubleValue() - expDuration;
				varDuration += tmp * tmp;
			}
			varDuration = Math.sqrt(varDuration / (double) (linkbreaks - 1));
			stats.println(
				radius[k]
					+ " "
					+ (2. * linkDuration / (duration * (double)node.length))
					+ " "
					+ (avgPart / duration)
					+ " "
					+ (avgPartDeg / (duration * (double)((node.length - 1) * node.length)))
					+ " "
					+ expDuration
					+ " "
					+ varDuration
					+ " "
					+ linkbreaks
					+ " "
					+ (linkDuration / (double) links)
					+ " "
					+ links);
		}
		stats.close();
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'f' :
				name = val;
				return true;
			case 'r' : // radius
				radius = App.parseDoubleArray(val);
				return true;
			case 't' :
				printTime = true;
				return true;
			case 'G' :
				flags = flags ^ STATS_PARTDEG;
				if (val.length()!=0)
					secG = Double.parseDouble(val);
				return true;
			case 'M' : // MinCut
				flags = flags ^ STATS_MINCUT;
				if (val.length()!=0)
					secM = Double.parseDouble(val);
				return true;
			case 'N' :
				flags = flags ^ STATS_NODEDEG;
				if (val.length()!=0)
					secN = Double.parseDouble(val);
				return true;
			case 'P' : // Partitions
				flags = flags ^ STATS_PARTITIONS;
				if (val.length()!=0)
					secP = Double.parseDouble(val);
				return true;
			case 'S' : // Stability
				flags = flags ^ STATS_STABILITY;
				if (val.length()!=0)
					secS = Double.parseDouble(val);
				return true;
			case 'U' : // Unidirectional
				flags = flags ^ STATS_UNIDIRECTIONAL;
				if (val.length()!=0)
					secU = Double.parseDouble(val);
				return true;
			default :
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		App.printHelp();
		System.out.println("Statistics:");
		System.out.println("\t-f <scenario name>");
		System.out.println("\t-r <list of transmission ranges>");
		System.out.println("\t-t [ print time (in progressive mode) ]");
		System.out.println("\t-G <sec> Partitioning Degree");
		System.out.println("\t-M <sec> MinCut");
		System.out.println("\t-N <sec> Node Degree");
		System.out.println("\t-P <sec> Partitions");
		System.out.println("\t-S <sec> Stability");
		System.out.println("\t-U <sec> Unidirectional");
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		new Statistics(args);
	}
}
