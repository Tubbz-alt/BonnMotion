package edu.bonn.cs.iv.bonnmotion;

import java.io.*;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.bonn.cs.iv.graph.*; // for mincut() and stability()
import edu.bonn.cs.iv.util.*;

/** A Topology object stores a set of Positions and offers methods to create adjacencies based on fixed or variable transmission ranges. */

public class Topology {
	public static final double SIR_RATIO = Math.pow(10.0, 0.25);

	protected Position[] pos = null;
	protected final int nodes;
	protected Graph adj;
	protected double[] range;
	protected double avgNeighbours = -1.0;
	/** Total number of edges. */
//	protected int edges = 0;
	/** Number of edges in connected components. */
	protected int connEdges = -1;
	/** Number of edges in biconnected components. */
	protected int biconnEdges = -1;

	protected int unidirEdges = -1;
	protected int bidirEdges = -1;

	protected long distortSeed = 0;
	protected double distortVar = 0.;
	protected double[][] distortion = null;

	protected int[] orig_id;

	public final boolean torus;
	public final double x, y, x2, y2;

	public Topology(Position[] pos) {
		this.pos = pos;
		nodes = pos.length;
		adj = new Graph();
		range = new double[nodes];

		torus = false; x = y = x2 = y2 = 0.0;
	}

	public Topology(Scenario s, double t) {
		this(s, t, false);
	}

//	public Topology(Scenario s, double t, boolean torus) {
//	MobileNode[] node = s.getNode();
//	nodes = node.length;
//	pos = new Position[nodes];
//	adj = new Graph();
//	for (int i = 0; i < nodes; i++)
//	pos[i] = node[i].positionAt(t);
//	range = new double[nodes];

//	this.torus = torus; x2 = (x = s.getX()) / 2.0; y2 = (y = s.getY()) / 2.0;
//	}

	public Topology(Scenario s, double t, boolean torus) {
		MobileNode[] node = s.getNode();
		int count_on_nodes = 0;

		// count amount of nodes that is not of
		for (int i = 0; i < node.length; i++) {
			if (node[i].positionAt(t).status != 2) 
				count_on_nodes++;
		}

		nodes = count_on_nodes;
		pos = new Position[count_on_nodes];
		adj = new Graph();
		orig_id = new int[count_on_nodes];

		int c = 0;
		for (int i = 0; i < node.length; i++) {
			if (node[i].positionAt(t).status != 2) {
				pos[c] = node[i].positionAt(t);
				orig_id[c] = i;
				c++;
			}
			//System.out.println("Nodes: "+node.length+" | On: "+count_on_nodes+" | Pos-Size: "+pos.length+" \n");
		}

		range = new double[nodes];

		this.torus = torus; x2 = (x = s.getX()) / 2.0; y2 = (y = s.getY()) / 2.0;
	}

	public Topology(String filename) throws FileNotFoundException, IOException {
		boolean torus = false;
		int nodes = 0, nSum = 0;
		double x = 0., y = 0.;
		String line;
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		while ((line = in.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line);
			String t0 = st.nextToken();
			if (t0.equals("torus")) {
				String tm = st.nextToken();
				torus = tm.equals("true");
			} else if (t0.equals("nodecount")) {
				nodes = Integer.parseInt(st.nextToken());
				pos = new Position[nodes];
				range = new double[nodes];
				adj = new Graph();
			} else if (t0.equals("x")) {
				x = Double.parseDouble(st.nextToken());
			} else if (t0.equals("y")) {
				y = Double.parseDouble(st.nextToken());
			} else if (t0.equals("distortion")) {
				distortSeed = Long.parseLong(st.nextToken());
				distortVar = Double.parseDouble(st.nextToken());
			} else if (t0.equals("node")) {
				int idx = Integer.parseInt(st.nextToken());
				t0 = st.nextToken();
				if (t0.equals("pos")) {
					double xpos = Double.parseDouble(st.nextToken());
					double ypos = Double.parseDouble(st.nextToken());
					pos[idx] = new Position(xpos, ypos);
					adj.checkNode(idx);
				} else if (t0.equals("range")) {
					range[idx] = Double.parseDouble(st.nextToken());
				} else if (t0.equals("neighbours")) {
					Node ni = adj.checkNode(idx);
					while (st.hasMoreTokens()) {
						nSum++;
						ni.addSucc(adj.checkNode(Integer.parseInt(st.nextToken())), 1);
					}
				}
			}
		}
		in.close();
		this.nodes = nodes;
		this.torus = torus;
		x2 = (this.x = x) / 2.;
		y2 = (this.y = y) / 2.;
//		avgNeighbours = (double)nSum / (double)nodes;
		if (distortVar > 0.)
			distort(distortSeed, distortVar);
	}

	public void write(String filename) throws FileNotFoundException, IOException {
		PrintWriter outfile = new PrintWriter(new FileOutputStream(filename));
		outfile.println("torus " + torus);
		outfile.println("nodecount " + nodes);
		outfile.println("x " + x);
		outfile.println("y " + y);
		if (distortion != null)
			outfile.println("distortion " + distortSeed + " " + distortVar);
		for (int i = 0; i < nodes; i++) {
			outfile.println("node " + i + " pos " + pos[i].x + " " + pos[i].y);
			outfile.println("node " + i + " range " + range[i]);
			Node ni = adj.checkNode(i);
			int nn = ni.outDeg();
			int nn_bi = ni.number_bi();
			outfile.println("node " + i + " orig_id " + orig_id[i] + " nn " + nn_bi);
			if (nn > 0) {
				outfile.print("node " + i + " neighbours");
				for (int j = 0; j < nn; j++)
					outfile.print(" " + ni.succAt(j).dstNode().getKey());
				outfile.println();
			}
		}
		outfile.close();
	}

	public double distance(int i, int j) {
		if (i == j)
			return 0.;
		double r;
		if (torus) {
			double xi = pos[i].x, xj = pos[j].x, yi = pos[i].y, yj = pos[j].y;
			double dx = xi - xj, dy = yi - yj;
			if (dx < -x2)
				dx += x;
			else if (dx > x2)
				dx -= x;
			if (dy < -y2)
				dy += y;
			else if (dy > y2)
				dy -= y;
			r = Math.sqrt(dx * dx + dy * dy);
		} else
			r = pos[i].distance(pos[j]);
		if (distortion != null) {
			int ii;
			int jj;
			if (i < j) {
				ii = i;
				jj = j;
			} else {
				ii = j;
				jj = i;
			}
			jj -= ii + 1;
//			System.out.println("XXX " + i + ":" + j + " -> " + ii + ":" + jj);
			r *= distortion[ii][jj];
		}
		return r;
	}

	public int nodeCount() {
		return nodes;
	}

	public double avgNeighbours() {
		if (avgNeighbours < 0.)
			buildGraph(false);
		return avgNeighbours;
	}

	public double getRange(int i) {
		return range[i];
	}

	public double[] getRanges() {
		double[] r = new double[nodes];
		System.arraycopy(range, 0, r, 0, nodes);
		return r;
	}

	public Position getPos(int i) {
		return pos[i];
	}

	/*	public double[] getRangeArray () {
		double[] returnArray = new double[range.length];
		System.arraycopy(range, 0, returnArray, 0, range.length);
		return returnArray;
	} */

	public Position[] getPositions() {
		Position[] r = new Position[nodes];
		System.arraycopy(pos, 0, r, 0, nodes);
		return r;
	}

	/** Do we have one connected component with the adjacencies as they are now? */
	public boolean connected() {
		boolean[] rLabel = new boolean[nodes];
		boolean[] dLabel = new boolean[nodes];

		for (int i = 0; i < nodes; i++) {
			rLabel[i] = false;
			dLabel[i] = true;
		}
		rLabel[0] = true;
		boolean c = true;
		while (c) {
			c = false;
			for (int i = 0; i < nodes; i++) {
				Node ni = adj.getNode(i);
				if (rLabel[i] && dLabel[i]) {
					dLabel[i] = false;
					for (int j = 0; j < ni.outDeg(); j++) {
						int jx = adj.indexOf(ni.succAt(j).dstNode().getKey());
						if (adjacent(jx, i))
							rLabel[jx] = c = true;
					}
				}
			}
		}
		boolean conn = true;
		for (int i = 0; (i < nodes) && conn; i++)
			conn &= rLabel[i];
		return conn;
	}

