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

package edu.bonn.cs.iv.bonnmotion.models;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Scenario;
import edu.bonn.cs.iv.bonnmotion.ScenarioLinkException;
import edu.bonn.cs.iv.bonnmotion.Waypoint;

/** Manhattan Grid mobility scenario. */

public class ManhattanGrid extends Scenario {

	private static final String MODEL_NAME = "ManhattanGrid";

	/** Number of blocks on x-axis. */
	protected int xblocks = 10;
	/** Number of blocks on y-axis. */
	protected int yblocks = 10;

	/** Distance interval in which to possibly update the mobile's speed [m]. */
	protected double updateDist = 5.0;
	/** Probability for the mobile to turn at a crossing. */
	protected double turnProb = 0.5;
	/** Probability for the mobile to change its speed (every updateDist m). */
	protected double speedChangeProb = 0.2;
	/** Mobile's mean speed [m/s]. */
	protected double meanSpeed = 1.0;
	/** Mobile's minimum speed [m/s]. */
	protected double minSpeed = 0.5;
	/** Standard deviation of normally distributed random speed [m/s]. */
	protected double speedStdDev = 0.2;
	/** Probability for the mobile to pause (every updateDist m), given it does not change it's speed. */
	protected double pauseProb = 0.0;
	/** Maximum pause time [s]. */
	protected double maxPause = 120.0;

	/** Size of a block on x-axis, calculated from xblocks. */
	protected double xdim;
	/** Size of a block on y-axis, calculated from yblocks. */
	protected double ydim;

	public ManhattanGrid(
		int nodes,
		double x,
		double y,
		double duration,
		double ignore,
		long randomSeed,
		int xblocks,
		int yblocks,
		double updateDist,
		double turnProb,
		double speedChangeProb,
		double meanSpeed,
		double minSpeed,
		double speedStdDev,
		double pauseProb,
		double maxPause) {
		super(nodes, x, y, duration, ignore, randomSeed);
		this.xblocks = xblocks;
		this.yblocks = yblocks;
		this.updateDist = updateDist;
		this.turnProb = turnProb;
		this.speedChangeProb = speedChangeProb;
		this.meanSpeed = meanSpeed;
		this.minSpeed = minSpeed;
		this.speedStdDev = speedStdDev;
		this.pauseProb = pauseProb;
		this.maxPause = maxPause;
		generate();
	}

	public ManhattanGrid(String args[]) {
		go(args);
	}

	public ManhattanGrid(String args[], Scenario _pre, Integer _transitionMode) {
		// we've got a predecessor, so a transtion is needed
		super( args, _pre, _transitionMode  );
		go(args);
	}

	public void go(String[] args) {
		super.go(args);
		generate();
	}

