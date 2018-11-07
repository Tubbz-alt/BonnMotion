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

/** The event of a link going up or down at a certain point in time. */

public class LinkStatusChange {
	/** Time of link status change. */
	public final double time;
	/** Link source. */
	public final int src;
	/** Link destination. */
	public final int dst;
	/** True, if the link is going up, false, if it is going down. */
	public final boolean up;
	
	public LinkStatusChange(double time, int src, int dst, boolean up) {
		this.time = time;
		this.src = src;
		this.dst = dst;
		this.up = up;
	}
}
