/*
 * Created on 08.01.2004
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
public class TwoPhaseInfo {
	private int numUsedColors_;
	private int[] numPacketsOfFlow_;
	
	public TwoPhaseInfo (int numUsedColors, int[] numPacketsOfFlow) {
		numUsedColors_ = numUsedColors;
		numPacketsOfFlow_ = numPacketsOfFlow;
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
	public int[] getNumPacketsOfFlow() {
		return numPacketsOfFlow_;
	}

}