	public void generate() {
		preGeneration();

		xdim = x / (double) xblocks;
		ydim = y / (double) yblocks;

		pauseProb += speedChangeProb;

		double init_xh = x * (double)(xblocks+1);
		double init_xr = init_xh / (init_xh + (y * (double)(yblocks+1)));

		for (int i = 0; i < node.length; i++) {
			//			System.out.println("node " + (i + 1) + "/" + node.length);
			node[i] = new MobileNode();

			double t = 0.0, st = 0.0;
			Position src = null;
			int dir = 0; // 0=up, 1=down, 2=right, 3=left
			double griddist;
			if (isTransition) {
				griddist = ydim; // FIX THIS!!
				try {
					Waypoint lastW = transition(predecessorScenario, transitionMode, i);
					src = lastW.pos;
					st = t = lastW.time;
				
					if ( src.y >= y ) // Sonst stiehlt sich der Knoten vielleicht aus dem Spielfeld
						dir = 1;

				} catch (ScenarioLinkException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} else {
				//				System.out.println("add waypoint (1) " + src + " at 0.0");
//				src = new Position(0.0, 0.0);
				if (randomNextDouble() < init_xr) { // move along x-axis
					src = new Position(randomNextDouble() * x, (double)((int)(randomNextDouble() * (double)(yblocks+1))) * ydim);
					dir = (int)(randomNextDouble() * 2.) + 2;
					griddist = src.x - (double)((int)(src.x / xdim) * xdim);
					if (dir == 2)
						griddist = xdim - griddist;
				} else { // move along y-axis
					src = new Position((double)((int)(randomNextDouble() * (double)(xblocks+1))) * xdim, randomNextDouble() * y);
					dir = (int)(randomNextDouble() * 2.);
					griddist = src.y - (double)((int)(src.y / ydim) * ydim);
					if (dir == 0)
						griddist = ydim - griddist;
				}
				if (!node[i].add(0.0, src)) {
					System.out.println(MODEL_NAME + ".<init>: error while adding node movement (1)");
					System.exit(0);
				}
			}
			
			Position pos = src;
//			double griddist = ydim;
			double speed = meanSpeed;
			double dist = updateDist;
			while (t < duration) {
				Position dst = getNewPos(pos, dist, dir);
//				System.out.println("node=" + i + " t=" + t + " pos=" + pos + " dir=" + dir + " dist=" + dist + " griddist=" + griddist + " dst=" + dst);
				boolean exactHit = false;
				if (outOfBounds(dst) || (exactHit = mustTurn(dst, dir)) || ((griddist <= dist) && (randomNextDouble() < turnProb))) { // turn
//					System.out.println("turn outOfBound=" + outOfBounds(dst) + " exactHit=" + exactHit);
					double mdist;
					if (exactHit) { // Actually, this shouldn't happen anymore, because of the modified initialisation. But anyway, now it should *really* work.
						mdist = dist;
						dist = updateDist;
					} else {
						mdist = griddist;
						dist -= mdist;
						if (dist == 0.)
							dist = updateDist;
					}
					t += mdist / speed;
//					System.out.println("dst=getNewPos("+pos+", "+mdist+", "+dir+")");
					dst = alignPos(getNewPos(pos, mdist, dir));
					if (!src.equals(dst)) {
						// this is of concern when xdim or ydim are multiple of updateDist
//						System.out.println("add waypoint (2) " + dst + " at " + t);
						if (outOfBounds(dst)) {
							System.out.println(MODEL_NAME + ".<init>: out of bounds (2)");
							System.exit(0);
						}
						if (!node[i].add(t, dst))
							if (!nodeAddErrorHandler(i, t, dst)) {
								System.out.println(MODEL_NAME + ".<init>: error while adding node movement (2)");
								System.exit(0);
							}
						src = dst;
					}
					pos = dst;
					st = t;
					if (dir < 2)
						if (pos.x > 0.0)
							if (pos.x < x)
								dir = (int) (randomNextDouble() * 2) + 2;
							else
								dir = 3;
						else
							dir = 2;
					else if (pos.y > 0.0)
						if (pos.y < y)
							dir = (int) (randomNextDouble() * 2);
						else
							dir = 1;
					else
						dir = 0;
					if (dir < 2)
						griddist = ydim;
					else
						griddist = xdim;
//					System.out.println("Newdir: " + dir);
				} else {
					t += dist / speed;
					pos = dst;
					griddist -= dist;
					dist = updateDist;
					if (griddist < 0.0)
						if (dir < 2)
							griddist += ydim;
						else
							griddist += xdim;
					double rnd = randomNextDouble();
					if (rnd < pauseProb) {
						if (!src.equals(dst)) {
							// this is of concern when xdim or ydim are multiple of updateDist
//							System.out.println("add waypoint (3) " + dst + " at " + t);
							if (outOfBounds(dst)) {
								System.out.println(MODEL_NAME + ".<init>: out of bounds (3)");
								System.exit(0);
							}
							if (!node[i].add(t, dst))
								if (!nodeAddErrorHandler(i, t, dst)) {
									System.out.println(MODEL_NAME + ".<init>: error while adding node movement (3)");
									System.exit(0);
								}
							src = dst;
						}
						if (rnd < speedChangeProb)
							st = t;
						else {
							st = t += randomNextDouble() * maxPause;
//							System.out.println("add waypoint (5) " + dst + " at " + t);
							if (outOfBounds(dst)) {
								System.out.println(MODEL_NAME + ".<init>: out of bounds (5)");
								System.exit(0);
							}
							if (!node[i].add(t, dst))
								if (!nodeAddErrorHandler(i, t, dst)) {
									System.out.println(MODEL_NAME + ".<init>: error while adding node movement (5)");
									System.exit(0);
								}
						}
						speed = (randomNextGaussian() * speedStdDev) + meanSpeed;
						if (speed < minSpeed)
							speed = minSpeed;
						//						System.out.println("update speed to " + speed);
					}
				}
			}
			if (st < duration) {
				Position dst = getNewPos(src, src.distance(pos) * (duration - st) / (t - st), dir);
//				System.out.println("add waypoint (4) " + dst + " at " + duration);
				if (outOfBounds(dst)) {
					System.out.println(MODEL_NAME + ".<init>: out of bounds (4)");
					System.exit(0);
				}
				if (!node[i].add(duration, dst)) {
					System.out.println(MODEL_NAME + ".<init>: error while adding node movement (4)");
					System.exit(0);
				}
			}
		}
		
		postGeneration();
	}

	protected boolean parseArg(String key, String value) {
		if (key.equals("xblocks")) {
			xblocks = Integer.parseInt(value);
			return true;
		} else if (key.equals("yblocks")) {
			yblocks = Integer.parseInt(value);
			return true;
		} else if (key.equals("updateDist")) {
			updateDist = Double.parseDouble(value);
			return true;
		} else if (key.equals("turnProb")) {
			turnProb = Double.parseDouble(value);
			return true;
		} else if (key.equals("speedChangeProb")) {
			speedChangeProb = Double.parseDouble(value);
			return true;
		} else if (key.equals("minSpeed")) {
			minSpeed = Double.parseDouble(value);
			return true;
		} else if (key.equals("meanSpeed")) {
			meanSpeed = Double.parseDouble(value);
			return true;
		} else if (key.equals("speedStdDev")) {
			speedStdDev = Double.parseDouble(value);
			return true;
		} else if (key.equals("pauseProb")) {
			pauseProb = Double.parseDouble(value);
			return true;
		} else if (key.equals("maxPause")) {
			maxPause = Double.parseDouble(value);
			return true;
		} else if (key.equals("ignore")) {
			ignore = Double.parseDouble(value);
			return true;
		} else if (key.equals("randomSeed")) {
			randomSeed = Long.parseLong(value);
			return true;
		} else
			return super.parseArg(key, value);
	}

	public boolean outOfBounds(Position pos) {
		return ((pos.x < 0.0) || (pos.y < 0.0) || (pos.x > x) || (pos.y > y));
	}

	protected boolean mustTurn(Position pos, int dir) {
		boolean r = (((dir == 0) && (pos.y == y)) || ((dir == 1) && (pos.y == 0.)) || ((dir == 2) && (pos.x == x)) || ((dir == 3) && (pos.x == 0.)));
//		System.out.println("mustTurn(" + pos + ", " + dir + ")=" + r);
		return r;
	}

	public Position alignPos(Position pos) {
		Position r = new Position((double) ((int) (pos.x / xdim + 0.5)) * xdim, (double) ((int) (pos.y / ydim + 0.5)) * ydim);
//		System.out.println("alignPos(" + pos + ")=" + r);
		return r;
	}

	public Position getNewPos(Position src, double dist, int dir) {
		switch (dir) {
			case 0 : // up
				return new Position(src.x, src.y + dist);
			case 1 : // down
				return new Position(src.x, src.y - dist);
			case 2 : // right
				return new Position(src.x + dist, src.y);
			case 3 : // left
				return new Position(src.x - dist, src.y);
			default :
				return null;
		}
	}

	public boolean nodeAddErrorHandler(int i, double t, Position dst) {
		Waypoint last = node[i].lastElement();
		double distance = Math.abs(dst.x - last.pos.x + dst.y - last.pos.y);
		if ((t == last.time) && (distance < 0.1)) {
			node[i].removeLastElement();
			if (node[i].add(t, dst))
				return true;
		}
		System.out.println(MODEL_NAME + ".<init>: error while adding node movement");
		return false;
	}

	public void write(String _name) throws FileNotFoundException, IOException {
		String[] p = new String[10];

		p[0] = "xblocks=" + xblocks;
		p[1] = "yblocks=" + yblocks;
		p[2] = "updateDist=" + updateDist;
		p[3] = "turnProb=" + turnProb;
		p[4] = "speedChangeProb=" + speedChangeProb;
		p[5] = "minSpeed=" + minSpeed;
		p[6] = "meanSpeed=" + meanSpeed;
		p[7] = "speedStdDev=" + speedStdDev;
		// hier muss speedChangeProb abgezogen werden, da es in go
		// nach parseArgs aufaddiert wird.
		p[8] = "pauseProb=" + (pauseProb - speedChangeProb);
		p[9] = "maxPause=" + maxPause;
		super.write(_name, p);
	}

	protected boolean parseArg(char key, String val) {
		switch (key) {
			case 'c' :
				speedChangeProb = Double.parseDouble(val);
				return true;
			case 'e' :
				minSpeed = Double.parseDouble(val);
				return true;
			case 'o' :
				maxPause = Double.parseDouble(val);
				return true;
			case 'p' :
				pauseProb = Double.parseDouble(val);
				return true;
			case 'q' :
				updateDist = Double.parseDouble(val);
				return true;
			case 'm' :
				meanSpeed = Double.parseDouble(val);
				return true;
			case 's' :
				speedStdDev = Double.parseDouble(val);
				return true;
			case 't' :
				turnProb = Double.parseDouble(val);
				return true;
			case 'u' :
				xblocks = Integer.parseInt(val);
				return true;
			case 'v' :
				yblocks = Integer.parseInt(val);
				return true;
			default :
				return super.parseArg(key, val);
		}
	}

	public static void printHelp() {
		Scenario.printHelp();
		System.out.println(MODEL_NAME + ":");
		System.out.println("\t-c <speed change probability>");
		System.out.println("\t-e <min. speed>");
		System.out.println("\t-m <mean speed>");
		System.out.println("\t-o <max. pause>");
		System.out.println("\t-p <pause probability>");
		System.out.println("\t-q <update distance>");
		System.out.println("\t-s <speed standard deviation>");
		System.out.println("\t-t <turn probability>");
		System.out.println("\t-u <no. of blocks along x-axis>");
		System.out.println("\t-v <no. of blocks along y-axis>");
	}

	public Waypoint transitionWaypointFast( Waypoint _w) {
		Waypoint w = super.transitionWaypointFast(  _w);
		return new Waypoint(0.0, alignPos(w.pos));
	}

}
