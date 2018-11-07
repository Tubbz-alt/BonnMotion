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

package edu.bonn.cs.iv.bonnmotion;

/** Position in 2-dimensional space, which can also be viewed as Vector starting at (0,0) -- therefore functions like "angle" etc. */

public class Position {
	public final double x;
	public final double y;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distance(Position p) {
		double deltaX = p.x - x;
		double deltaY = p.y - y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}

	public Position rndprox(double maxdist, double _dist, double _dir) {
		double dist = _dist * maxdist;
		double dir = _dir * 2 * Math.PI;
		return new Position(x + Math.cos(dir) * dist, y + Math.sin(dir) * dist);
	}

	public double norm() {
		return Math.sqrt(x * x + y * y);
	}

	public String toString() {
		return "(" + x + ", " + y + ")";
	}

	public String toString(int precision) {
		int mult = 1;
		for (int i = 0; i < precision; i++)
			mult *= 10;
		return "("
			+ ((double) ((int) (x * mult + 0.5)) / mult)
			+ ", "
			+ ((double) ((int) (y * mult + 0.5)) / mult)
			+ ")";
	}

	public boolean equals(Position p) {
		return ((p.x == x) && (p.y == y));
	}

	/** Calculate angle between two vectors, their order being irrelevant.
	* 	@return "Inner" angle between 0 and Pi. */
	public static double angle(Position p, Position q) {
		return Math.acos(scalarProduct(p, q) / (p.norm() * q.norm()));
	}

	/** Calculate angle, counter-clockwise from the first to the second vector.
	* 	@return Angle between 0 and 2*Pi. */
	public static double angle2(Position p, Position q) {
		double a = angle(p, q);
		double o = angle(new Position(-p.y, p.x), q);
		if (o > Math.PI / 2)
			a = 2 * Math.PI - a;
		return a;
	}
	/** Difference between q and p ("how to reach q from p"). */
	public static Position diff(Position p, Position q) {
		return new Position(q.x - p.x, q.y - p.y);
	}

	public static double scalarProduct(Position p, Position q) {
		return p.x * q.x + p.y * q.y;
	}


}
