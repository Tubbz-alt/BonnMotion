package edu.bonn.cs.iv.bonnmotion;

import edu.bonn.cs.iv.util.*;

public class TopologyEvaluator {
	protected final double[] il;
	protected final double[] qu;

	protected SampleSet[] interference;
	protected SampleSet[] qRange;

	protected int samples = 0;
	protected int connected = 0;
	protected int biconnected = 0;
	protected SampleSet connRatio = new SampleSet();
	protected SampleSet biconnRatio = new SampleSet();
	protected SampleSet neighbours = new SampleSet();		
	protected SampleSet minRange = new SampleSet();
	protected SampleSet maxRange = new SampleSet();
	protected SampleSet avgRange = new SampleSet();
	protected SampleSet biLinks = new SampleSet();
	protected SampleSet uniLinks = new SampleSet();
	protected SampleSet asym_avg = new SampleSet();
	protected SampleSet asym_max = new SampleSet();
	protected SampleSet maxstretch = null;
	protected SampleSet avgstretch = null;
	protected SampleSet tstretch = null;
	protected SampleSet hstretch = null;
	protected SampleSet avgstretchGeo = null;
	protected SampleSet tstretchGeo = null;
	protected SampleSet hstretchGeo = null;
	protected Function distWeight;

	public TopologyEvaluator(double[] il, double[] qu, boolean allPairsMetrics, Function distWeight) {
		this.il = il;
		interference = new SampleSet[il.length];
		for (int i = 0; i < interference.length; i++)
			interference[i] = new SampleSet();
		this.qu = qu;
		qRange = new SampleSet[qu.length];
		for (int i = 0; i < qRange.length; i++)
			qRange[i] = new SampleSet();
		if (allPairsMetrics) {
			maxstretch = new SampleSet();
			avgstretch = new SampleSet();
			tstretch = new SampleSet();
			hstretch = new SampleSet();
			avgstretchGeo = new SampleSet();
			tstretchGeo = new SampleSet();
			hstretchGeo = new SampleSet();
			this.distWeight = distWeight;
		}
	}

	public TopologyEvaluator(double[] il, double[] qu) {
		this(il, qu, false, null);
	}

	public void updateStats(Topology topo) {
		samples++;

		int nodes = topo.nodeCount();

		int all = nodes * (nodes - 1) / 2;
		if (topo.connEdges() == all) {
			connected++;
			if (topo.biconnEdges() == all) {
				biconnected++;
				System.out.println("biconnected");
			}
			else
				System.out.println("connected");
		}
		else
			System.out.println("disconnected");

		double connectivity = (double)topo.connEdges() / (double)all;
		double biconnectivity = (double)topo.biconnEdges() / (double)all;
		connRatio.add(connectivity);
		biconnRatio.add(biconnectivity);

		neighbours.add(topo.avgNeighbours());

		System.out.println("connectivity=" + connectivity);
		System.out.println("biconnectivity=" + biconnectivity);
		System.out.println("nn=" + topo.avgNeighbours());

		for (int i = 0; i < il.length; i++) {
			int[] r = topo.interferenceLevel(il[i]);
			double tmp = (double)r[0] / (double)r[1];
			interference[i].add(tmp);
			System.out.println("interferencelevel(" + il[i] + ")=" + tmp);
		}

		SampleSet ranges = new SampleSet();
		for (int i = 0; i < nodes; i++) {
			ranges.add(topo.getRange(i));
		}
		double minR, maxR, avgR;
		minRange.add(minR = ranges.min());
		maxRange.add(maxR = ranges.max());
		avgRange.add(avgR = ranges.avg());
		System.out.println("minrange=" + minR);
		System.out.println("maxrange=" + maxR);
		System.out.println("avgrange=" + avgR);

		for (int i = 0; i < qu.length; i++) {
			double tmp = ranges.quantile(qu[i]);
			qRange[i].add(tmp);
			System.out.println("range-quantile(" + qu[i] + ")=" + tmp);
		}

		int uni = topo.unidirectionalLinks();
		int bi = topo.bidirectionalLinks();
		uniLinks.add(uni);
		biLinks.add(bi);
		System.out.println("unidirectional=" + uni);
		System.out.println("bidirectional=" + bi);
		double[] asym = topo.asymmetry();
		asym_max.add(asym[0]);
		asym_avg.add(asym[1]);
		System.out.println("asym_max=" + asym[0]);
		System.out.println("asym_avg=" + asym[1]);
		
		if (tstretch != null) {
			double[] ce = topo.allPairsStuff(distWeight);
			maxstretch.add(ce[0]);
			System.out.println("maxstretch=" + ce[0]);
			avgstretch.add(ce[1]);
			System.out.println("avgstretch=" + ce[1]);
			tstretch.add(ce[2]);
			System.out.println("txstretch=" + ce[2]);
			hstretch.add(ce[3]);
			System.out.println("hstretch=" + ce[3]);
			avgstretchGeo.add(ce[4]);
			System.out.println("avgstretch_geo=" + ce[4]);
			tstretchGeo.add(ce[5]);
			System.out.println("txstretch_geo=" + ce[5]);
			hstretchGeo.add(ce[6]);
			System.out.println("hstretch_geo=" + ce[6]);
		}
	}

