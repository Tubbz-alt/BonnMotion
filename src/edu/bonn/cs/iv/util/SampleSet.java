package edu.bonn.cs.iv.util;

import java.util.Vector;

public class SampleSet {
	protected Vector<Double> s = new Vector<Double>();
	protected double sum = 0.0;
	protected double min = Double.MAX_VALUE;
	protected double max = Double.MIN_VALUE;

	protected int getPos(double val) {
		int idxLo = 0, idxHi = s.size();
		while (idxLo < idxHi) {
			int idxTest = (idxLo + idxHi) / 2;
			double tst = ((Double)s.elementAt(idxTest)).doubleValue();
			if (val > tst)
				idxLo = idxTest + 1;
			else //if (val < tst)
				idxHi = idxTest - 1;
		}
		return idxLo;
	}

	public void add(double val) {
		s.insertElementAt(new Double(val), getPos(val));
		sum += val;
		if (val < min)
			min = val;
		if (val > max)
			max = val;
	}

	public int size() {
		return s.size();
	}

	public double avg() {
		double a = sum / (double)s.size();
		if (a < min)
			a = min;
		else if (a > max)
			a = max;
		return a;
	}

	public double min() {
		return min;
	}
	
	public double max() {
		return max;
	}

	public double conf95delta() {
		double avg = avg();
		double st = 0.0;
		for (int i = 0; i < s.size(); i++) {
			double tmp = avg - ((Double)s.elementAt(i)).doubleValue();
			st += tmp * tmp;
		}
		st = Math.sqrt(st / (double)(s.size() - 1));
		return 1.96 * st / Math.sqrt((double)s.size());
	}
	
	public double quantile(double q) {
		return ((Double)s.elementAt((int)(q * (double)(s.size() - 1)))).doubleValue();
	}
	
	public double fractionGreaterThan(double val) {
		return (double)(s.size() - getPos(val)) / (double)s.size();
	}
}