	public int connEdges() {
		if (connEdges < 0)
			calculateConnectivity();
		return connEdges;
	}

	public int biconnEdges() {
		if (biconnEdges < 0)
			calculateConnectivity();
		return biconnEdges;
	}

	protected void calculateConnectivity() {
		int partitions = nodes;
		int[] label = new int[nodes];
		int[] pSize = new int[nodes];
		for (int i = 0; i < nodes; i++) {
			label[i] = i;
			pSize[i] = 1;
		}
		for (int i = 0; i < nodes; i++) {
			Node ni = adj.nodeAt(i);
			for (int l = 0; l < ni.outDeg(); l++) {
				int j = adj.indexOf(ni.succAt(l).dstNode().getKey());
				if ((adjacent(j, i)) && (label[i] != label[j])) {
					partitions--;
					int o, n;
					if (label[i] < label[j]) {
						n = label[i];
						o = label[j];
					} else {
						n = label[j];
						o = label[i];
					}
					pSize[n] += pSize[o];
					pSize[o] = 0;
					for (int k = 0; k < label.length; k++)
						if (label[k] == o)
							label[k] = n;
				}
			}
		}

		connEdges = 0;
		biconnEdges = 0;
		Graph u = (Graph)adj.clone();
		u.unidirRemove(null);
		for (int i = 0; i < nodes; i++)
			if (pSize[i] > 0) {
				/*				if (pSize[i] < 10) {
					System.out.print("mini-cluster");
					for (int j = 0; j < nodes; j++)
						if (label[j] == i)
							System.out.print(" " + j);
					System.out.println("");
				} */
				connEdges += pSize[i] * (pSize[i] - 1);
				if (pSize[i] > 2)
					biconnEdges += u.biconnectivity(i)[1];
			}
		connEdges /= 2;
	}

	/** Is there an edge from src to dst? */
	public boolean adjacent(int src, int dst) {
		return (adj.getNode(src).getSucc(dst) != null);
	}

	/** @returns capacity estimation (0), overhead estimation for shortest-hop-paths (1), stretch factor (=max stretch) (2), avg. (weighted) stretch (3), bottleneckicity (4), overhead estimation for shortest-range-sum-paths (5) */
	public double[] capacityEstimator(Function f, double csRange) {
		int[][] hops = new int[nodes][nodes];
		int[][] rcount = new int[nodes][nodes];
		double[][] rdist = new double[nodes][nodes]; // "sum of ranges on all min-hop-paths", needed for overhead estimation
		double[][] sdist = new double[nodes][nodes]; // "lowest sum of distances", needed for stretch factor calculation
		double[][] tdist = new double[nodes][nodes]; // "lowest sum of ranges", needed for rval5
		for (int i = 0; i < nodes; i++) {
			for (int j = i; j < nodes; j++) {
				if (i == j) {
					hops[i][j] = hops[j][i] = 0;
					rcount[i][j] = rcount[j][i] = 1;
					rdist[i][j] = rdist[j][i] = 0.;
					sdist[i][j] = sdist[j][i] = 0.;
					tdist[i][j] = tdist[j][i] = 0.;
				} else {
					double d = distance(i, j);
					if ((d <= range[i]) && (d <= range[j])) {
						hops[i][j] = hops[j][i] = 1;
						rcount[i][j] = rcount[j][i] = 1;
						tdist[i][j] = rdist[i][j] = range[i];
						tdist[j][i] = rdist[j][i] = range[j];
						sdist[i][j] = sdist[j][i] = d;
					} else {
						hops[i][j] = hops[j][i] = -1;
						rcount[i][j] = rcount[j][i] = 0;
						rdist[i][j] = rdist[j][i] = Double.POSITIVE_INFINITY;
						sdist[i][j] = sdist[j][i] = Double.POSITIVE_INFINITY;
						tdist[i][j] = tdist[j][i] = Double.POSITIVE_INFINITY;
					}
				}
			}
		}
		for (int k = 0; k < nodes; k++) {
			for (int i = 0; i < nodes; i++) {
				if (hops[i][k] > 0) {
					for (int j = 0; j < nodes; j++) {
						if (hops[k][j] > 0) {
							int jh = hops[i][k] + hops[k][j];
							if ((hops[i][j] < 0) || (jh < hops[i][j])) {
								hops[i][j] = jh;
								rcount[i][j] = rcount[i][k] * rcount[k][j];
								rdist[i][j] = (double)rcount[k][j] * rdist[i][k] + (double)rcount[i][k] * rdist[k][j];
							} else if (jh == hops[i][j]) {
								rcount[i][j] += rcount[i][k] * rcount[k][j];
								rdist[i][j] += (double)rcount[k][j] * rdist[i][k] + (double)rcount[i][k] * rdist[k][j];
							}
							double sp = sdist[i][k] + sdist[k][j];
							if (sp < sdist[i][j])
								sdist[i][j] = sp;
							sp = tdist[i][k] + tdist[k][j];
							if (sp < tdist[i][j])
								tdist[i][j] = sp;
						}
					}
				}
			}
		}

		// needed only for throughput estimation, bottleneckicity:
		double[] busy = new double[nodes]; // station is busy sending
		double[] rbusy = new double[nodes]; // station is busy receiving
		double[] nbusy = new double[nodes]; // how much business is there around each station
		for (int i = 0; i < nodes; i++) {
			for (int j = 0; j < nodes; j++) {
				if (hops[i][j] > 0) {
					double w = (f == null) ? 1. : f.calculate(distance(i, j));
					for (int k = 0; k < nodes; k++) {
						if ((hops[i][j] == hops[i][k] + hops[k][j]) && (hops[i][k] >= 0) && (hops[k][j] >= 0)) {
							double v = w * (double)(rcount[i][k] * rcount[k][j]) / (double)rcount[i][j];
							if (hops[k][j] > 0)
								busy[k] += v;
							if (hops[i][k] > 0)
								rbusy[k] += v;
						}
					}
				}
			}
		}
		for (int i = 0; i < nodes; i++) {
//			old version. now, this value is _not_ added, because instead we are (falsely) adding the receive times for the downstream stations.
//			nbusy[i] += busy[i];
			nbusy[i] += rbusy[i];
			for (int j = i + 1; j < nodes; j++) {
				double d = distance(i, j);
				if (d <= csRange * range[i])
					nbusy[i] += rbusy[j];
				if (d <= csRange * range[j])
					nbusy[j] += rbusy[i];
				// old version, the way all those simulations are running now.....
				/*				if ((d <= csRange * range[i]) || (d <= csRange * range[j])) {
					nbusy[i] += busy[j];
					nbusy[j] += busy[i];
				} */
			}
		}

		double[] rVal = new double[6];
		int connected = 0;
		double wsum = 0.;
		for (int i = 0; i < nodes; i++) {
			if (nbusy[i] > rVal[4])
				rVal[4] = nbusy[i];
			for (int j = 0; j < nodes; j++) {
				if (hops[i][j] > 0) {
					double d = distance(i, j);

//					double tmp1 = rdist[i][j] * (double)hops[i][j] / (d * (double)rcount[i][j]);
					double tmp1 = rdist[i][j] / (d * (double)rcount[i][j]);
					double tmp2 = 0.; // throughput estimation
//					double tmp3 = 0.; // should be the same as tmp1 -> overhead

					for (int k = 0; k < nodes; k++) {
						if ((hops[i][k] >= 0) && (hops[k][j] > 0) && (hops[i][j] == hops[i][k] + hops[k][j])) {
							tmp2 += (double)(rcount[i][k] * rcount[k][j]) / nbusy[k];
//							tmp3 += (double)(rcount[i][k] * rcount[k][j]) * range[k];
						}
					}
					tmp2 *= d / ((double)(rcount[i][j] * hops[i][j]));
//					tmp3 *= (double)hops[i][j] / (d * (double)rcount[i][j]);

					/*					double tmp4 = tmp1 / tmp3;
					if ((tmp4 < 0.9999) || (tmp4 > 1.0001))
						System.out.println("hops " + hops[i][j] + " routes " + rcount[i][j] + " right " + tmp1 + " wrong " + tmp3); */

					double stretch = sdist[i][j] / d;
					if (stretch > rVal[2])
						rVal[2] = stretch;

					double tstretch = tdist[i][j] / d;

					if (f != null) {
						double w = f.calculate(d);
						wsum += w;
						rVal[0] += w * tmp2;
						rVal[1] += w * tmp1;
						rVal[3] += w * stretch;
						rVal[5] += w * tstretch;
					} else {
						connected++;
						rVal[0] += tmp2;
						rVal[1] += tmp1;
						rVal[3] += stretch;
						rVal[5] += tstretch;
					}
				}
			}
		}

		if (f == null)
			wsum = (double)connected;
		rVal[1] /= wsum;
		rVal[3] /= wsum;
		rVal[4] /= wsum;
		rVal[5] /= wsum;
		return rVal;
	}

