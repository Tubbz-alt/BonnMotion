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

public class Building {
      public Building(double x1, double x2, double y1, double y2, double doorx, double doory) {
         this.x1 = x1;
         this.x2 = x2;
         this.y1 = y1;
         this.y2 = y2;
         this.doorx = doorx;
         this.doory = doory;
      }

		protected double x1 = 0;
		protected double x2 = 0;
		protected double y1 = 0;
		protected double y2 = 0;
		protected double doorx = 0;
		protected double doory = 0;
}
