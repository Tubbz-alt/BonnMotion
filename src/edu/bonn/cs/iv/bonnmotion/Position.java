package edu.bonn.cs.iv.bonnmotion;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/** Position in 2-dimensional space, which can also be viewed as Vector starting at (0,0) -- therefore functions like "angle" etc. */

public class Position {
	public final double x;
	public final double y;

	//borderentry -> "2";  (node OFF / leaves scenario)
	//borderexit  -> "1";  (node ON  / arrives in scenario)
	//not on border, not status change -> "0";
	public final double status;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
		this.status = 0.0;
	}

	public Position(double x, double y, double status) {
		this.x = x;
		this.y = y;
		this.status = status;
	}

	public double distance(Position p) {
	    assert !((this instanceof Position3D) || (p instanceof Position3D)) : 
	        "using 2D method with 3D object";
	    
		double deltaX = p.x - x;
		double deltaY = p.y - y;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}

	public Position rndprox(double maxdist, double _dist, double _dir) {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	    
		double dist = _dist * maxdist;
		double dir = _dir * 2 * Math.PI;
		return new Position(x + Math.cos(dir) * dist, y + Math.sin(dir) * dist, status);
	}

	public double norm() {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	       
		return Math.sqrt(x * x + y * y);
	}

	public String toString() {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	       
		return "(" + x + ", " + y + ")";
	}

	public String toString(int precision) {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	    
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
        assert !((this instanceof Position3D) || (p instanceof Position3D)) : 
            "using 2D method with 3D object";
	    
		return ((p.x == x) && (p.y == y));
	}

	/** Calculate angle between two vectors, their order being irrelevant.
	 * 	@return "Inner" angle between 0 and Pi. */
	public static double angle(Position p, Position q) {
        assert !((p instanceof Position3D) || (q instanceof Position3D)) : 
            "using 2D method with 3D object";
	    
		return Math.acos(scalarProduct(p, q) / (p.norm() * q.norm()));
	}

	/** Calculate angle, counter-clockwise from the first to the second vector.
	 * 	@return Angle between 0 and 2*Pi. */
	public static double angle2(Position p, Position q) {
        assert !((p instanceof Position3D) || (q instanceof Position3D)) : 
            "using 2D method with 3D object";
	       
		double a = angle(p, q);
		double o = angle(new Position(-p.y, p.x), q);
		if (o > Math.PI / 2)
			a = 2 * Math.PI - a;
		return a;
	}

	/** Difference between q and p ("how to reach q from p"). */
	public static Position diff(Position p, Position q) {
        assert !((p instanceof Position3D) || (q instanceof Position3D)) : 
            "using 2D method with 3D object";
	    
		return new Position(q.x - p.x, q.y - p.y);
	}

	public static double scalarProduct(Position p, Position q) {
        assert !((p instanceof Position3D) || (q instanceof Position3D)) : 
            "using 2D method with 3D object";
	    
		return p.x * q.x + p.y * q.y;
	}

	public static double slope(Line2D.Double line){
		if((line.y2 - line.y1) == 0){
			return Double.MAX_VALUE;
		}
		double slope = (line.x2 - line.x1) / (line.y2 - line.y1);
		return slope;
	}

	public String getMovementStringPart() {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	    
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.x);
		sb.append(" ");
		sb.append(this.y);
		
		return sb.toString();
	}
	
	public Position getWeightenedPosition(Position w, double weight) {
        assert !((this instanceof Position3D) || (w instanceof Position3D)) : 
            "using 2D method with 3D object";
	    
		return new Position(
				this.x * (1 - weight) + w.x * weight,
				this.y * (1 - weight) + w.y * weight,
				this.status);
	}
	
	public Position newShiftedPosition(double _x, double _y) {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	    
		return new Position(this.x + _x, this.y + _y);
	}
	
	public Position clone(double status) {
        assert !(this instanceof Position3D) : "using 2D method with 3D object";
	    
		return new Position(this.x, this.y, status);
	}
	
	/** Get a Point2D.Double representation of the current position. */
	public Point2D.Double getPoint2D() {
		return new Point2D.Double(this.x, this.y);
	}
}
