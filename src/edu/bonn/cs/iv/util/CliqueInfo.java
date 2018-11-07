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
public class CliqueInfo {
	private int cliqueSize_;
	private boolean[] bestClique_;
	int[] cliqueSizeOfLink_;
	
	public CliqueInfo (int cliqueSize, boolean[] linkInClique, int[] cliqueSizeOfLink) {
		cliqueSize_ = cliqueSize;
		bestClique_ = linkInClique;
		cliqueSizeOfLink_ = cliqueSizeOfLink;
	}
	
	/**
	 * @return
	 */
	public int getCliqueSize() {
		return cliqueSize_;
	}

	/**
	 * @return
	 */
	public boolean[] getBestClique() {
		return bestClique_;
	}
	
	public int[] getCliqueSizeOfLink() {
		return cliqueSizeOfLink_;
	}

}