	/** @returns stretch factor (=max stretch) (0), avg. (weighted) stretch (1), overhead estimation for shortest-range-sum-paths (2), overhead estimation for lowest-hopcount-paths (3), avg. (weighted) stretch (4), overhead estimation for shortest-range-sum-paths (5), overhead estimation for lowest-hopcount-paths (6), where 1-3 are calculated as ratio of means, whereas 4-6 are calculated as geometric mean */
	public double[] allPairsStuff(Function f) {
		int[][] hops = new int[nodes][nodes];
		double[][] sdist = new double[nodes][nodes]; // "lowest sum of distances", needed for stretch factor calculation
		double[][] tdist = new double[nodes][nodes]; // "lowest sum of ranges", needed for rval2
		double[][] rdist = new double[nodes][nodes]; // "sum of ranges on all min-hop-paths", needed for rval3
		double[][] rcount = new double[nodes][nodes]; // number of min-hop-paths, needed for rval3
		for (int i = 0; i < nodes; i++) {
			for (int j = i; j < nodes; j++) {
				if (i == j) {
					hops[i][j] = hops[j][i] = 0;
					sdist[i][j] = sdist[j][i] = 0.;
					tdist[i][j] = tdist[j][i] = 0.;
					rdist[i][j] = rdist[j][i] = 0.;
					rcount[i][j] = rcount[j][i] = 1.;
				} else {
					double d = distance(i, j);
					if ((d <= range[i]) && (d <= range[j])) {
						hops[i][j] = hops[j][i] = 1;
						tdist[i][j] = rdist[i][j] = range[i];
						tdist[j][i] = rdist[j][i] = range[j];
						sdist[i][j] = sdist[j][i] = d;
						rcount[i][j] = rcount[j][i] = 1.;
					} else {
						hops[i][j] = hops[j][i] = -1;
						sdist[i][j] = sdist[j][i] = Double.POSITIVE_INFINITY;
						tdist[i][j] = tdist[j][i] = Double.POSITIVE_INFINITY;
						rdist[i][j] = rdist[j][i] = Double.POSITIVE_INFINITY;
						rcount[i][j] = rcount[j][i] = 0.;
					}
				}
			}
		}
		for (int k = 0; k < nodes; k++) {
			for (int i = 0; i < nodes; i++) {
				if (hops[i][k] > 0) {
					for (int j = 0; j < nodes; j++) {
						if (hops[k][j] > 0) {
							int jh = hops[i][k] + hops[k][j];
							if ((hops[i][j] < 0) || (jh < hops[i][j])) {
								hops[i][j] = jh;
								rcount[i][j] = rcount[i][k] * rcount[k][j];
								rdist[i][j] = rcount[k][j] * rdist[i][k] + rcount[i][k] * rdist[k][j];
							} else if (jh == hops[i][j]) {
								rcount[i][j] += rcount[i][k] * rcount[k][j];
								rdist[i][j] += rcount[k][j] * rdist[i][k] + rcount[i][k] * rdist[k][j];
							}
							double sp = sdist[i][k] + sdist[k][j];
							if (sp < sdist[i][j])
								sdist[i][j] = sp;
							sp = tdist[i][k] + tdist[k][j];
							if (sp < tdist[i][j])
								tdist[i][j] = sp;
						}
					}
				}
			}
		}

		double[] rVal = new double[7];
		rVal[0] = 0.;
		double total_stretch = 0.;
		double total_tstretch = 0.;
		double total_hstretch = 0.;
		double total_dist = 0.;

		GeometricMeanCalculator stretchGeo = new GeometricMeanCalculator();
		GeometricMeanCalculator tstretchGeo = new GeometricMeanCalculator();
		GeometricMeanCalculator hstretchGeo = new GeometricMeanCalculator();

		int connected = 0;
		double wsum = 0.;
		for (int i = 0; i < nodes; i++) {
			for (int j = 0; j < nodes; j++) {
				if (hops[i][j] > 0) {
					double d = distance(i, j);

					double stretch = sdist[i][j] / d;
					if (stretch > rVal[0])
						rVal[0] = stretch;

					double minhoprangesum = rdist[i][j] / rcount[i][j];
					double hstretch = minhoprangesum / d;
					if (hstretch < 0) {
						System.err.println("hstretch = ("+rdist[i][j]+"/"+rcount[i][j]+")/"+d+" = "+hstretch);
						System.err.println("hops = " + hops[i][j]);
						System.exit(0);
					}

					if (f != null) {
						double w = f.calculate(d);
						wsum += w;
						total_stretch += w * sdist[i][j];
						total_tstretch += w * tdist[i][j];
						total_hstretch += w * minhoprangesum;
						total_dist += w * d;
						stretchGeo.add(Math.pow(stretch, w));
						tstretchGeo.add(Math.pow(tdist[i][j] / d, w));
						hstretchGeo.add(Math.pow(hstretch, w));
					} else {
						connected++;
						total_stretch += sdist[i][j];
						total_tstretch += tdist[i][j];
						total_hstretch += minhoprangesum;
						total_dist += d;
						stretchGeo.add(stretch);
						tstretchGeo.add(tdist[i][j] / d);
						hstretchGeo.add(hstretch);
					}
				}
			}
		}

		if (f == null)
			wsum = (double)connected;
		//double exp1 = (double)nodes / wsum;
		rVal[1] = total_stretch / total_dist;
		rVal[2] = total_tstretch / total_dist;
		rVal[3] = total_hstretch / total_dist;
		rVal[4] = stretchGeo.result();
		rVal[5] = tstretchGeo.result();
		rVal[6] = hstretchGeo.result();
		return rVal;
	}

	/** Create a new topology by multiplying all transmission ranges with f. */
	public Topology factor(double f) {
		Topology r = new Topology(pos);
		if (distortion != null)
			r.distort(distortSeed, distortVar);
		for (int i = 0; i < nodes; i++)
			r.range[i] = f * range[i];
		r.buildGraph(true);
		return r;
	}

	public void buildGraph(boolean really) {
		connEdges = biconnEdges = -1;
		unidirEdges = bidirEdges = 0;
		if ((adj == null) || really) {
			adj = new Graph();
			//int edges = 0;
			for (int i = 0; i < nodes; i++) {
				adj.checkNode(i);
				for (int j = i + 1; j < nodes; j++) {
					double d = distance(i, j);
					int c = 0;
					if (range[i] >= d) {
						adj.adjustWeight(i, j, 1);
						c++;
					}
					if (range[j] >= d) {
						adj.adjustWeight(j, i, 1);
						c++;
					}
					if (c == 1)
						unidirEdges++;
					else
						if (c == 2)
							bidirEdges++;
				}
			}
		} else {
			for (int i = 0; i < nodes; i++) {
				Node ni = adj.checkNode(i);
				for (int j = 0; j < ni.outDeg(); j++)
					if (ni.succAt(j).dstNode().getSucc(i) == null)
						unidirEdges++;
					else
						bidirEdges++;
			}
			bidirEdges /= 2;
		}
		avgNeighbours = 2. * (double)bidirEdges / (double)nodes;
	}

