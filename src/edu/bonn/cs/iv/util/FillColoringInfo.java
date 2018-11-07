/*
 * Created on 08.01.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

import java.util.BitSet;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FillColoringInfo {
	private int numUsedColors_;
	private BitSet[] colorsOfLink_;
	private double[] numColorsOfLink_;
	
	public FillColoringInfo (int numUsedColors, BitSet[] colorsOfLink, double[] numColorsOfLink) {
		numUsedColors_ = numUsedColors;
		colorsOfLink_ = colorsOfLink;
		numColorsOfLink_ = numColorsOfLink;
	}
	
	/**
	 * @return
	 */
	public int getNumUsedColors() {
		return numUsedColors_;
	}

	/**
	 * @return
	 */
	public BitSet[] getColorsOfLink() {
		return colorsOfLink_;
	}

	/**
	 * @return
	 */
	public double[] getNumColorsOfLink() {
		return numColorsOfLink_;
	}

}