	public String description() {
		String si = "";
		for (int i = 0; i < il.length; i++)
			si += " il-" + il[i] + " il-" + il[i] + "-conf95";
		String sq = "";
		for (int i = 0; i < qu.length; i++)
			sq += " q-" + qu[i] + " q-" + qu[i] + "-conf95";
		if (tstretch != null)
			sq += " maxstretch maxstretch-conf95 avgstretch avgstretch-conf95 txstretch txstretch-conf95 hstretch hstretch-conf95 avgstretch_geo avgstretch_geo-conf95 txstretch_geo txstretch_geo-conf95 hstretch_geo hstretch_geo-conf95";
		return "pconn pbiconn connratio connratio-conf95 minconnratio biconnratio biconnratio-conf95 frac_biconnratio_gt_0.9 nn nn-conf95 bilinks bilinks-conf95 unilinks unilinks-conf95 asym_max asym_max-conf95 asym_avg asym_avg-conf95" + si + " minrange minrange-conf95 avgrange avgrange-conf95 maxrange maxrange-conf95" + sq;
	}

	public String toString() {
		double pConn = (double)connected / (double)samples;
		double pBiConn = (double)biconnected / (double)samples;
		String rVal = pConn + " " + pBiConn + " " + connRatio.avg() + " " + connRatio.conf95delta() + " " + connRatio.min() + " " + biconnRatio.avg() + " " + biconnRatio.conf95delta() + " " + biconnRatio.fractionGreaterThan(0.9) + " " + neighbours.avg() + " " + neighbours.conf95delta() + " " + biLinks.avg() + " " + biLinks.conf95delta() + " " + uniLinks.avg() + " " + uniLinks.conf95delta() + " " + asym_max.avg() + " " + asym_max.conf95delta() + " " + asym_avg.avg() + " " + asym_avg.conf95delta();
		for (int i = 0; i < il.length; i++)
			rVal += " " + interference[i].avg() + " " + interference[i].conf95delta();
		rVal += " " + minRange.avg() + " " + minRange.conf95delta() + " " + avgRange.avg() + " " + avgRange.conf95delta() + " " + maxRange.avg() + " " + maxRange.conf95delta();
		for (int i = 0; i < qu.length; i++)
			rVal += " " + qRange[i].avg() + " " + qRange[i].conf95delta();
		if (tstretch != null)
			rVal += " " + maxstretch.avg() + " " + maxstretch.conf95delta() + " " + avgstretch.avg() + " " + avgstretch.conf95delta() + " " + tstretch.avg() + " " + tstretch.conf95delta() + " " + hstretch.avg() + " " + hstretch.conf95delta() + " " + avgstretchGeo.avg() + " " + avgstretchGeo.conf95delta() + " " + tstretchGeo.avg() + " " + tstretchGeo.conf95delta() + " " + hstretchGeo.avg() + " " + hstretchGeo.conf95delta();
		return rVal;
	}
}