	public void smoothRanges(double alpha) {
		double[] c = new double[range.length]; // chaos
		double[] w = new double[range.length]; // weight sum
		for (int i = 0; i < nodes; i++) {
			for (int j = i + 1; j < nodes; j++) {
				double d = distance(i, j);
				double d1 = 1. / d;
				double tmp = (range[i] - range[j]) * (range[i] + range[j]) * d1;
				c[i] += tmp;
				w[i] += d1;
				c[j] -= tmp;
				w[j] += d1;
			}
		}
		double alpha1 = 1. - alpha;
		for (int i = 0; i < c.length; i++) {
			/*			w[i] /= (double)(nodes - 1);
			System.out.println("" + i + ": " + c[i] + " " + w[i] + " " + (1. / w[i])); */
			double sign = (c[i] < 0.) ? -1. : 1.;
			c[i] *= sign;
			range[i] = alpha * range[i] + alpha1 * (range[i] + sign * Math.sqrt(c[i] / w[i]));
		}
	}

	public void smoothRanges1(double alpha) {
		double[] r = new double[range.length]; // new ranges
		double[] w = new double[range.length]; // weight sum
		for (int i = 0; i < nodes; i++) {
			for (int j = i + 1; j < nodes; j++) {
				double d = distance(i, j);
				double d1 = 1. / d;
				r[i] += d1 * range[j];
				w[i] += d1;
				r[j] += d1 * range[i];
				w[j] += d1;
			}
		}
		double alpha1 = 1. - alpha;
		for (int i = 0; i < nodes; i++) {
			r[i] = alpha * range[i] + alpha1 * (r[i] / w[i]);
		}
		range = r;
		buildGraph(true);
	}

	public double[] asymmetry() {
		GeometricMeanCalculator asym_avg = new GeometricMeanCalculator();
		double[] r = new double[2];
		r[0] = 1.;
		int maxIn = 0, maxOut = 0;
		for (int i = 0; i < nodes; i++) {
			Node ni = adj.checkNode(i);
			int indeg = ni.inDeg();
			int outdeg = ni.outDeg();
			if (maxIn < indeg)
				maxIn = indeg;
			if (maxOut < outdeg)
				maxOut = outdeg;
			if ((indeg > 0) && (outdeg > 0))
				asym_avg.add((indeg > outdeg) ? ((double)indeg / (double)outdeg) : ((double)outdeg / (double)indeg));
		}
		if ((maxIn > 0) && (maxOut > 0))
			r[0] = (maxIn > maxOut) ? ((double)maxIn / (double)maxOut) : ((double)maxOut / (double)maxIn);
			r[1] = asym_avg.result();
			return r;
	}

	public void setRanges(double[] range) {
		System.arraycopy(range, 0, this.range, 0, nodes);
		/*		for (int i = 0; i < nodes; i++)
			this.range[i] = range[i]; */
		buildGraph(true);
	}

	/** Set all transmission ranges to txRange and create adjacencies. */
	public void setCommonRange(double txRange) {
		setCommonRange(txRange, false);
	}

	/** Set all transmission ranges to txRange and create adjacencies. */
	public void setCommonRange(double txRange, boolean optimise) {
		for (int i = 0; i < nodes; i++)
			range[i] = txRange;
		buildGraph(true);
		if (optimise) {
			for (int i = 0; i < nodes; i++)
				range[i] = 0.;
			for (int i = 0; i < nodes; i++)
				for (int j = i + 1; j < nodes; j++)
					if (adjacent(i, j)) {
						double d = distance(i, j);
						if (range[i] < d)
							range[i] = d;
						if (range[j] < d)
							range[j] = d;
					}
		}
	}

	/** Set transmission ranges such that each node has at least neighbourGoal bidirectional links, but don't let a transmission range exceed maxTxRange. */
	public void setNearestNeighboursRanges(int neighbourGoal, double maxTxRange) {
		setNearestNeighboursRanges(neighbourGoal, maxTxRange, 0);
	}

	public void setNearestNeighboursRanges(int neighbourGoal, double maxTxRange, int mode) {
		/*
			mode 0: connect each node to all of _his_ nearest neighbours
			mode 1: mutual nearest neighbours
			mode 2: LINT
			mode 3: test -- mode 0 + randomly set maxTxRange for some nodes
		 */
		if (neighbourGoal >= nodes) {
			throw new RuntimeException("This is ridiculous: How shall someone find at least " + neighbourGoal + " neighbours if there are only " + nodes + " nodes in the simulation area?!");
		}

		for (int i = 0; i < nodes; i++)
			range[i] = 0.;

		if ((mode == 0) || (mode == 3)) {
			for (int i = 0; i < nodes; i++) {
				Heap nh = new Heap();
				for (int j = 0; j < nodes; j++)
					if (i != j) {
						double dist = distance(i, j);
						if (dist <= maxTxRange)
							nh.add(new Integer(j), dist);
					}
				double dist = 0.;
				for (int j = 0; (j < neighbourGoal) && (nh.size() > 0); j++) {
					dist = nh.minLevel();
					int k = ((Integer)nh.deleteMin()).intValue();
					if (range[k] < dist)
						range[k] = dist;
				}
				if (range[i] < dist)
					range[i] = dist;
			}
		} else if (mode == 1) {
			Graph nn = new Graph();
			for (int i = 0; i < nodes; i++) {
				Heap nh = new Heap();
				for (int j = 0; j < nodes; j++)
					if (i != j) {
						double dist = distance(i, j);
						if (dist <= maxTxRange)
							nh.add(new Integer(j), dist);
					}
				for (int j = 0; (j < neighbourGoal) && (nh.size() > 0); j++) {
					int k = ((Integer)nh.deleteMin()).intValue();
					nn.checkNode(i).addSucc(nn.checkNode(k), 1);
				}
			}
			for (int i = 0; i < nodes; i++) {
				Node ni = nn.checkNode(i);
				for (int j = 0; j < ni.outDeg(); j++) {
					Node nj = ni.succAt(j).dstNode();
					int k = nj.getKey();
					if ((i < k) && (nj.getSucc(i) != null)) {
						double d = distance(i, k);
						if (range[i] < d)
							range[i] = d;
						if (range[k] < d)
							range[k] = d;
					}
				}
			}
		} else if (mode == 2) {
			double[] trrange = new double[nodes];
			int[] neighbours = new int[nodes];
			for (int i = 0; i < nodes; i++) {
				trrange[i] = 0.;
				neighbours[i] = 0;
			}
			boolean done = false;
			while (! done) {
				int lpi = -1;
				for (int i = 0; i < nodes; i++)
					if ((neighbours[i] != neighbourGoal) && (trrange[i] < maxTxRange) && ((lpi < 0) || (trrange[i] < trrange[lpi])))
						lpi = i;
				done = (lpi < 0);
				if (! done) {
					double nnrange = maxTxRange;
					int nnidx = -1;
					for (int i = 0; i < nodes; i++)
						if (i != lpi) {
							double d = distance(lpi, i);
							if ((d > trrange[lpi]) && (d <= nnrange)) {
								nnrange = d;
								nnidx = i;
							}
						}
					trrange[lpi] = nnrange;
					if ((nnidx >= 0) && (trrange[nnidx] >= nnrange)) {
						neighbours[nnidx]++;
						if (range[nnidx] < nnrange)
							range[nnidx] = nnrange;
						neighbours[lpi]++;
						range[lpi] = nnrange;
					}
				}
			}
		}
		if (mode == 3) {
			boolean[] mark = new boolean[nodes];
			for (int i = 0; i < nodes; i++)
				mark[i] = false;
			int m = 5;
			for (int i = 0; i < m; i++) {
				int c;
				do {
					c = (int)(nodes * Math.random());
				} while (mark[c]);
				mark[c] = true;
				range[c] = maxTxRange;
			}
		}
		buildGraph(true);
	}

