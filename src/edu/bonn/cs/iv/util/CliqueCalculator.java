/*
 * Created on 30.12.2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.bonn.cs.iv.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author peschlow
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CliqueCalculator {
	public static boolean[] findClique (
	    final int[] linkIndexOfFlowLinkIndex,
	    final int[] numCountedFlowLinksOfLink,
	    final BitSet[] interferes) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		FlowLinkCliqueInfo[] cliqueInfoOfFlowLinkIndex = new FlowLinkCliqueInfo[numUsedFlowLinks];

		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			int linkIndex = linkIndexOfFlowLinkIndex[flowLinkIndex];
			int degree = numCountedFlowLinksOfLink[linkIndex];
			cliqueInfoOfFlowLinkIndex[flowLinkIndex] = new FlowLinkCliqueInfo(flowLinkIndex, degree);
		}

		Arrays.sort(cliqueInfoOfFlowLinkIndex);

		boolean[] clique = new boolean[numUsedFlowLinks];
		int cliqueSize = 0;

		for (int i = 0; i < numUsedFlowLinks; i++) {
			int flowLinkIndex = cliqueInfoOfFlowLinkIndex[i].flowLinkIndex_;
			int linkIndex = linkIndexOfFlowLinkIndex[flowLinkIndex];
			boolean add = true;

			for (int j = 0; j < i; j++) {
				int flowLinkIndex2 = cliqueInfoOfFlowLinkIndex[j].flowLinkIndex_;

				if (clique[flowLinkIndex2]) {
					int linkIndex2 = linkIndexOfFlowLinkIndex[flowLinkIndex2];

					if (!interferes[linkIndex].get(linkIndex2)) {
						add = false;
						break;
					}
				}
			}

			if (add) {
				clique[flowLinkIndex] = true;
				cliqueSize++;
			}
		}

		System.out.println("Clique size = " + cliqueSize);

		return clique;
	}

	//static public boolean[] findCliqueLocal(final int[] linkIndexOfFlowLinkIndex, final int[] numCountedFlowLinksOfLink, final BitSet[] interferes, final ArrayList[] flowLinksUsingLink) {
	public static CliqueInfo findCliqueLocal (
	    final int[] linkIndexOfFlowLinkIndex,
	    final int[] numCountedFlowLinksOfLink,
	    final BitSet[] interferes,
	    final ArrayList[] flowLinksUsingLink) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;
		LinkCliqueInfo[] cliqueInfoOfLinkIndex = new LinkCliqueInfo[numUsedDirLinks];

		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			int degree = numCountedFlowLinksOfLink[linkIndex];
			cliqueInfoOfLinkIndex[linkIndex] = new LinkCliqueInfo(linkIndex, degree);
		}

		Arrays.sort(cliqueInfoOfLinkIndex);

		boolean[] bestClique = null;
		int bestCliqueSize = 0;
		int[] bestCliqueSizeOfLink = new int[numUsedDirLinks];
		boolean[] indexBlocked = new boolean[numUsedDirLinks];

		for (int linkIndexCenter = 0; linkIndexCenter < numUsedDirLinks; linkIndexCenter++) {
			if ((linkIndexCenter % 100) == 0) {
				System.out.println(linkIndexCenter + " / " + numUsedDirLinks);
			}

			int cliqueSize = 0;
			boolean[] clique = new boolean[numUsedDirLinks];

			for (int index = 0; index < numUsedDirLinks; index++) {
				if (indexBlocked[index]) {
					continue;
				}

				int linkIndex = cliqueInfoOfLinkIndex[index].linkIndex_;

				if (!interferes[linkIndexCenter].get(linkIndex)) {
					continue;
				}

				boolean add = true;

				for (int smallerIndex = 0; smallerIndex < index; smallerIndex++) {
					int linkIndex2 = cliqueInfoOfLinkIndex[smallerIndex].linkIndex_;

					if (clique[linkIndex2]) {
						if (!interferes[linkIndex].get(linkIndex2)) {
							add = false;
							break;
						}
					}
				}

				if (add) {
					clique[linkIndex] = true;
					cliqueSize += flowLinksUsingLink[linkIndex].size();

					for (int largerIndex = 0; largerIndex > index; largerIndex++) {
						int linkIndex2 = cliqueInfoOfLinkIndex[largerIndex].linkIndex_;

						if (!interferes[linkIndex].get(linkIndex2)) {
							indexBlocked[largerIndex] = true;
						}
					}
				}
			}

			for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
				if (clique[linkIndex] && (cliqueSize > bestCliqueSizeOfLink[linkIndex])) {
					bestCliqueSizeOfLink[linkIndex] = cliqueSize;
				}
			}

			if (cliqueSize > bestCliqueSize) {
				System.out.println("New max with size = " + cliqueSize);
				bestClique = clique;
				bestCliqueSize = cliqueSize;
			} else {
				if (cliqueSize == bestCliqueSize) {
					//System.out.println("Found another with " + cliqueSize);
					for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
						if (clique[linkIndex] && !bestClique[linkIndex]) {
							bestClique[linkIndex] = true;
						}
					}
				}
			}

			if (cliqueSize == numUsedFlowLinks) {
				break;
			}
		}

		System.out.println("Clique size = " + bestCliqueSize);

		/*for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
		   System.out.println("Link " + linkIndex + ": best clique size = " + bestCliqueSizeOfLink[linkIndex]);
		}*/
		/*boolean[] flowLinkClique = new boolean[numUsedFlowLinks];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			if (bestClique[linkIndex]) {
				for (int i = 0, size = flowLinksUsingLink[linkIndex].size(); i < size; i++) {
					int flowLinkIndex = ((Integer)flowLinksUsingLink[linkIndex].get(i)).intValue();
					flowLinkClique[flowLinkIndex] = true;
				}
			}
		}*/

		//return flowLinkClique;
		//return bestCliqueSizeOfLink;
		return new CliqueInfo(bestCliqueSize,bestClique,bestCliqueSizeOfLink);
	}

	static class FlowLinkCliqueInfo implements Comparable {
		public int flowLinkIndex_;
		public int numInterferers_;
		public boolean isInClique_;

		public FlowLinkCliqueInfo (
		    int flowLinkIndex,
		    int numInterferers) {
			flowLinkIndex_ = flowLinkIndex;
			numInterferers_ = numInterferers;
			isInClique_ = false;
		}

		public int compareTo (Object coloringInfo) {
			int diff = numInterferers_ - ((FlowLinkCliqueInfo)coloringInfo).numInterferers_;

			if (diff < 0) {
				return 1;
			}

			if (diff > 0) {
				return -1;
			}

			return 0;
		}
	}

	static class LinkCliqueInfo implements Comparable {
		public int linkIndex_;
		public int numInterferers_;
		public boolean isInClique_;

		public LinkCliqueInfo (
		    int linkIndex,
		    int numInterferers) {
			linkIndex_ = linkIndex;
			numInterferers_ = numInterferers;
			isInClique_ = false;
		}

		public int compareTo (Object coloringInfo) {
			int diff = numInterferers_ - ((LinkCliqueInfo)coloringInfo).numInterferers_;

			if (diff < 0) {
				return 1;
			}

			if (diff > 0) {
				return -1;
			}

			return 0;
		}
	}
}
