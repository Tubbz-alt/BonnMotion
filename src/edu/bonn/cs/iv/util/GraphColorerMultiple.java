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
public class GraphColorerMultiple {
	public static final GraphColoringMultiple colorIt(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int numFlows,
		final int maxDegree,
		final int maxNumberOfRuns) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		FlowLinkColoringInfo[] coloringInfoOfFlowLinkIndex = new FlowLinkColoringInfo[numUsedFlowLinks];
		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			int linkIndex = linkIndexOfFlowLinkIndex[flowLinkIndex];
			int degree = numCountedFlowLinksOfLink[linkIndex];
			coloringInfoOfFlowLinkIndex[flowLinkIndex] = new FlowLinkColoringInfo(flowLinkIndex, 0, degree);
		}

		FlowLinkColoringInfo[] flowLinkColoringInfos = new FlowLinkColoringInfo[numUsedFlowLinks];

		int[] numColorsOfRun = new int[maxNumberOfRuns];
		int[] remaining = new int[maxNumberOfRuns];
		int[][] color = new int[maxNumberOfRuns][numUsedFlowLinks];
		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			for (int run = 0; run < maxNumberOfRuns; run++) {
				color[run][flowLinkIndex] = -1;
			}
		}

		final int total = maxNumberOfRuns * maxDegree;
		boolean[] colorUsedInVicinity = new boolean[total];
		int[] totalUsageOfColor = new int[total];
		ColorInfo[] sortedColorInfos = new ColorInfo[total];
		for (int i = 0; i < total; i++) {
			sortedColorInfos[i] = new ColorInfo(i);
		}

		int totalNumUsedColors = 0;
		for (int run = 0; run < maxNumberOfRuns; run++) {
			int stillOldColors = 0;
			boolean[] oldColoredFlowLink = new boolean[numUsedFlowLinks];
			for (int i = 0; i < numUsedFlowLinks; i++) {
				flowLinkColoringInfos[i] = (FlowLinkColoringInfo) coloringInfoOfFlowLinkIndex[i].copy();
			}
			for (int i = 0; i < numUsedFlowLinks; i++) {
				int bestFlowLinkIndex = 0;
				boolean foundFirstUncolored = false;
				for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
					if (!flowLinkColoringInfos[flowLinkIndex].isColored_) {
						if (!foundFirstUncolored) {
							bestFlowLinkIndex = flowLinkIndex;
							foundFirstUncolored = true;
						} else {
							if (flowLinkColoringInfos[flowLinkIndex].compareTo(flowLinkColoringInfos[bestFlowLinkIndex]) == -1) {
								bestFlowLinkIndex = flowLinkIndex;
							}
						}
					}
				}
				Arrays.fill(colorUsedInVicinity, 0, totalNumUsedColors, false);

				int linkIndex = linkIndexOfFlowLinkIndex[bestFlowLinkIndex];
				for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
					for (int flowLinkCount = 0, size = flowLinksUsingLink[linkIndex2].size(); flowLinkCount < size; flowLinkCount++) {
						int flowLinkIndex2 = ((Integer) flowLinksUsingLink[linkIndex2].get(flowLinkCount)).intValue();
						for (int r = 0; r <= run; r++) {
							if (color[r][flowLinkIndex2] >= 0) {
								colorUsedInVicinity[color[r][flowLinkIndex2]] = true;
							}
						}
						flowLinkColoringInfos[flowLinkIndex2].numColoredNeighbours_++;
					}
				}
				Arrays.sort(sortedColorInfos, 0, totalNumUsedColors);

				int mostOftenUsedColor = 0;
				boolean needNewColor = true;
				for (int j = 0; j < totalNumUsedColors; j++) {
					if (!colorUsedInVicinity[sortedColorInfos[j].colorIndex_]) {
						if (sortedColorInfos[j].runFirstTimeUsed_ < run) {
							stillOldColors++;
							oldColoredFlowLink[bestFlowLinkIndex] = true;
						}
						needNewColor = false;
						mostOftenUsedColor = j;
						break;
					}
				}
				if (needNewColor) {
					mostOftenUsedColor = totalNumUsedColors++;
					numColorsOfRun[run]++;
					sortedColorInfos[mostOftenUsedColor].runFirstTimeUsed_ = run;
				}
				color[run][bestFlowLinkIndex] = sortedColorInfos[mostOftenUsedColor].colorIndex_;
				sortedColorInfos[mostOftenUsedColor].howOftenUsed_++;
				totalUsageOfColor[mostOftenUsedColor]++;
				flowLinkColoringInfos[bestFlowLinkIndex].isColored_ = true;
			}
			remaining[run] = numUsedFlowLinks - stillOldColors;
			System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numColorsOfRun[run] + " by " + remaining[run] + ")");
			System.gc();
			if ((run > 0) && (remaining[run] == remaining[run - 1])) {
				int numGot = 0;
				boolean[] gotFlow = new boolean[numFlows];
				for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
					if (!oldColoredFlowLink[flowLinkIndex]) {
						if (!gotFlow[flowIndexOfFlowLinkIndex[flowLinkIndex]]) {
							numGot++;
							gotFlow[flowIndexOfFlowLinkIndex[flowLinkIndex]] = true;
						}
					}
				}
				System.out.println("Got " + numGot + " flows");
				return new GraphColoringMultiple(totalNumUsedColors, null, null);
			}
		}
		return new GraphColoringMultiple(totalNumUsedColors, null, null);
	}

	public static GraphColoringMultiple colorItFast(final int[] linkIndexOfFlowLinkIndex, final int[] numCountedFlowLinksOfLink, final BitSet[] interferes, final ArrayList[] flowLinksUsingLink, final int maxDegree, final int maxNumberOfRuns) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;
		final int total = maxNumberOfRuns * maxDegree;

		int[][] colorsOfLinkIndex = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorsOfLinkIndex[linkIndex] = new int[maxNumberOfRuns * flowLinksUsingLink[linkIndex].size()];
		}

		int[] nextFree = new int[numUsedDirLinks];
		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];
		int[] stillToColorOfLinkIndex = new int[numUsedDirLinks];
		boolean[] isLinkIndexColored = new boolean[numUsedDirLinks];

		int[] numColorsOfRun = new int[maxNumberOfRuns];
		int[] remaining = new int[maxNumberOfRuns];
		int[][] color = new int[maxNumberOfRuns][numUsedFlowLinks];
		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			for (int run = 0; run < maxNumberOfRuns; run++) {
				color[run][flowLinkIndex] = -1;
			}
		}

		boolean[] colorUsedInVicinity = new boolean[total];
		int[] totalUsageOfColor = new int[total];
		int[] firstTimeColorUsed = new int[total];

		int totalNumUsedColors = 0;
		for (int run = 0; run < maxNumberOfRuns; run++) {
			for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
				stillToColorOfLinkIndex[linkIndex] = flowLinksUsingLink[linkIndex].size();
			}

			int stillOldColors = 0;
			boolean[] oldColoredFlowLink = new boolean[numUsedFlowLinks];
			for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
				numColoredNeighsOfLinkIndex[linkIndex] = 0;
				isLinkIndexColored[linkIndex] = false;
			}
			for (int i = 0; i < numUsedFlowLinks; i++) {
				int bestLinkIndex = 0;
				boolean foundFirstUncolored = false;
				for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
					if (!isLinkIndexColored[linkIndex]) {
						if (!foundFirstUncolored) {
							bestLinkIndex = linkIndex;
							foundFirstUncolored = true;
						} else {
							if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							} else {
								if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
									if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
										bestLinkIndex = linkIndex;
									}
								}
							}
						}
					}
				}
				Arrays.fill(colorUsedInVicinity, 0, totalNumUsedColors, false);
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					for (int j = 0, size = colorsOfLinkIndex[linkIndex2].length; j < size; j++) {
						int colorIndex = colorsOfLinkIndex[linkIndex2][j];
						colorUsedInVicinity[colorIndex] = true;
					}
					numColoredNeighsOfLinkIndex[linkIndex2]++;
				}

				int chosenColor = 0;
				boolean needNewColor = true;
				for (int colorIndex = 0; colorIndex < totalNumUsedColors; colorIndex++) {
					if (!colorUsedInVicinity[colorIndex]) {
						if (firstTimeColorUsed[colorIndex] < run) {
							stillOldColors++;
							oldColoredFlowLink[bestLinkIndex] = true;
						}
						needNewColor = false;
						chosenColor = colorIndex;
						break;
					}
				}
				if (needNewColor) {
					chosenColor = totalNumUsedColors++;
					numColorsOfRun[run]++;
					firstTimeColorUsed[chosenColor] = run;
				}
				colorsOfLinkIndex[bestLinkIndex][nextFree[bestLinkIndex]++] = chosenColor;
				totalUsageOfColor[chosenColor]++;
				if (--stillToColorOfLinkIndex[bestLinkIndex] == 0) {
					isLinkIndexColored[bestLinkIndex] = true;
				}
			}
			remaining[run] = numUsedFlowLinks - stillOldColors;
			System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numColorsOfRun[run] + " by " + remaining[run] + " remaining)");
			System.gc();

			/*if (run > 0 && remaining[run] == remaining[run - 1]) {
			   int numGot = 0;
			   boolean[] gotFlow = new boolean[numFlows];
			   for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			           if (!oldColoredFlowLink[flowLinkIndex]) {
			                   if (!gotFlow[flowIndexOfFlowLinkIndex[flowLinkIndex]])
			                           numGot++;
			                   gotFlow[flowIndexOfFlowLinkIndex[flowLinkIndex]] = true;
			                   ;
			           }
			   }
			   System.out.println("Got " + numGot + " flows");
			   return new GraphColoringMultiple(color, numColorsOfRun, gotFlow);
			   }*/
		}
		return new GraphColoringMultiple(totalNumUsedColors, colorsOfLinkIndex, null);
	}

	public static GraphColoringMultiple colorItVeryFast(final int[] linkIndexOfFlowLinkIndex, final int[] numCountedFlowLinksOfLink, final BitSet[] interferes, final ArrayList[] flowLinksUsingLink, final int maxDegree, final int maxNumberOfRuns) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;
		final int total = maxNumberOfRuns * maxDegree;

		int[][] colorsOfLinkIndex = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorsOfLinkIndex[linkIndex] = new int[maxNumberOfRuns * flowLinksUsingLink[linkIndex].size()];
		}

		int[] nextFree = new int[numUsedDirLinks];
		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];
		int[] stillToColorOfLinkIndex = new int[numUsedDirLinks];
		boolean[] isLinkIndexColored = new boolean[numUsedDirLinks];
		int[] runOfLatestColorOfLinkIndex = new int[numUsedDirLinks];

		int[] numColorsOfRun = new int[maxNumberOfRuns];
		int[] remaining = new int[maxNumberOfRuns];
		int[][] color = new int[maxNumberOfRuns][numUsedFlowLinks];
		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			for (int run = 0; run < maxNumberOfRuns; run++) {
				color[run][flowLinkIndex] = -1;
			}
		}

		boolean[] colorUsedInVicinity = new boolean[total];
		int[] totalUsageOfColor = new int[total];
		int[] firstTimeColorUsed = new int[total];

		int totalNumUsedColors = 0;
		for (int run = 0; run < maxNumberOfRuns; run++) {
			for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
				stillToColorOfLinkIndex[linkIndex] = flowLinksUsingLink[linkIndex].size();
			}

			int stillOldColors = 0;
			boolean[] oldColoredFlowLink = new boolean[numUsedFlowLinks];
			for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
				numColoredNeighsOfLinkIndex[linkIndex] = 0;
				isLinkIndexColored[linkIndex] = false;
			}
			for (int i = 0; i < numUsedDirLinks; i++) {
				int bestLinkIndex = 0;
				boolean foundFirstUncolored = false;
				for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
					if (!isLinkIndexColored[linkIndex]) {
						if (!foundFirstUncolored) {
							bestLinkIndex = linkIndex;
							foundFirstUncolored = true;
						} else {
							if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							} else {
								if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
									if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
										bestLinkIndex = linkIndex;
									}
								}
							}
						}
					}
				}

				int numFlowLinks = flowLinksUsingLink[bestLinkIndex].size();
				Arrays.fill(colorUsedInVicinity, 0, totalNumUsedColors, false);
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					for (int j = 0, size = colorsOfLinkIndex[linkIndex2].length; j < size; j++) {
						int colorIndex = colorsOfLinkIndex[linkIndex2][j];
						colorUsedInVicinity[colorIndex] = true;
					}
					numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinks;
				}

				int startFrom = 0;
				for (int j = 0; j < numFlowLinks; j++) {
					int chosenColor = 0;
					boolean needNewColor = true;
					for (int colorIndex = startFrom; colorIndex < totalNumUsedColors; colorIndex++) {
						if (!colorUsedInVicinity[colorIndex]) {
							if (firstTimeColorUsed[colorIndex] < run) {
								stillOldColors++;
								oldColoredFlowLink[bestLinkIndex] = true;
							}
							needNewColor = false;
							chosenColor = colorIndex;
							break;
						}
					}
					if (needNewColor) {
						chosenColor = totalNumUsedColors++;
						numColorsOfRun[run]++;
						firstTimeColorUsed[chosenColor] = run;
					}
					startFrom = chosenColor + 1;
					colorsOfLinkIndex[bestLinkIndex][nextFree[bestLinkIndex]++] = chosenColor;
					runOfLatestColorOfLinkIndex[bestLinkIndex] = firstTimeColorUsed[chosenColor];
					totalUsageOfColor[chosenColor]++;
				}
				isLinkIndexColored[bestLinkIndex] = true;
			}
			remaining[run] = numUsedFlowLinks - stillOldColors;
			System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numColorsOfRun[run] + " by " + remaining[run] + " remaining)");
			System.gc();
			if (run > 0) {
				//				calc wasted resources
				int[] numPossibleFlowLinks = new int[totalNumUsedColors];
				int[] numPossibleColors = new int[maxNumberOfRuns];
				boolean[] hadColor = new boolean[totalNumUsedColors];
				for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
					Arrays.fill(colorUsedInVicinity, 0, totalNumUsedColors, false);
					for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
						for (int j = 0, size = colorsOfLinkIndex[linkIndex2].length; j < size; j++) {
							int colorIndex = colorsOfLinkIndex[linkIndex2][j];
							colorUsedInVicinity[colorIndex] = true;
						}
					}
					for (int colorIndex = 0; colorIndex < totalNumUsedColors; colorIndex++) {
						if (!colorUsedInVicinity[colorIndex]) {
							if (!hadColor[colorIndex]) {
								numPossibleColors[firstTimeColorUsed[colorIndex]]++;
								hadColor[colorIndex] = true;
							}
							numPossibleFlowLinks[colorIndex] += flowLinksUsingLink[linkIndex].size();
						}
					}
				}

				int lastRun = -1;
				for (int colorIndex = 0; colorIndex < totalNumUsedColors; colorIndex++) {
					int thisRun = firstTimeColorUsed[colorIndex];
					if (thisRun > lastRun) {
						System.out.println("Run " + thisRun + ": still possible colors = " + numPossibleColors[thisRun]);
						lastRun = thisRun;
					}

					//System.out.println("\tColor " + colorIndex + ": still possible flowlinks = " + numPossibleFlowLinks[colorIndex]);
				}

				if ((remaining[run] == numColorsOfRun[run]) || (remaining[run] == remaining[run - 1])) {
					return new GraphColoringMultiple(totalNumUsedColors, colorsOfLinkIndex, runOfLatestColorOfLinkIndex);
				}
			}
		}

		return new GraphColoringMultiple(totalNumUsedColors, colorsOfLinkIndex, runOfLatestColorOfLinkIndex);
	}

	public static FillColoringInfo colorItVeryFastFillUp(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int numFlows,
		final int maxDegree,
		final int expNumberOfRuns) {
		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;

		boolean[] flowBlocked = new boolean[numFlows];

		//final int expectedNumColors = maxDegree;
		final int expectedNumColors = expNumberOfRuns * maxDegree;

		//System.out.println("exp = " + expectedNumColors);
		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];

		//int[] howOftenColoredLinkIndex = new int[numUsedDirLinks];
		double[] howOftenColoredLinkIndexFlowLinks = new double[numUsedDirLinks];
		BitSet isOut = new BitSet(numUsedDirLinks);

		//boolean[][] colorUsedByLink = new boolean[numUsedDirLinks][expectedNumColors];
		//boolean[][] colorUsedInVicinityOfLink = new boolean[numUsedDirLinks][expectedNumColors];;
		BitSet[] colorUsedByLink = new BitSet[numUsedDirLinks];
		BitSet[] colorUsedInVicinityOfLink = new BitSet[numUsedDirLinks];

		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorUsedByLink[linkIndex] = new BitSet(expectedNumColors);
			colorUsedInVicinityOfLink[linkIndex] = new BitSet(expectedNumColors);
		}

		int[][] newColors = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			newColors[linkIndex] = new int[flowLinksUsingLink[linkIndex].size()];
		}

		ArrayList<Integer> howOftenSeenNumColors = null;
		int totalNumUsedColors = 0;
		int numRemainingFlowLinksThisRun = 0;
		//int numRemainingFlowLinksLastRun = 0;
		int numNewColorsThisRun = 0;
		//int numNewColorsLastRun = 0;
		int numColoredLinksThisRun = 0;
		int numColoredFlowLinksThisRun = 0;
		int run = 0;
		boolean stop = false;
		//int countStagnation = 0;
		int maxStagnation = 5;
		//Random rng = new Random();
		do {
			//Integer runInteger = new Integer(run);
			//numNewColorsLastRun = numNewColorsThisRun;
			numNewColorsThisRun = 0;
			//numRemainingFlowLinksLastRun = numRemainingFlowLinksThisRun;
			numRemainingFlowLinksThisRun = numUsedFlowLinks;
			numColoredLinksThisRun = 0;
			numColoredFlowLinksThisRun = 0;

			int firstNewColorIndexThisRun = totalNumUsedColors;
			BitSet coloredThisRun = (BitSet) isOut.clone();
			int numLinksStillToColor = numUsedDirLinks - isOut.cardinality();

			/*int[] perm = new int[numLinksStillToColor];
			   int count = 0;
			   for (int linkIndex = coloredThisRun.nextClearBit(0); (linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
			           perm[count++] = linkIndex;
			   }
			   for (int i = 0; i < numLinksStillToColor; i++) {
			           int where = (int) (rng.nextDouble() * numLinksStillToColor);
			           int help = perm[where];
			           perm[where] = perm[i];
			           perm[i] = help;
			   }*/
			for (int i = 0; i < numLinksStillToColor; i++) {
				int bestLinkIndex = coloredThisRun.nextClearBit(0);
				for (int linkIndex = coloredThisRun.nextClearBit(bestLinkIndex + 1);(linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
					/*int bestLinkIndex = 0;
					   for (int z = 0; z < numLinksStillToColor; z++) {
					           if (!coloredThisRun.get(perm[z]))
					                   bestLinkIndex = perm[z];
					   }
					   for (int z = 0; z < numLinksStillToColor; z++) {
					           int linkIndex = perm[z];
					           if (coloredThisRun.get(linkIndex)) continue;
					 */
					if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
						bestLinkIndex = linkIndex;
					} else {
						if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
							if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							}
						}
					}
				}

				/*for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
				   if (!isOut.get(linkIndex2)) { // cos then I got its colors already
				           //colorUsedInVicinityOfLink[bestLinkIndex].or(colorUsedByLink[linkIndex2]);
				           for (int colIndex = 0; colIndex < totalNumUsedColors; colIndex++) {
				                   colorUsedInVicinityOfLink[bestLinkIndex][colIndex] |= colorUsedByLink[linkIndex2][colIndex];
				           }
				   }
				   }*/
				int startFrom = 0;
				int numFlowLinksColored = 0;
				//int numFlowLinks = flowLinksUsingLink[bestLinkIndex].size();

				int numFlowLinks = 0;
				int size = flowLinksUsingLink[bestLinkIndex].size();
				for (int k = 0; k < size; k++) {
					if (!flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]]) {
						numFlowLinks++;
					}
				}
				if (numFlowLinks == 0) {
					isOut.set(bestLinkIndex);
				}
				for (int j = 0; j < numFlowLinks; j++) {
					int chosenColor = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(startFrom);
					if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
						// new color needed
						if (!stop) {
							chosenColor = totalNumUsedColors++;
							numNewColorsThisRun++;
						} else {
							for (int k = 0; k < flowLinksUsingLink[bestLinkIndex].size(); k++) {
								flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]] = true;
							}
							isOut.set(bestLinkIndex);
							break; // the other flow links cannot be colored too
						}
					} else {
						if (chosenColor < firstNewColorIndexThisRun) {
							numRemainingFlowLinksThisRun--;
						}
					}
					if (startFrom == 0) {
						numColoredLinksThisRun++;
					}
					newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					startFrom = chosenColor + 1;
					colorUsedByLink[bestLinkIndex].set(chosenColor);
					//colorUsedByLink[bestLinkIndex][chosenColor] = true;
					numColoredFlowLinksThisRun++;

					/*int chosenColor = 0;
					   boolean needNewColor = true;
					   for (int colorIndex = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(startFrom); colorIndex >= 0 && colorIndex < totalNumUsedColors; colorIndex = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(colorIndex + 1)) {
					   //for (int colorIndex = startFrom; colorIndex < totalNumUsedColors; colorIndex++) {
					   //        if (!colorUsedInVicinityOfLink[bestLinkIndex][colorIndex]) {
					           if (colorIndex < firstNewColorIndexThisRun) {
					                   numRemainingFlowLinksThisRun--;
					           }
					           needNewColor = false;
					           chosenColor = colorIndex;
					           break;
					   //        }
					   }
					   if (needNewColor) {
					           if (!stop) {
					                   if (startFrom == 0) {
					                           numColoredLinksThisRun++;
					                   }
					                   chosenColor = totalNumUsedColors++;
					                   newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					                   numNewColorsThisRun++;
					                   startFrom = chosenColor + 1;
					                   colorUsedByLink[bestLinkIndex].set(chosenColor);
					                   //colorUsedByLink[bestLinkIndex][chosenColor] = true;
					                   numColoredFlowLinksThisRun++;
					           } else {
					                   isOut.set(bestLinkIndex);
					                   break; // the other flow links cannot be colored too
					           }
					   } else {
					           if (startFrom == 0) {
					                   numColoredLinksThisRun++;
					           }
					           newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					           startFrom = chosenColor + 1;
					           colorUsedByLink[bestLinkIndex].set(chosenColor);
					           //colorUsedByLink[bestLinkIndex][chosenColor] = true;
					           numColoredFlowLinksThisRun++;
					   }*/
				}
				coloredThisRun.set(bestLinkIndex);
				howOftenColoredLinkIndexFlowLinks[bestLinkIndex] += numFlowLinksColored;
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					if (!isOut.get(linkIndex2)) {
						numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinksColored;
						for (int j = 0; j < numFlowLinksColored; j++) {
							//colorUsedInVicinityOfLink[linkIndex2][newColors[bestLinkIndex][j]] = true;
							colorUsedInVicinityOfLink[linkIndex2].set(newColors[bestLinkIndex][j]);
						}
					}
				}
			}
			if (run == 20) {
				stop = true;
			}
			if (stop) {
				System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (still colored " + numColoredLinksThisRun + " links)");
			} else {
				System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numNewColorsThisRun + " by " + numRemainingFlowLinksThisRun + " remaining)");
				if (run > 0) {
					if (numRemainingFlowLinksThisRun == numNewColorsThisRun) {
						//stop = true;

						/*for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
						   BitSet colorUsedByLinkNew = new BitSet(totalNumUsedColors);
						   colorUsedByLinkNew.or(colorUsedByLink[linkIndex]);
						   BitSet colorUsedInVicinityOfLinkNew = new BitSet(totalNumUsedColors);
						   colorUsedInVicinityOfLinkNew.or(colorUsedInVicinityOfLink[linkIndex]);
						   colorUsedByLink[linkIndex] = colorUsedByLinkNew;
						   colorUsedInVicinityOfLink[linkIndex] = colorUsedInVicinityOfLinkNew;
						   }
						   System.gc();*/
					}
					/*if (numRemainingFlowLinksThisRun == numRemainingFlowLinksLastRun) {
						countStagnation++;
						if (countStagnation == maxStagnation) {
							stop = true;
						}
					} else {
						countStagnation = 0;
					}*/
					/*if (numNewColorsThisRun == numNewColorsLastRun) {
						countStagnation++;
						if (countStagnation == maxStagnation) {
							stop = true;
						}
					} else {
						countStagnation = 0;
					}*/
					/*if (run == expNumberOfRuns) {
						stop = true;
					}*/
				} else {
					howOftenSeenNumColors = new ArrayList(numNewColorsThisRun * 2);
					for (int i = 0; i < numNewColorsThisRun * 2; i++) {
						howOftenSeenNumColors.add(new Integer(0));
					}
				}
				int oldValue = ((Integer) howOftenSeenNumColors.get(numNewColorsThisRun - 1)).intValue();
				if (++oldValue == maxStagnation) {
					//stop = true;
				}
				howOftenSeenNumColors.set(numNewColorsThisRun - 1, new Integer(oldValue));
			}
			run++;
		} while (numColoredFlowLinksThisRun > 0);

		//return howOftenColoredLinkIndexFlowLinks;
		return new FillColoringInfo(totalNumUsedColors, colorUsedByLink, howOftenColoredLinkIndexFlowLinks);
	}

	public static TwoPhaseInfo colorItTwoPhaseOld(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int numFlows,
		final int maxDegree,
		final int maxStagnation) {

		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;

		boolean[] flowBlocked = new boolean[numFlows];
		int[] numPackets = new int[numFlows];

		final int expectedNumColors = maxStagnation * 3 * maxDegree;

		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];
		//int[] lastColor = new int[numUsedFlowLinks];

		double[] howOftenColoredLinkIndexFlowLinks = new double[numUsedDirLinks];
		BitSet isOut = new BitSet(numUsedDirLinks);

		BitSet[] colorUsedByLink = new BitSet[numUsedDirLinks];
		BitSet[] colorUsedInVicinityOfLink = new BitSet[numUsedDirLinks];
		//BitSet[] moreThanOnce = new BitSet[numUsedDirLinks];

		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorUsedByLink[linkIndex] = new BitSet(expectedNumColors);
			colorUsedInVicinityOfLink[linkIndex] = new BitSet(expectedNumColors);
			//moreThanOnce[linkIndex] = new BitSet(expectedNumColors);
		}

		int[][] newColors = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			newColors[linkIndex] = new int[flowLinksUsingLink[linkIndex].size()];
		}

		int totalNumUsedColors = 0;
		int numRemainingFlowLinksThisRun = 0;
		//		int numRemainingFlowLinksLastRun = 0;
		int numNewColorsThisRun = Integer.MAX_VALUE;
		int numNewColorsLastRun = 0;
		int numColoredLinksThisRun = 0;
		int numColoredFlowLinksThisRun = 0;
		int run = 0;
		boolean firstPhase = true;
		int countStagnation = 0;
		//Random rng = new Random();
		do {
			numNewColorsLastRun = numNewColorsThisRun;
			numNewColorsThisRun = 0;
			//			numRemainingFlowLinksLastRun = numRemainingFlowLinksThisRun;
			numRemainingFlowLinksThisRun = numUsedFlowLinks;
			numColoredLinksThisRun = 0;
			numColoredFlowLinksThisRun = 0;
			int firstNewColorIndexThisRun = totalNumUsedColors;

			// Saturation Largest First -> chose the link to color			
			BitSet coloredThisRun = (BitSet) isOut.clone();
			int numLinksStillToColor = numUsedDirLinks - isOut.cardinality();
			/*int[] perm = new int[numLinksStillToColor];
			   int count = 0;
			   for (int linkIndex = coloredThisRun.nextClearBit(0); (linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
			           perm[count++] = linkIndex;
			   }
			   for (int i = 0; i < numLinksStillToColor; i++) {
			           int where = (int) (rng.nextDouble() * numLinksStillToColor);
			           int help = perm[where];
			           perm[where] = perm[i];
			           perm[i] = help;
			   }*/
			for (int i = 0; i < numLinksStillToColor; i++) {
				int bestLinkIndex = coloredThisRun.nextClearBit(0);
				for (int linkIndex = coloredThisRun.nextClearBit(bestLinkIndex + 1);(linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
					/*int bestLinkIndex = 0;
					   for (int z = 0; z < numLinksStillToColor; z++) {
					           if (!coloredThisRun.get(perm[z]))
					                   bestLinkIndex = perm[z];
					   }
					   for (int z = 0; z < numLinksStillToColor; z++) {
					           int linkIndex = perm[z];
					           if (coloredThisRun.get(linkIndex)) continue;
					 */
					if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
						bestLinkIndex = linkIndex;
					} else {
						if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
							if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							}
						}
					}
				}

				// how many flow links are there to color on this link
				int numFlowLinks = 0;
				int size = flowLinksUsingLink[bestLinkIndex].size();
				for (int k = 0; k < size; k++) {
					if (!flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]]) {
						numFlowLinks++;
					}
				}
				if (numFlowLinks == 0) {
					isOut.set(bestLinkIndex);
				}

				int startFromColor = 0;
				int numFlowLinksColored = 0;
				for (int j = 0; j < numFlowLinks; j++) {
					int chosenColor = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(startFromColor);
					if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
						// new color needed
						if (firstPhase) {
							chosenColor = totalNumUsedColors++;
							numNewColorsThisRun++;
						} else {
							//System.out.println("Conflict, had already " + j);
							for (int k = 0; k < flowLinksUsingLink[bestLinkIndex].size(); k++) {
								flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]] = true;
							}
							// XXX why block all? some may be colored to an end.
							isOut.set(bestLinkIndex);
							break; // the other flow links cannot be colored too
						}
					} else {
						if (chosenColor < firstNewColorIndexThisRun) {
							numRemainingFlowLinksThisRun--;
						}
					}
					if (startFromColor == 0) {
						numColoredLinksThisRun++;
					}
					newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					startFromColor = chosenColor + 1;
					colorUsedByLink[bestLinkIndex].set(chosenColor);
					numColoredFlowLinksThisRun++;
				}
				coloredThisRun.set(bestLinkIndex);
				howOftenColoredLinkIndexFlowLinks[bestLinkIndex] += numFlowLinksColored;

				// tell the neighbourhood about the new colors
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					if (!isOut.get(linkIndex2)) {
						numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinksColored;
						for (int j = 0; j < numFlowLinksColored; j++) {
							/*if (colorUsedInVicinityOfLink[linkIndex2].get(newColors[bestLinkIndex][j])) {
								moreThanOnce[linkIndex2].set(newColors[bestLinkIndex][j]);
							}*/
							colorUsedInVicinityOfLink[linkIndex2].set(newColors[bestLinkIndex][j]);
						}
					}
				}
			}
			// check whether stagnation has been reached
			if (firstPhase) {
				System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numNewColorsThisRun + " by " + numRemainingFlowLinksThisRun + " remaining)");
				if (run > 0) {
					if (numRemainingFlowLinksThisRun == numNewColorsThisRun) {
						firstPhase = false;
						for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
							numPackets[flowIndex] = run + 1;
						}
					} else {
						if (numNewColorsThisRun >= numNewColorsLastRun) {
							countStagnation++;
							if (countStagnation == maxStagnation) {
								firstPhase = false;
								for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
									numPackets[flowIndex] = run + 1;
								}
							}
						}
					}
				}
			} else {
				for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
					if (!flowBlocked[flowIndex]) {
						numPackets[flowIndex]++;
					}
				}
				System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (still colored " + numColoredLinksThisRun + " links)");
			}
			run++;
		} while (numColoredFlowLinksThisRun > 0);

		return new TwoPhaseInfo(totalNumUsedColors, numPackets);
	}

	private static int[] firstPhase(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int maxStagnation,
		final int numUsedFlowLinks,
		int[] numColoredNeighsOfLinkIndex,
		BitSet[] colorUsedByLink,
		BitSet[] colorUsedInVicinityOfLink,
		final boolean[] bestClique) {

		final int numUsedDirLinks = interferes.length;

		int[][] newColors = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			newColors[linkIndex] = new int[flowLinksUsingLink[linkIndex].size()];
		}

		int totalNumUsedColors = 0;
		int numRemainingFlowLinksThisRun = 0;
		int numNewColorsThisRun = Integer.MAX_VALUE;
		int numNewColorsLastRun = 0;
		int numColoredLinksThisRun = 0;
		int numColoredFlowLinksThisRun = 0;
		int run = 0;
		boolean firstPhase = true;
		int countStagnation = 0;

		do {
			numNewColorsLastRun = numNewColorsThisRun;
			numNewColorsThisRun = 0;
			numRemainingFlowLinksThisRun = numUsedFlowLinks;
			numColoredLinksThisRun = 0;
			numColoredFlowLinksThisRun = 0;
			int firstNewColorIndexThisRun = totalNumUsedColors;

			BitSet coloredThisRun = new BitSet(numUsedDirLinks);
			// Color the best clique
			if (bestClique != null) {
				for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
					if (bestClique[linkIndex]) {
						int numFlowLinks = flowLinksUsingLink[linkIndex].size();
						int startFromColor = 0;
						int numFlowLinksColored = 0;
						for (int j = 0; j < numFlowLinks; j++) {
							int chosenColor = colorUsedInVicinityOfLink[linkIndex].nextClearBit(startFromColor);
							if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
								// new color needed
								chosenColor = totalNumUsedColors++;
								numNewColorsThisRun++;
							} else {
								if (chosenColor < firstNewColorIndexThisRun) {
									numRemainingFlowLinksThisRun--;
								}
							}
							if (startFromColor == 0) {
								numColoredLinksThisRun++;
							}
							newColors[linkIndex][numFlowLinksColored++] = chosenColor;
							startFromColor = chosenColor + 1;
							colorUsedByLink[linkIndex].set(chosenColor);
							numColoredFlowLinksThisRun++;
						}
						coloredThisRun.set(linkIndex);

						// tell the neighbourhood about the new colors
						for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
							numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinksColored;
							for (int j = 0; j < numFlowLinksColored; j++) {
								colorUsedInVicinityOfLink[linkIndex2].set(newColors[linkIndex][j]);
							}
						}

					}
				}
			}

			int remainingLinks = numUsedDirLinks - numColoredLinksThisRun;
			// Saturation Largest First -> chose the link to color			
			for (int i = 0; i < remainingLinks; i++) {
				int bestLinkIndex = coloredThisRun.nextClearBit(0);
				for (int linkIndex = coloredThisRun.nextClearBit(bestLinkIndex + 1);(linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
					if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
						bestLinkIndex = linkIndex;
					} else {
						if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
							if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							}
						}
					}
				}

				int numFlowLinks = flowLinksUsingLink[bestLinkIndex].size();
				int startFromColor = 0;
				int numFlowLinksColored = 0;
				for (int j = 0; j < numFlowLinks; j++) {
					int chosenColor = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(startFromColor);
					if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
						// new color needed
						chosenColor = totalNumUsedColors++;
						numNewColorsThisRun++;
					} else {
						if (chosenColor < firstNewColorIndexThisRun) {
							numRemainingFlowLinksThisRun--;
						}
					}
					if (startFromColor == 0) {
						numColoredLinksThisRun++;
					}
					newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					startFromColor = chosenColor + 1;
					colorUsedByLink[bestLinkIndex].set(chosenColor);
					numColoredFlowLinksThisRun++;
				}
				coloredThisRun.set(bestLinkIndex);

				// tell the neighbourhood about the new colors
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinksColored;
					for (int j = 0; j < numFlowLinksColored; j++) {
						colorUsedInVicinityOfLink[linkIndex2].set(newColors[bestLinkIndex][j]);
					}
				}
			}
			// check whether stagnation has been reached
			//System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (new: " + numNewColorsThisRun + " by " + numRemainingFlowLinksThisRun + " remaining)");
			if (run++ > 0) {
				// Not exactly sure why PP did this -- but I believe it's safer to take it out:
//				if (numRemainingFlowLinksThisRun == numNewColorsThisRun) {
//					firstPhase = false;
//				else
					if ((numNewColorsThisRun >= numNewColorsLastRun) && (++countStagnation == maxStagnation))
						firstPhase = false;
			}
		} while (firstPhase);

		int[] info = new int[2];
		info[0] = run;
		info[1] = totalNumUsedColors;
		return info;
	}

	public static TwoPhaseInfo colorItTwoPhase(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int[] fairTxCount,
		final int maxDegree,
		final int maxStagnation,
		final boolean[] bestClique) {

		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;
		final int numFlows = fairTxCount.length;

		final int expectedNumColors = maxStagnation * maxDegree;

		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];
		//int[] lastColor = new int[numUsedFlowLinks];

		BitSet[] colorUsedByLink = new BitSet[numUsedDirLinks];
		BitSet[] colorUsedInVicinityOfLink = new BitSet[numUsedDirLinks];
		//BitSet[] moreThanOnce = new BitSet[numUsedDirLinks];

		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorUsedByLink[linkIndex] = new BitSet(expectedNumColors);
			colorUsedInVicinityOfLink[linkIndex] = new BitSet(expectedNumColors);
			//moreThanOnce[linkIndex] = new BitSet(expectedNumColors);
		}

		int[] info = firstPhase(linkIndexOfFlowLinkIndex, numCountedFlowLinksOfLink, interferes, flowLinksUsingLink, maxStagnation, numUsedFlowLinks, numColoredNeighsOfLinkIndex, colorUsedByLink, colorUsedInVicinityOfLink, bestClique);
		int run = info[0];
		System.out.println("phase1run=" + run);
		int totalNumUsedColors = info[1];

		//System.exit(0);
		int[] numPackets = new int[numFlows];
		for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
			numPackets[flowIndex] = run * fairTxCount[flowIndex];
		}
		/*boolean yes = true;
		if (yes) {
			return new TwoPhaseInfo(totalNumUsedColors, numPackets);			
		}*/

		boolean[] flowBlocked = new boolean[numFlows];
		BitSet isOut = new BitSet(numUsedDirLinks);
		int[][] newColors = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			newColors[linkIndex] = new int[flowLinksUsingLink[linkIndex].size()];
		}

		int numColoredLinksThisRun = 0;

		do {
			numColoredLinksThisRun = 0;

			// Saturation Largest First -> chose the link to color			
			BitSet coloredThisRun = (BitSet) isOut.clone();
			int numLinksStillToColor = numUsedDirLinks - isOut.cardinality();
			for (int i = 0; i < numLinksStillToColor; i++) {
				int bestLinkIndex = coloredThisRun.nextClearBit(0);
				for (int linkIndex = coloredThisRun.nextClearBit(bestLinkIndex + 1);(linkIndex >= 0) && (linkIndex < numUsedDirLinks); linkIndex = coloredThisRun.nextClearBit(linkIndex + 1)) {
					if (numColoredNeighsOfLinkIndex[linkIndex] > numColoredNeighsOfLinkIndex[bestLinkIndex]) {
						bestLinkIndex = linkIndex;
					} else {
						if (numColoredNeighsOfLinkIndex[linkIndex] == numColoredNeighsOfLinkIndex[bestLinkIndex]) {
							if (numCountedFlowLinksOfLink[linkIndex] >= numCountedFlowLinksOfLink[bestLinkIndex]) {
								bestLinkIndex = linkIndex;
							}
						}
					}
				}

				// how many flow links are there to color on this link
				int numFlowLinks = 0;
				int size = flowLinksUsingLink[bestLinkIndex].size();
				for (int k = 0; k < size; k++) {
					if (!flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]]) {
						numFlowLinks++;
					}
				}
				if (numFlowLinks == 0) {
					isOut.set(bestLinkIndex);
				}

				int startFromColor = 0;
				int numFlowLinksColored = 0;
				for (int j = 0; j < numFlowLinks; j++) {
					int chosenColor = colorUsedInVicinityOfLink[bestLinkIndex].nextClearBit(startFromColor);
					if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
						// new color needed
						//System.out.println("Conflict, had already " + j);
						for (int k = 0; k < flowLinksUsingLink[bestLinkIndex].size(); k++) {
							flowBlocked[flowIndexOfFlowLinkIndex[((Integer) flowLinksUsingLink[bestLinkIndex].get(k)).intValue()]] = true;
						}
						// XXX why block all? some may be colored to an end.
						isOut.set(bestLinkIndex);
						break; // the other flow links cannot be colored too
					}
					if (startFromColor == 0) {
						numColoredLinksThisRun++;
					}
					newColors[bestLinkIndex][numFlowLinksColored++] = chosenColor;
					startFromColor = chosenColor + 1;
					colorUsedByLink[bestLinkIndex].set(chosenColor);
				}
				coloredThisRun.set(bestLinkIndex);

				// tell the neighbourhood about the new colors
				for (int linkIndex2 = interferes[bestLinkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[bestLinkIndex].nextSetBit(linkIndex2 + 1)) {
					if (!isOut.get(linkIndex2)) {
						numColoredNeighsOfLinkIndex[linkIndex2] += numFlowLinksColored;
						for (int j = 0; j < numFlowLinksColored; j++) {
							/*if (colorUsedInVicinityOfLink[linkIndex2].get(newColors[bestLinkIndex][j])) {
								moreThanOnce[linkIndex2].set(newColors[bestLinkIndex][j]);
							}*/
							colorUsedInVicinityOfLink[linkIndex2].set(newColors[bestLinkIndex][j]);
						}
					}
				}
			}
			for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
				if (!flowBlocked[flowIndex]) {
					numPackets[flowIndex] += fairTxCount[flowIndex];
				}
			}
			//System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (still colored " + numColoredLinksThisRun + " links)");
			run++;
		} while (numColoredLinksThisRun > 0);
		System.out.println("phase2run=" + run);

		// sanity check
		/*System.out.print("Sanity check...");
		boolean sane = true;
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			for (int color = colorUsedByLink[linkIndex].nextSetBit(0); color >= 0; color = colorUsedByLink[linkIndex].nextSetBit(color + 1)) {
				for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
					if (linkIndex2 != linkIndex && colorUsedByLink[linkIndex2].get(color)) {
						System.out.println(" uh oh. Failed.");
						sane = false;
						break;
					}
				}
			}
		}
		if (sane) {
			System.out.println(" successful!");
		}*/

		return new TwoPhaseInfo(totalNumUsedColors, numPackets);
	}

	public static TwoPhaseInfo colorItTwoPhaseIS(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int[] fairTxCount,
		final int maxDegree,
		final int maxStagnation,
		final boolean[] bestClique) {

		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		//final int numUsedDirLinks = interferes.length;
		final int numFlows = fairTxCount.length;

		int run = 0;
		int totalNumUsedColors = 0;
		//int numColoredLinksThisRun = 0;
		int numRemainingFlowLinksThisRun = 0;
		int numNewColorsThisRun = 0;
		int numNewColorsLastRun = 0;
		int countStagnation = 0;
		boolean firstPhase = true;
		int numStillIn = numUsedFlowLinks;
		boolean[] overAndOut = new boolean[numUsedFlowLinks];
		ArrayList sets = new ArrayList(1000);
		boolean[] flowBlocked = new boolean[numFlows];
		int numBlockedFlows = 0;
		int maxSetIndex = 0;
		
		int[] numPackets = new int[numFlows];

		do {
			//System.out.println("run " + run);
			numNewColorsLastRun = numNewColorsThisRun;
			numNewColorsThisRun = 0;
			FlowLinkColoringInfo[] flowLinkInfos = new FlowLinkColoringInfo[numUsedFlowLinks];
			FlowLinkColoringInfo[] flowLinkInfoByIndex = new FlowLinkColoringInfo[numUsedFlowLinks];
			boolean[] flowLinkBlocked = new boolean[numUsedFlowLinks];
			for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
				if (overAndOut[flowLinkIndex]) {
					flowLinkBlocked[flowLinkIndex] = true;
				} else {
					if (flowBlocked[flowIndexOfFlowLinkIndex[flowLinkIndex]]) {
						overAndOut[flowLinkIndex] = true;
						numStillIn--;
						flowLinkBlocked[flowLinkIndex] = true;
					}					
				}
				FlowLinkColoringInfo flowLinkInfo = new FlowLinkColoringInfo(flowLinkIndex, 0, numCountedFlowLinksOfLink[linkIndexOfFlowLinkIndex[flowLinkIndex]]);
				flowLinkInfos[flowLinkIndex] = flowLinkInfo;
				flowLinkInfoByIndex[flowLinkIndex] = flowLinkInfo;
			}

			int countScheduled = 0;
			int setIndex = 0;
			int kickedOut = 0;
			//System.out.println("Number of sets = " + sets.size());
			while ((countScheduled < numStillIn) && (firstPhase || setIndex <= maxSetIndex)) {
				//Arrays.sort(flowLinkInfos);
				int thisIS = 0;
				ArrayList thisSlot = null;
				boolean kickItOut = false;
				if (setIndex == 0) {
					kickItOut = true;
				}
				if (sets.size() <= setIndex || sets.get(setIndex) == null) {
					thisSlot = new ArrayList(20);
					sets.add(thisSlot);
					totalNumUsedColors++;
					numNewColorsThisRun++;
					maxSetIndex = setIndex;
				} else {
					thisSlot = (ArrayList) sets.get(setIndex);
				}
				//System.out.println("Size of IS " + setCounter + " = " + thisSlot.size());
				for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
					if (!flowLinkBlocked[flowLinkIndex]) {
						boolean canBeScheduled = true;
						for (int i = 0, size = thisSlot.size(); i < size; i++) {
							int index = ((FlowLinkColoringInfo) thisSlot.get(i)).flowLinkIndex_;
							if (interferes[linkIndexOfFlowLinkIndex[flowLinkIndex]].get(linkIndexOfFlowLinkIndex[index])) {
								canBeScheduled = false;
								break;
							}
						}
						if (canBeScheduled) {
							FlowLinkColoringInfo flcInfo = flowLinkInfoByIndex[flowLinkIndex];
							thisSlot.add(flcInfo);
							flowLinkBlocked[flowLinkIndex] = true;
							for (int linkIndex = interferes[linkIndexOfFlowLinkIndex[flowLinkIndex]].nextSetBit(0); linkIndex >= 0; linkIndex = interferes[linkIndexOfFlowLinkIndex[flowLinkIndex]].nextSetBit(linkIndex + 1)) {
								for (int i = 0, size = flowLinksUsingLink[linkIndex].size(); i < size; i++) {
									int flowLinkIndex2 = ((Integer) flowLinksUsingLink[linkIndex].get(i)).intValue();
									FlowLinkColoringInfo flcInfo2 = flowLinkInfoByIndex[flowLinkIndex2];
									flcInfo2.numInterferers_--;
								}
							}
							flcInfo.numInterferers_ = 0;
							thisIS++;
							countScheduled++;
							kickItOut = false;
						}
					}
				}
				//System.out.println("Size of IS " + setCounter + " (2) = " + thisSlot.size());
				if (kickItOut) {
					sets.remove(setIndex);
					kickedOut++;
					maxSetIndex--;
				} else {
					setIndex++;
				}
			}
			//System.out.println("Kicked " + kickedOut + " out.");
			for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
				if (!flowLinkBlocked[flowLinkIndex]) {
					int flowIndex = flowIndexOfFlowLinkIndex[flowLinkIndex];
					if (!flowBlocked[flowIndex]) {
						flowBlocked[flowIndex] = true;
						numBlockedFlows++;
					}
				}
			}

			if (firstPhase && run > 0) {
				if (numRemainingFlowLinksThisRun == numNewColorsThisRun) {
					firstPhase = false;
					System.out.println("Switch!");
					for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
						numPackets[flowIndex] = (run + 1) * fairTxCount[flowIndex];
					}
				} else {
					if (numNewColorsThisRun >= numNewColorsLastRun) {
						countStagnation++;
						if (countStagnation == maxStagnation) {
							firstPhase = false;
							System.out.println("Switch!");
							for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
								numPackets[flowIndex] = (run + 1) * fairTxCount[flowIndex];
							}
						}
					}
				}
			} else {
				for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
					if (!flowBlocked[flowIndex]) {
						numPackets[flowIndex]++;
					}
				}
			}
			run++;
			//System.out.println("New colors = " + numNewColorsThisRun + ", flows blocked = " + numBlockedFlows);
			//if (run == 1) firstPhase = false;
		}
		while (numBlockedFlows < numFlows);

		//System.out.println("Total num used colors = " + totalNumUsedColors);

		return new TwoPhaseInfo(totalNumUsedColors, numPackets);
	}

	public static TwoPhaseInfo colorItTwoPhaseFlowWise(
		final int[] linkIndexOfFlowLinkIndex,
		final int[] numCountedFlowLinksOfLink,
		final BitSet[] interferes,
		final ArrayList[] flowLinksUsingLink,
		final int[] flowIndexOfFlowLinkIndex,
		final int[] fairTxCount,
		final int maxDegree,
		final int maxStagnation,
		final int[] hopCountOfFlow) {

		final int numUsedFlowLinks = linkIndexOfFlowLinkIndex.length;
		final int numUsedDirLinks = interferes.length;
		final int numFlows = fairTxCount.length;

		final int expectedNumColors = maxStagnation * maxDegree;

		int[] numColoredNeighsOfLinkIndex = new int[numUsedDirLinks];

		BitSet[] colorUsedByLink = new BitSet[numUsedDirLinks];
		BitSet[] colorUsedInVicinityOfLink = new BitSet[numUsedDirLinks];

		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			colorUsedByLink[linkIndex] = new BitSet(expectedNumColors);
			colorUsedInVicinityOfLink[linkIndex] = new BitSet(expectedNumColors);
		}

		int[] info = firstPhase(linkIndexOfFlowLinkIndex, numCountedFlowLinksOfLink, interferes, flowLinksUsingLink, maxStagnation, numUsedFlowLinks, numColoredNeighsOfLinkIndex, colorUsedByLink, colorUsedInVicinityOfLink, null);
		int run = info[0];
		int totalNumUsedColors = info[1];

		//System.exit(0);
		int[] numPackets = new int[numFlows];
		for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
			numPackets[flowIndex] = run * fairTxCount[flowIndex];
		}
		int[][] newColors = new int[numUsedDirLinks][];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			newColors[linkIndex] = new int[flowLinksUsingLink[linkIndex].size()];
		}

		int[][] linkIndicesOfFlow = new int[numFlows][];

		for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
			linkIndicesOfFlow[flowIndex] = new int[hopCountOfFlow[flowIndex]];
		}

		int[] flowCounter = new int[numFlows];
		boolean[][] hadThisFlowOnLink = new boolean[numFlows][numUsedDirLinks];
		for (int flowLinkIndex = 0; flowLinkIndex < numUsedFlowLinks; flowLinkIndex++) {
			int flowIndex = flowIndexOfFlowLinkIndex[flowLinkIndex];
			int linkIndex = linkIndexOfFlowLinkIndex[flowLinkIndex];
			if (!hadThisFlowOnLink[flowIndex][linkIndex]) {
				/*System.out.println(
					"linkIndicesOfFlow.length = " + linkIndicesOfFlow.length +
					"flowIndex = " + flowIndex +
				"linkIndicesOfFlow[flowIndex].length = " + linkIndicesOfFlow[flowIndex].length +
				"flowCounter[flowIndex = " + flowCounter[flowIndex]);
				System.out.println("Hop count = " + hopCountOfFlow[flowIndex]);*/
				linkIndicesOfFlow[flowIndex][flowCounter[flowIndex]++] = linkIndex;
				hadThisFlowOnLink[flowIndex][linkIndex] = true;
			}
		}

		boolean[] flowBlocked = new boolean[numFlows];
		boolean[] linkBlocked = new boolean[numUsedDirLinks];
		/*int[] flowLinksLeft = new int[numUsedDirLinks];
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			flowLinksLeft[linkIndex] = flowLinksUsingLink[linkIndex].size();
		}*/
		int numFlowsStillToColor = numFlows;

		int numColoredFlowPacketsThisRun = 0;

		int[] unblockedTxCount = new int[numFlows];
		for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
			unblockedTxCount[flowIndex] = fairTxCount[flowIndex];
		}

		try {
			do {
				int[] thisRunTxCount = new int[numFlows];
				numColoredFlowPacketsThisRun = 0;
				int hadFairFlows = 0;
				int smallRun = 0;
				boolean[] flowColoredThisRun = new boolean[numFlows];
				int numUnblockedFlows = numFlowsStillToColor;

				//while (hadFairFlows < numFlows) {
				while (hadFairFlows < numUnblockedFlows) {
					//System.out.println(smallRun);

					int[] flowColoredNeighSums = new int[numFlows];
					int[] flowDegreeSums = new int[numFlows];

					for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
						if (!linkBlocked[linkIndex]) {
							int size = flowLinksUsingLink[linkIndex].size();
							for (int i = 0; i < size; i++) {
								int flowLinkIndex = ((Integer) flowLinksUsingLink[linkIndex].get(i)).intValue();
								int flowIndex = flowIndexOfFlowLinkIndex[flowLinkIndex];
								flowColoredNeighSums[flowIndex] += numColoredNeighsOfLinkIndex[linkIndex];
								flowDegreeSums[flowIndex] += numCountedFlowLinksOfLink[linkIndex];
							}
						}
					}
					for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
						//System.out.println("test " + flowIndex + " HC = " + hopCountOfFlow[flowIndex]);
						flowColoredNeighSums[flowIndex] /= (hopCountOfFlow[flowIndex] * fairTxCount[flowIndex]);
						flowDegreeSums[flowIndex] /= (hopCountOfFlow[flowIndex] * fairTxCount[flowIndex]);
					}

					//int howMany = numFlowsStillToColor;
					int howMany = numUnblockedFlows - hadFairFlows;
					for (int i = 0; i < howMany; i++) {
						int bestFlowIndex = -1;
						for (int flowIndex = 0; flowIndex < numFlows; flowIndex++) {
							if (!flowBlocked[flowIndex] && !flowColoredThisRun[flowIndex]) {
								if (bestFlowIndex == -1 || flowColoredNeighSums[flowIndex] > flowColoredNeighSums[bestFlowIndex]) {
									bestFlowIndex = flowIndex;
								} else {
									if (flowColoredNeighSums[flowIndex] == flowColoredNeighSums[bestFlowIndex] && flowDegreeSums[flowIndex] > flowDegreeSums[bestFlowIndex]) {
										bestFlowIndex = flowIndex;
									}
								}
							}
						}
						// test if it can be colored
						int[] colors = new int[hopCountOfFlow[bestFlowIndex]];
						int startColor = 0;
						boolean coloredAll = true;
						for (int j = 0; j < hopCountOfFlow[bestFlowIndex]; j++) {
							int linkIndex = linkIndicesOfFlow[bestFlowIndex][j];
							int chosenColor = colorUsedInVicinityOfLink[linkIndex].nextClearBit(startColor);
							if ((chosenColor == -1) || (chosenColor == totalNumUsedColors)) {
								// new color needed
								coloredAll = false;
								break;
							} else {
								boolean ok = true;
								for (int k = 0; k < hopCountOfFlow[bestFlowIndex]; k++) {
									if (colors[k] == chosenColor && interferes[linkIndex].get(linkIndicesOfFlow[bestFlowIndex][k])) {
										ok = false;
										break;
									}
								}
								if (ok) {
									colors[j] = chosenColor;
								} else {
									startColor = chosenColor + 1;
									j--;
								}
							}
						}
						if (coloredAll) {
							// tell the neighbourhood about the new colors
							for (int j = 0; j < hopCountOfFlow[bestFlowIndex]; j++) {
								int linkIndex = linkIndicesOfFlow[bestFlowIndex][j];
								int color = colors[j];
								colorUsedByLink[linkIndex].set(color);
								for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
									if (!linkBlocked[linkIndex2]) {
										numColoredNeighsOfLinkIndex[linkIndex2]++;
										colorUsedInVicinityOfLink[linkIndex2].set(color);
									}
								}
							}
							numPackets[bestFlowIndex]++;
							thisRunTxCount[bestFlowIndex]++;
							numColoredFlowPacketsThisRun++;
							if (thisRunTxCount[bestFlowIndex] == unblockedTxCount[bestFlowIndex]) {
								hadFairFlows++;
								flowColoredThisRun[bestFlowIndex] = true;
							}
						} else {
							hadFairFlows++;
							unblockedTxCount[bestFlowIndex] = thisRunTxCount[bestFlowIndex];
							if (thisRunTxCount[bestFlowIndex] == 0) {
								numUnblockedFlows--;
								flowBlocked[bestFlowIndex] = true;
							}
							/*for (int k = 0; k < hopCountOfFlow[bestFlowIndex]; k++) {
								int linkIndex2 = linkIndicesOfFlow[bestFlowIndex][k];
								flowLinksLeft[linkIndex2] -= (unblockedTxCount[bestFlowIndex] - thisRunTxCount[bestFlowIndex]);
							}*/
						}
					}
					smallRun++;
				}
				//System.out.println("After run " + run + ": total num colors = " + totalNumUsedColors + " (still colored " + numColoredFlowPacketsThisRun + " flow packets)");
				run++;
				numFlowsStillToColor = numUnblockedFlows;
			}
			while (numFlowsStillToColor > 0);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}

		// sanity check
		/*System.out.print("Sanity check...");
		boolean sane = true;
		for (int linkIndex = 0; linkIndex < numUsedDirLinks; linkIndex++) {
			for (int color = colorUsedByLink[linkIndex].nextSetBit(0); color >= 0; color = colorUsedByLink[linkIndex].nextSetBit(color + 1)) {
				for (int linkIndex2 = interferes[linkIndex].nextSetBit(0); linkIndex2 >= 0; linkIndex2 = interferes[linkIndex].nextSetBit(linkIndex2 + 1)) {
					if (linkIndex2 != linkIndex && colorUsedByLink[linkIndex2].get(color)) {
						System.out.println(" uh oh. Failed.");
						sane = false;
						break;
					}
				}
			}
		}
		if (sane) {
			System.out.println(" successful!");
		}*/

		return new TwoPhaseInfo(totalNumUsedColors, numPackets);
	}

	static class FlowLinkColoringInfo implements Comparable {
		public int flowLinkIndex_;
		public int numColoredNeighbours_;
		public int numInterferers_;
		public boolean isColored_;

		public FlowLinkColoringInfo(int linkIndex, int numColouredNeighs, int numInterferers) {
			flowLinkIndex_ = linkIndex;
			numColoredNeighbours_ = numColouredNeighs;
			numInterferers_ = numInterferers;
			isColored_ = false;
		}

		public int compareTo(Object coloringInfo) {
			int diff = numColoredNeighbours_ - ((FlowLinkColoringInfo) coloringInfo).numColoredNeighbours_;
			if (diff < 0) {
				return 1;
			}
			if (diff > 0) {
				return -1;
			}
			diff = numInterferers_ - ((FlowLinkColoringInfo) coloringInfo).numInterferers_;
			if (diff < 0) {
				return 1;
			}
			if (diff > 0) {
				return -1;
			}
			return 0;
		}

		public FlowLinkColoringInfo copy() {
			return new FlowLinkColoringInfo(flowLinkIndex_, numColoredNeighbours_, numInterferers_);
		}
	}

	static class ColorInfo implements Comparable {
		int colorIndex_;
		int howOftenUsed_;
		int runFirstTimeUsed_;

		public ColorInfo(int colIndex) {
			colorIndex_ = colIndex;
			howOftenUsed_ = 0;
			runFirstTimeUsed_ = Integer.MAX_VALUE;
		}

		public int compareTo(Object colInfo) {
			int diff = runFirstTimeUsed_ - ((ColorInfo) colInfo).runFirstTimeUsed_;
			if (diff > 0) {
				return 1;
			}
			if (diff < 0) {
				return -1;
			}
			diff = howOftenUsed_ - ((ColorInfo) colInfo).howOftenUsed_;
			if (diff > 0) {
				return -1;
			}
			if (diff < 0) {
				return 1;
			}
			diff = colorIndex_ - ((ColorInfo) colInfo).colorIndex_;
			if (diff > 0) {
				return 1;
			}
			if (diff < 0) {
				return -1;
			}
			return 0;
		}
	}
}