	public void setMSTRanges(double maxTxRange) {
		int[] pi = new int[nodes]; // partition index
		int partitions = nodes;
		for (int i = 0; i < nodes; i++) {
			pi[i] = i;
			range[i] = 0.;
		}
		Heap cdn = new Heap();
		for (int i = 0; i < nodes; i++)
			setMSTRanges_heapfiller(cdn, i, -1., maxTxRange);
		while ((partitions > 1) && (cdn.size() > 0)) {
			double d = cdn.minLevel();
			int[] pair = (int[])cdn.deleteMin();
			if (pi[pair[0]] != pi[pair[1]]) {
				if (range[pair[0]] < d)
					range[pair[0]] = d;
				if (range[pair[1]] < d)
					range[pair[1]] = d;
				int cp = pi[pair[1]];
				for (int j = 0; j < nodes; j++)
					if (pi[j] == cp)
						pi[j] = pi[pair[0]];
				partitions--;
			}
			setMSTRanges_heapfiller(cdn, pair[0], d, maxTxRange);
		}
		buildGraph(true);
	}

	protected void setMSTRanges_heapfiller(Heap h, int i, double lo, double hi) {
		int mi = -1;
		double md = Double.POSITIVE_INFINITY;
		for (int j = 0; j < nodes; j++)
			if (i != j) {
				double d = distance(i, j);
				if ((d > lo) && (d <= hi) && (d < md)) {
					mi = j;
					md = d;
				}
			}
		int[] pair = new int[2];
		pair[0] = i;
		pair[1] = mi;
		h.add(pair, md);
	}

	public void setXTCRanges(double maxTxRange) {
		boolean[] processed = new boolean[nodes];
		for (int i = 0; i < nodes; i++) {
//			System.out.println("i=" + i);
			Heap nh = new Heap();
			for (int j = 0; j < nodes; j++) {
				processed[j] = false;
				if (i != j) {
					double dist = distance(i, j);
					if (dist <= maxTxRange)
						nh.add(new Integer(j), dist);
				}
			}
			while (nh.size() > 0) {
				double dist = nh.minLevel();
				int j = ((Integer)nh.deleteMin()).intValue();
//				System.out.print("j=" + j + " dist=" + dist + " ");
				for (int k = 0; k < nodes; k++) {
					if (processed[k] && (distance(j, k) < dist)) {
						processed[j] = true;
//						System.out.println("noconnect:" + k);
						break;
					}
				}
				if (! processed[j]) {
//					System.out.println("connect");
//					if ((i == 249) || (j == 249)) System.out.println(""+i+"("+pos[i]+")->"+j+"("+pos[j]+")");
					range[i] = dist;
					if ((j < i) && (range[j] < range[i])) {
						System.err.println("RNG error: " + j + "<" + i + " && " + range[j] + "<" + range[i]); //System.exit(0);
						range[j] = range[i];
					}
					processed[j] = true;
				}// else processed[j] = false; // uncomment this (and comment out the "RNG error") for the NTC wannabe-Delauny-topology
			}
		}
		buildGraph(true);
	}

	public void setGabrielRanges(double maxTxRange) {
		double[] idist = new double[nodes];
		for (int i = 0; i < nodes; i++) {
			Heap nh = new Heap();
			boolean[] processed = new boolean[nodes];
			idist[i] = Double.POSITIVE_INFINITY;
			for (int j = 0; j < nodes; j++) {
				processed[j] = false;
				if (i != j) {
					idist[j] = distance(i, j);
					if (idist[j] <= maxTxRange)
						nh.add(new Integer(j), idist[j]);
				}
			}
			while (nh.size() > 0) {
				double dist = nh.minLevel();
				int j = ((Integer)nh.deleteMin()).intValue();
				for (int k = 0; k < nodes; k++) {
					double dik = idist[k];
					if (dik < dist) {
						double djk = distance(j, k);
						if (dik * dik + djk * djk < dist * dist) {
							processed[j] = true;
							break;
						}
					}
				}
				if (! processed[j]) {
					range[i] = dist;
					if ((j < i) && (range[j] < range[i])) {
						System.err.println("Gabriel error"); System.exit(0);
						range[j] = range[i];
					}
					processed[j] = true;
				}
			}
		}
		buildGraph(true);
	}

	public void setDiNTCRanges(double maxTxRange) {
		Vector[] links = new Vector[nodes];
		for (int i = 0; i < nodes; i++) {
			links[i] = new Vector();
			Heap nh = new Heap();
			boolean[] processed = new boolean[nodes];
			for (int j = 0; j < nodes; j++) {
				processed[j] = false;
				if (i != j) {
					double dist = distance(i, j);
					if (dist <= maxTxRange)
						nh.add(new Integer(j), dist);
				}
			}
			while (nh.size() > 0) {
				double dist = nh.minLevel();
				int j = ((Integer)nh.deleteMin()).intValue();
				for (int k = 0; k < nodes; k++) {
					if (processed[k] && (distance(j, k) < dist)) {
						processed[j] = true;
						break;
					}
				}
				if (! processed[j]) {
					processed[j] = true;
					links[i].addElement(new Integer(j));
					/*					System.out.println("# " + i + "->" + j);
					System.out.println("" + pos[i].x + " " + pos[i].y);
					System.out.println("" + pos[j].x + " " + pos[j].y);
					System.out.println(""); */
				} else
					processed[j] = false;
			}
		}
		for (int i = 0; i < nodes; i++) {
			for (int ji = 0; ji < links[i].size(); ji++) {
				int j = ((Integer)links[i].elementAt(ji)).intValue();
				boolean agree = false;
				for (int k = 0; (! agree) && (k < links[j].size()); k++) {
					int l = ((Integer)links[j].elementAt(k)).intValue();
					agree = (i == l);
				}
				if (agree)
					range[i] = distance(i, j);
//				else System.out.println("no they don't");
			}
		}
		buildGraph(true);
	}

	private final static boolean debug(int i) {
		return false;
	}

	/* mode:
		0: simple CBTC
		1: connect only if it reduces gapsum
		2: "submit" only if it reduces gapsum to zero or at least by alpha
		"asymmetric edge removal" by setting bit 2^2
		"pairwise edge removal" by setting bit 2^3
	 */
	public void setConeBasedRanges(double alpha, double maxTxRange, int mode) {
		Graph g = null;
		boolean er_asymmetric = ((mode & 0x04) > 0);
		boolean er_pairwise = ((mode & 0x08) > 0);
		if (er_asymmetric || er_pairwise) {
			g = new Graph();
		}
		mode &= 0x03;
		//double alpha2 = alpha/2.;
		double pi3 = Math.PI/3.;
		for (int i = 0; i < nodes; i++)
			range[i] = 0.0;
		Position refv = new Position(1., 0.);
		double[] angle = new double[nodes];
		double[] dist = new double[nodes];
		//int[] apos = new int[nodes];
		for (int i = 0; i < nodes; i++) {
			Node ni = null;
			if (g != null)
				ni = g.checkNode(i);
			boolean debug = false;
			if (debug)
				System.out.println("i="+i);
			Heap dn = new Heap(); // all potential neighbours we are not yet connected to
			Vector av = new Vector();
			Vector<Integer> pn = new Vector<Integer>(); // potential new neighbours
			for (int j = 0; j < nodes; j++) {
				if ((i != j) && ((dist[j] = distance(i, j)) < maxTxRange)) {
					angle[j] = Position.angle2(refv, Position.diff(pos[i], pos[j]));
					dn.add(new Integer(j), dist[j]);
				}
			}
			double empty = cb2_empty_cones(av, alpha);
			double eLast = empty;
			while ((dn.size() > 0) && (empty > 0.)) {
				Integer jo = (Integer)dn.deleteMin();
				int j = jo.intValue();
				cb2_insert_angle(av, angle[j]);
				double eNew = cb2_empty_cones(av, alpha);
				if ((mode == 0) || ((mode >= 1) && (mode <= 2) && (eNew < eLast)) || (eNew == 0.) || (eNew + alpha < empty)) {
					pn.addElement(jo);
					if (debug) {
						System.out.println("candidate j=" + j + " angle=" + angle[j] + " eNew=" + eNew);
					}
				}
				if (((mode <= 1) && (eNew < eLast)) || (eNew == 0.) || (eNew + alpha < empty)) {
					for (int k = 0; k < pn.size(); k++) {
						j = ((Integer)pn.elementAt(k)).intValue();
						if (debug)
							System.out.println("submit j=" + j);
						if (g == null) {
							if (range[i] < dist[j])
								range[i] = dist[j];
							if (range[j] < dist[j])
								range[j] = dist[j];
						} else
							ni.addSucc(g.checkNode(j), 1);
					}
//					System.out.println("batchsize=" + pn.size());
					pn = new Vector<Integer>();
					empty = eNew;
				}
				eLast = eNew;
			}
		}

		if (er_asymmetric)
			for (int i = 0; i < nodes; i++) {
				Node ni = g.getNode(i);
				for (int i2 = 0; i2 < ni.outDeg(); i2++) {
					Edge e = ni.succAt(i2);
					Node nj = e.dstNode();
					if (nj.getSucc(i) == null)
						ni.delSuccAt(i2--);
				}
			}

		if (er_pairwise)
			for (int i = 0; i < nodes; i++) {
				Node ni = g.getNode(i);
				boolean[] redundant = new boolean[ni.outDeg()];
				Position[] direction = new Position[ni.outDeg()];
				for (int ji = 0; ji < redundant.length; ji++) {
					int j = ni.succAt(ji).dstNode().getKey();
					redundant[ji] = false;
					dist[ji] = distance(i, j);
					direction[ji] = Position.diff(pos[i], pos[j]);
				}
				for (int ji = 0; ji < ni.outDeg(); ji++) {
					for (int ki = ji+1; ki < ni.outDeg(); ki++) {
						if (Position.angle(direction[ji], direction[ki]) < pi3) {
							if (dist[ji] > dist[ki])
								redundant[ji] = true;
							else
								redundant[ki] = true;
						}
					}
				}
				double maxnr = 0.;
				for (int j = 0; j < redundant.length; j++)
					if ((! redundant[j]) && (dist[j] > maxnr))
						maxnr = dist[j];
				int deleted = 0;
				for (int ji = 0; ji < ni.outDeg() + deleted; ji++) {
					if (redundant[ji] && (dist[ji] > maxnr)) {
						int idx = ji - deleted;
						ni.succAt(idx).dstNode().delSucc(i);
						ni.delSuccAt(idx);
						deleted++;
					}
				}
			}

		if (g != null)
			for (int i = 0; i < nodes; i++) {
				Node ni = g.getNode(i);
				for (int i2 = 0; i2 < ni.outDeg(); i2++) {
					Edge e = ni.succAt(i2);
					int j = e.dstNode().getKey();
					double d = distance(i,j);
					if (range[i] < d)
						range[i] = d;
					if (range[j] < d)
						range[j] = d;
				}
			}

		buildGraph(true);
	}

