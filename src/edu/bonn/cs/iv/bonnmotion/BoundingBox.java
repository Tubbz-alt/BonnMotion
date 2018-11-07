/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2010 University of Bonn                                **
 ** Code: Matthias Schwamborn                                                 **
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

import java.awt.geom.Point2D;
import com.jhlabs.map.proj.*;

public class BoundingBox
{
    protected double left = 0;
    protected double bottom = 0;
    protected double right = 0;
    protected double top = 0;
    protected double width = 0;
    protected double height = 0;
    protected Position origin = null;
    protected Projection proj = null;

    public BoundingBox(double left, double bottom, double right, double top)
    {
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.top = top;
        this.width = right - left;
        this.height = top - bottom;
        this.origin = new Position(left, bottom);
    }

    public Position origin()
    {
        return this.origin;
    }

    public double width()
    {
        return this.width;
    }

    public double height()
    {
        return this.height;
    }

    public Projection proj()
    {
        return this.proj;
    }

    public void setProjection(Projection proj)
    {
        this.proj = proj;
    }

    public boolean contains(Position p)
    {
        return p.x >= left && p.x <= right && p.y >= bottom && p.y <= top;
    }

    public BoundingBox transform()
    {
        BoundingBox result = null;

        if (proj != null) // transform coordinates to lon/lat (WGS84)
        {
            Point2D.Double srclb = new Point2D.Double(left, bottom);
            Point2D.Double dstlb = new Point2D.Double();
            Point2D.Double srcrt = new Point2D.Double(right, top);
            Point2D.Double dstrt = new Point2D.Double();
            proj.inverseTransform(srclb, dstlb);
            proj.inverseTransform(srcrt, dstrt);
            result = new BoundingBox(dstlb.x, dstlb.y, dstrt.x, dstrt.y);
        }

        return result;
    }
}
