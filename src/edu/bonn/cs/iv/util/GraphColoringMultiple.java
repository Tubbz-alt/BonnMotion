/*
 * Created on 30.12.2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;


/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GraphColoringMultiple {
	
	private int numUsedColors_;
	private int[][] colorsOfLink_;
	private int[] intInfoOfLinkIndex_;
	
	public GraphColoringMultiple (int numUsedColors, int[][] colorsOfLink, int[] intInfoOfLinkIndex) {
		numUsedColors_ = numUsedColors;
		colorsOfLink_ = colorsOfLink;
		intInfoOfLinkIndex_ = intInfoOfLinkIndex;
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
	public int[][] getColorsOfLink() {
		return colorsOfLink_;
	}

	/**
	 * @return
	 */
	public int[] getIntInfo() {
		return intInfoOfLinkIndex_;
	}

}