	/** AER = Asymmetric Edge Removal */
	protected void setConeBasedRangesAER(double alpha, double maxTxRange) {
		//double alpha2 = alpha/2.;
		Graph g = new Graph();
		for (int i = 0; i < nodes; i++) {
			range[i] = 0.0;
			g.checkNode(i);
		}
		Position refv = new Position(1., 0.);
		double[] angle = new double[nodes];
		double[] dist = new double[nodes];
		for (int i = 0; i < nodes; i++) {
			boolean debug = false;
			if (debug)
				System.out.println("i="+i);
			Heap dn = new Heap(); // all potential neighbours we are not yet connected to
			Vector av = new Vector();
			//Vector pn = new Vector(); // potential new neighbours
			for (int j = 0; j < nodes; j++) {
				if ((i != j) && ((dist[j] = distance(i, j)) < maxTxRange)) {
					angle[j] = Position.angle2(refv, Position.diff(pos[i], pos[j]));
					dn.add(new Integer(j), dist[j]);
				}
			}
			double empty = cb2_empty_cones(av, alpha);
			while ((dn.size() > 0) && (empty > 0.)) {
				Integer jo = (Integer)dn.deleteMin();
				int j = jo.intValue();
				int apos = cb2_insert_angle(av, angle[j]);
				double eNew = cb2_empty_cones(av, alpha);
				if ((eNew < empty) || (eNew == 0.)) {
					if (debug)
						System.out.println("submit j=" + j);
					g.getNode(i).addSucc(g.getNode(j), 1);
					empty = eNew;
				} else
					av.removeElementAt(apos); // efficiency
			}
		}
		for (int i = 0; i < nodes; i++) {
			Node ni = g.getNode(i);
			for (int i2 = 0; i2 < ni.outDeg(); i2++) {
				Edge e = ni.succAt(i2);
				Node nj = e.dstNode();
				if (nj.getSucc(i) != null) {
					int j = nj.getKey();
					double d = distance(i,j);
					if (range[i] < d)
						range[i] = d;
					if (range[j] < d)
						range[j] = d;
				}
			}
		}
		buildGraph(true);
	}

	protected int cb2_insert_angle(Vector<Double> av, double a) {
		int pos = 0;
		while ((pos < av.size()) && (((Double)av.elementAt(pos)).doubleValue() < a))
			++pos;
		av.insertElementAt(new Double(a), pos);
//		System.out.println("insert a="+a);
		return pos;
	}

	protected double cb2_empty_cones(Vector av, double alpha) {
		if (av.size() == 0)
			return Double.POSITIVE_INFINITY;
		else if (av.size() == 1)
			return 2.*Math.PI;
		else {
			double r = 0.;
			for (int i = 0; i < av.size(); i++) {
				double a0 = ((Double)av.elementAt(i)).doubleValue();
				double a1 = (i+1 < av.size()) ? ((Double)av.elementAt(i+1)).doubleValue() : (((Double)av.elementAt(0)).doubleValue() + 2.*Math.PI);
				double da = a1 - a0;
				if (da > alpha) {
					r += da - alpha;
				}
//				System.out.println("check a0="+a0+" a1="+a1+" r="+r);
			}
//			System.out.println("check total r="+r);
			return r;
		}
	}

