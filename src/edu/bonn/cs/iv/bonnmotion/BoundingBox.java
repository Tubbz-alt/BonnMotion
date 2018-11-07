package edu.bonn.cs.iv.bonnmotion;

import java.awt.geom.Point2D;

import edu.bonn.cs.iv.util.maps.*;

public class BoundingBox
{
    protected double left = 0;
    protected double bottom = 0;
    protected double right = 0;
    protected double top = 0;
    protected double width = 0;
    protected double height = 0;
    protected Position origin = null;
    protected CoordinateTransformation transformation = null;

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

    public CoordinateTransformation transformation()
    {
        return this.transformation;
    }

    public void setTransformation(CoordinateTransformation transformation)
    {
        this.transformation = transformation;
    }

    public boolean contains(Position p)
    {
        return p.x >= left && p.x <= right && p.y >= bottom && p.y <= top;
    }

    public BoundingBox transform()
    {
        BoundingBox result = null;

        if (transformation != null) // transform coordinates to lon/lat (WGS84)
        {
            Point2D.Double srclb = new Point2D.Double(left, bottom);
            Point2D.Double dstlb = transformation.transform_inverse(srclb.x, srclb.y);
            Point2D.Double srcrt = new Point2D.Double(right, top);
            Point2D.Double dstrt = transformation.transform_inverse(srcrt.x, srcrt.y);

            result = new BoundingBox(dstlb.x, dstlb.y, dstrt.x, dstrt.y);
        }

        return result;
    }
    
    /** Get a Point2D.Double representation of the current bounding box. */
    public Point2D.Double[] getPoint2D()
    {
    	Point2D.Double[] bbp2d = new Point2D.Double[4];
    	bbp2d[0] = new Point2D.Double(left, bottom);
    	bbp2d[1] = new Point2D.Double(left, top);
    	bbp2d[2] = new Point2D.Double(right, top);
    	bbp2d[3] = new Point2D.Double(right, bottom);
    	return bbp2d;
    }
}