	/** Cone-based, optimised.
	@param strangeMode if set to true, add neighbour only if we don't already have a neighbour with angle alpha/2
	 */	
	public void setConeBasedRangesOld(double alpha, double maxTxRange, boolean strangeMode) {
		double alpha2 = alpha / 2.;

		for (int i = 0; i < nodes; i++) {
			range[i] = 0.0;
		}
		Vector[] neigh = new Vector[nodes];
		Vector[] av = new Vector[nodes];
		for (int i = 0; i < nodes; i++) {
			neigh[i] = new Vector();
			av[i] = new Vector();
		}
		Position refv = new Position(1., 0.);
		for (int i = 0; i < nodes; i++) {
			if (! angleCheck(av[i], alpha)) {
				Heap dn = new Heap(); // all neighbours we are not yet connected to
				double[] angle = new double[nodes];
				boolean[] nu = new boolean[nodes]; // are these potentially new neighbours
				for (int j = 0; j < nodes; j++) {
					double d;
					if ((i != j) && ((d = distance(i, j)) < maxTxRange)) {
						angle[j] = Position.angle2(refv, Position.diff(pos[i], pos[j]));
						boolean adj = false;
						int i1, i2;
						if (av[i].size() <= av[j].size()) {
							i1 = i;
							i2 = j;
						} else {
							i1 = j;
							i2 = i;
						}
						for (int k = 0; (! adj) && (k < neigh[i1].size()); k++)
							adj = (i2 == ((Integer)neigh[i1].elementAt(k)).intValue());
						if (adj)
							nu[j] = false;
						else {
							dn.add(new Integer(j), d);
							nu[j] = true;
						}
					} else {
						angle[j] = -1.;
						nu[j] = false;
					}
				}
				boolean todo = true;
				while (todo && (dn.size() > 0)) {
					if (neigh[i].size() != av[i].size())
						System.err.println("n" + neigh[i].size() + " a" + av[i].size());
					int j = ((Integer)dn.deleteMin()).intValue();
					if (av[i].size() == 0) {
						if (debug(i))
							System.out.println("XX " + i + " 0need j=" + j + " @ " + angle[j]);
						av[i].addElement(new Double(angle[j]));
						neigh[i].addElement(new Integer(j));
						todo = ! angleCheck(av[i], alpha);
					} else {
						double a0 = ((Double)av[i].elementAt(av[i].size() - 1)).doubleValue() - 2. * Math.PI, a1 = -13000.;
						int k;
						for (k = 0; k <= av[i].size(); k++) {
							a1 = (k < av[i].size()) ? ((Double)av[i].elementAt(k)).doubleValue() : ((Double)av[i].elementAt(0)).doubleValue() + 2. * Math.PI;
							if (angle[j] < a1)
								break;
							a0 = a1;
						}
						if ((a1 - a0 > alpha) && ((! strangeMode) || ((a1 - angle[j] > alpha2) && (angle[j] - a0 > alpha2)))) { // we need this node j...
							// ... except if it's in a dead end:
							boolean dead = false;
							if (debug(i))
								System.out.print("dead end test: " + i + "->" + j + " a=" + angle[j] + " in [" + a0 + ";" + a1 + "]:");
							// one side:
							double dead0 = a0 + alpha2;
							if (debug(i))
								System.out.print(" 1) dead0=" + dead0);
							if (angle[j] < dead0) {
								if (dead0 < 0.)
									dead0 += 2. * Math.PI;
								else if (dead0 > 2. * Math.PI)
									dead0 -= 2. * Math.PI;
//								double dead1 = dead0 + alpha;
								double dead1 = angle[j] + alpha;
								if (dead1 > 2. * Math.PI)
									dead1 -= 2. * Math.PI;
								if (debug(i))
									System.out.print(" dead1=" + dead1);
								dead = true;
								for (int l = 0; (l < angle.length) && dead; l++)
									if (dead0 < dead1)
										dead = ! ((dead0 < angle[l]) && (angle[l] < dead1));
									else
										dead = ! ((angle[l] >= 0.) && ((dead0 < angle[l]) || (angle[l] < dead1)));
							} else {
								// other side:
								double dead1 = a1 - alpha2;
								if (debug(i))
									System.out.print(" 2) dead1=" + dead1);
								if (angle[j] > dead1) {
									if (dead1 < 0.)
										dead1 += 2. * Math.PI;
//									dead0 = dead1 - alpha;
									dead0 = angle[j] - alpha;
									if (dead0 < 0.)
										dead0 += 2. * Math.PI;
									if (debug(i))
										System.out.print(" dead0=" + dead0);
									dead = true;
									for (int l = 0; (l < angle.length) && dead; l++)
										if (dead0 < dead1)
											dead = ! ((dead0 < angle[l]) && (angle[l] < dead1));
										else
											dead = ! ((angle[l] >= 0.) && ((dead0 < angle[l]) || (angle[l] < dead1)));
								}
							}
							if (debug(i))
								System.out.println("");

							if (! dead) {
								if (debug(i))
									System.out.println("XX " + i + " need j=" + j + " @ " + angle[j] + " k=" + k);
								av[i].insertElementAt(new Double(angle[j]), k);
								neigh[i].insertElementAt(new Integer(j), k);
								todo = ! angleCheck(av[i], alpha);

								// perhaps we can kick out the angle-neighbouring guys:
								// one direction:
//								System.err.println("XX dir1");
								int n0 = k - 1;
								if (n0 < 0)
									n0 = neigh[i].size() - 1;
								while (true) {
									int n1 = n0 - 1;
									if (n1 < 0)
										n1 = neigh[i].size() - 1;
									if (debug(i))
										System.err.println("XX " + i + " kick? n0=" + n0 + " n1=" + n1);
									double a = ((Double)av[i].elementAt(n1)).doubleValue();
									if (n1 >= k) // we had an underflow
										a -= 2. * Math.PI;
									if (angle[j] - a <= alpha) { // kick out n0?
										if (nu[((Integer)neigh[i].elementAt(n0)).intValue()]) { // we don't necessarily need n0
											if (debug(i))
												System.err.println("XX " + i + " kickit! " + angle[j] + "-" + a + "=" + (angle[j] - a));
											av[i].removeElementAt(n0);
											neigh[i].removeElementAt(n0);
											if (n0 < k)
												k--;
											if (n0 < n1)
												n1--;
										}
									} else
										break;
									n0 = n1;
								}
								// other direction:
//								System.err.println("XX dir2");
								n0 = k + 1;
								if (n0 == neigh[i].size())
									n0 = 0;
								while (true) {
									int n1 = n0 + 1;
									if (n1 == neigh[i].size())
										n1 = 0;
									if (debug(i))
										System.err.println("XX " + i + " kick? n0=" + n0 + " n1=" + n1);
									double a = ((Double)av[i].elementAt(n1)).doubleValue();
									if (n1 <= k) // we had an overflow
										a += 2. * Math.PI;
									if (a - angle[j] <= alpha) { // kick out n0?
										if (nu[((Integer)neigh[i].elementAt(n0)).intValue()]) { // we don't necessarily need n0
											if (debug(i))
												System.err.println("XX " + i + " kickit! " + a + "-" + angle[j] + "=" + (a - angle[j]));
											av[i].removeElementAt(n0);
											neigh[i].removeElementAt(n0);
											if (n0 < k)
												k--;
											if (n0 < n1)
												n1--;
										}
									} else
										break;
									n0 = n1;
								}
							}
						}
					}
				}
				// connect these guys
				for (int j = 0; j < neigh[i].size(); j++) {
					int k = ((Integer)neigh[i].elementAt(j)).intValue();
					if (nu[k]) {
						double ra = (angle[k] < Math.PI) ? (angle[k] + Math.PI) : (angle[k] - Math.PI);
						int ipos = 0;
						while ((ipos < av[k].size()) && (((Double)av[k].elementAt(ipos)).doubleValue() < ra))
							ipos++;
						av[k].insertElementAt(new Double(ra), ipos);
						neigh[k].insertElementAt(new Integer(i), ipos);
						double dist = distance(i, k);
						if (dist > range[i])
							range[i] = dist;
						if (dist > range[k])
							range[k] = dist;
					}
				}
			}
		}
		buildGraph(true);
	}

	protected boolean angleCheck(Vector av, double alpha) {
		boolean satisfied = (av.size() > 0);
		for (int l = 0; satisfied && (l < av.size()); l++) {
			double a0 = ((Double)av.elementAt(l)).doubleValue();
			double a1 = ((l+1) < av.size()) ? ((Double)av.elementAt(l+1)).doubleValue() : (((Double)av.elementAt(0)).doubleValue() + 2. * Math.PI);
			if (a1 - a0 > alpha)
				satisfied = false;
		}
		return satisfied;
	}

	protected Heap fillHeap(int i, double maxTxRange) {
		Heap tmp = new Heap();
		for (int j = 0; j < nodes; j++)
			if (i != j) {
				double dist = distance(i, j);
				if ((dist > range[i]) && (dist <= maxTxRange))
					tmp.add(new Integer(j), dist);
			}
		Heap r = new Heap();
		for (int j = 0; (j < 20) && (tmp.size() > 0); j++) {
			double dist = tmp.minLevel();
			Object data = tmp.deleteMin();
			r.add(data, dist);
		}
		return r;
	}

	/** Print current topology and transmission ranges in a format that can be loaded within an ns-2 TCL script. */
	public void print_ns() {
		for (int i = 0; i < nodes; i++) {
//			double range2 = range[i] * range[i];
//			double pt = (range[i] <= 86.142470561432130598645681569524) ? 5.3530386481240768322072823173423e-7 * range2 : 7.2138271604938271604938271604938e-11 * range2 * range2;
			System.out.println("set range(" + i + ") " + range[i]);
		}
	}

	public double minGlobalRange(double dmax, double minconn, boolean biconn) {
		double max = dmax;
		double min = 0.0;
		int allEdges = nodes * (nodes - 1) / 2;
		double connval;
		Vector<Double> rd = null;
		do {
			double t = (min + max) / ((max == dmax) ? 16.0 : 2.0);
//			System.err.println("min=" + min + " max=" + max + " t=" + t);
			/*			if (rd != null)
				System.err.print("rd.size=" + rd.size() + " ");
			System.err.println("t=" + t); */
			setCommonRange(t);
			connval = (double)(biconn ? biconnEdges() : connEdges()) / (double)allEdges;
//			if (connected() && ((! biconn) || (articulationPoints() == 0))) {
			if (connval >= minconn)
				max = t;
			else
				min = t;
			if (max - min <= 1.0)
				if (rd == null) {
					rd = new Vector<Double>();
					for (int i = 0; i < nodes; i++)
						for (int j = i + 1; j < nodes; j++) {
							double dist = distance(i, j);
							if ((dist >= min) && (dist <= max))
								rd.addElement(new Double(dist));
						}
				} else {
					Vector<Double> old = rd;
					rd = new Vector<Double>();
					for (int i = 0; i < old.size(); i++) {
						Double dist = (Double)old.elementAt(i);
						if ((dist.doubleValue() >= min) && (dist.doubleValue() <= max))
							rd.addElement(dist);
					}
				}
		} while ((max - min > 1.0) || (rd.size() > 1));
		setCommonRange(((Double)rd.elementAt(0)).doubleValue());

		// sanity check

		connval = (double)(biconn ? biconnEdges() : connEdges()) / (double)allEdges;
		if (connval < minconn) {
			System.err.println("whoopsydaisy: " + connval);
			System.exit(0);
		}
		return range[0];
	}

	public double biconnRatio() {
		return (double)(2 * biconnectivity()[1]) / (double)(nodes * (nodes - 1));
	}

	/** Vary distances to create topologies that possibly don't fulfill the triangle inequality as possible under real-world radio conditions. */
	public void distort(long seed, double var) {
		double v2 = var / 2.;
		Random rnd = new Random(seed);
		double[][] f = new double[nodes-1][];
		for (int i = 0; i < f.length; i++) {
			f[i] = new double[nodes-1 - i];
			for (int j = 0; j < f[i].length; j++)
				f[i][j] = Math.exp(var * (rnd.nextGaussian() - v2));
		}
		distortSeed = seed;
		distortVar = var;
		distortion = f;
		buildGraph(true);
	}

	public int articulationPoints() {
		return biconnectivity()[0];
	}

	public int[] biconnectivity() {
		return adj.biconnectivity(0);
	}

	public int stability() {
		Graph u = (Graph)adj.clone();
		u.unidirRemove(null);
		return u.stability();
	}

	public int mincut() {
		if (connected()) {
			Graph t = (Graph)adj.clone();
			t.unidirRemove(null);
			t = Graph.buildSeperatorTree(t);
			Edge m = t.findMinEdge();
			if (m != null)
				return m.weight;
			else
				return 0;
		} else
			return 0;
	}

	// this method assumes there is a bidirectional link between s and r, otherwise things make no sense
	public boolean interferer(int s, int r, int i, double dist_sr, double dist_is, double dist_ir) {
		return (((dist_ir / dist_sr) < SIR_RATIO * range[i] / range[s]) && (dist_ir > range[r]) && (dist_is > range[s]));
	}

	public int[] interferenceLevel(double csFact) {
		int[] results = new int[2]; // 0: interferers, 1: links
		for (int i = 0; i < nodes; i++) {
			Node ni = adj.getNode(i);
			for (int jx = 0; jx < ni.outDeg(); jx++) {
				int j = ni.succAt(jx).dstNode().getKey();
				if (adjacent(j, i) && (i < j)) {
					double dist_ij = distance(i, j);
					if ((dist_ij <= range[i]) && (dist_ij <= range[j])) {
						results[1]++;
						for (int k = 0; k < nodes; k++) {
							if ((i != k) && (j != k)) {
								double dist_ik = distance(i, k);
								double dist_jk = distance(j, k);
								if (interferer(i, j, k, dist_ij, dist_ik / csFact, dist_jk))
									results[0]++;
								if (interferer(j, i, k, dist_ij, dist_jk / csFact, dist_ik))
									results[0]++;
							}
						}
					}
				}
			}
		}
		results[1] *= 2;
		return results;
	}

	public int bidirectionalLinks() {
		if (bidirEdges < 0)
			buildGraph(false);
		return bidirEdges;
	}

	public int unidirectionalLinks() {
		if (unidirEdges < 0)
			buildGraph(false);
		return unidirEdges;
	}

	public static int broadcastCapacity(Topology txTopo, Topology csTopo) {
		int nodes = txTopo.nodes;

		// build conflict graph

		Graph conflict = new Graph();
		for (int i = 0; i < nodes; i++) {
			Node ci = conflict.checkNode(i);
			Node ti = txTopo.adj.getNode(i);
			for (int j = 0; j < ti.outDeg(); j++) {
				Edge e1 = ti.succAt(j);
				Node tj = e1.dstNode();
				int jIdx = tj.getKey();
				Node cj = conflict.checkNode(jIdx);
				if (ci.getSucc(jIdx) == null) {
					ci.addSucc(cj, 1);
					cj.addSucc(ci, 1);
				}
				Node sj = csTopo.adj.getNode(jIdx);
				for (int k = 0; k < sj.inDeg(); k++) {
					Edge e2 = sj.predAt(k);
					Node sk = e2.srcNode();
					int kIdx = sk.getKey();
					if ((i != kIdx) && (ci.getSucc(kIdx) == null)) {
						Node ck = conflict.checkNode(kIdx);
						ci.addSucc(ck, 1);
						ck.addSucc(ci, 1);
					}
				}
			}
		}

		// assign colours

		int[] colour = new int[nodes];
		int[] use = new int[nodes]; // how often did we use a certain colour?
		for (int i = 0; i < nodes; i++)
			colour[i] = -1;
		int numColours = 0;
		for (int i = 0; i < nodes; i++) {
			Node n;
			int nextNode = -1;
			int nextNodeCol = -1;
			int nextNodeDeg = -1;
			for (int j = 0; j < nodes; j++)
				if (colour[j] < 0) {
					n = conflict.getNode(j);
					int nCol = 0;
					for (int k = 0; k < n.outDeg(); k++) {
						int kIdx = n.succAt(k).dstNode().getKey();
						if (colour[kIdx] >= 0)
							++nCol;
					}
					int nDeg = n.outDeg();

					if ((nCol > nextNodeCol) || ((nCol == nextNodeCol) && (nDeg > nextNodeDeg))) {
						nextNode = j;
						nextNodeCol = nCol;
						nextNodeDeg = nDeg;
					}
				}

			boolean[] ok = new boolean[numColours];
			for (int j = 0; j < numColours; j++)
				ok[j] = true;
			n = conflict.getNode(nextNode);
			for (int j = 0; j < n.outDeg(); j++) {
				Edge e = n.succAt(j);
				Node s = e.dstNode();
				int sIdx = s.getKey();
				if (colour[sIdx] >= 0)
					ok[colour[sIdx]] = false;
			}
			int uColour = numColours;
			for (int j = 0; j < numColours; j++)
				if ((ok[j]) && (use[j] > use[uColour]))
					uColour = j;
			if (uColour == numColours)
				++numColours;
			colour[nextNode] = uColour;
			++use[uColour];
		}
		return numColours;
	}

	public void plot(String filename, String xlabel, double x, double y) throws FileNotFoundException, IOException {
		PrintWriter gp_nodes = new PrintWriter(new FileOutputStream(filename + ".gp_nodes"));
		PrintWriter gp_conn = new PrintWriter(new FileOutputStream(filename + ".gp_conn"));
		for (int i = 0; i < nodes; i++) {
			gp_nodes.println("" + pos[i].x + " " + pos[i].y);
			for (int j = i + 1; j < nodes; j++)
				if (adjacent(i, j) && adjacent(j, i)) {
					gp_conn.println("" + pos[i].x + " " + pos[i].y);
					gp_conn.println("" + pos[j].x + " " + pos[j].y);
					gp_conn.println("");
				}
		}
		gp_nodes.close();
		gp_conn.close();
		PrintWriter gnuplot = new PrintWriter(Runtime.getRuntime().exec("gnuplot").getOutputStream());
		gnuplot.println("set term png color");
		gnuplot.println("set output \"" + filename + ".png\"");
		if (xlabel != null)
			gnuplot.println("set xlabel \"" + xlabel + "\"");
		gnuplot.println("plot [0:" + x + "][0:" + y + "] \"" + filename + ".gp_conn\" notitle w l lt 2, \"" + filename + ".gp_conn\" notitle lt 1");
		gnuplot.close();
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Topology t = new Topology(args[0]);
		for (int i = 0; i < t.nodeCount(); i++)
			for (int j = i+1; j < t.nodeCount(); j++)
				System.out.println(t.distance(i,j));
	}
}
