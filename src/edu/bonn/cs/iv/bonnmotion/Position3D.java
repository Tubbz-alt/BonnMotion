package edu.bonn.cs.iv.bonnmotion;

public class Position3D extends Position {
	public final double z;
	
	public Position3D() {
		super(0, 0);
		z = 0.0;
	}
	
	public Position3D(final double x, final double y, final double z) {
		super(x, y);
		this.z = z;
	}

	public Position3D(final double x, final double y, final double z, final double status) {
		super(x, y, status);
	    this.z = z;
	}

	@Override
	public double distance(Position _p) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
	    
	    Position3D p = (Position3D)_p;
        double deltaX = p.x - x;
        double deltaY = p.y - y;
        double deltaZ = p.z - z;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
	}
	
	@Override
	public Position3D rndprox(double maxdist, double _dist, double _dir) {
		double dist = _dist * maxdist;
		double dir = _dir * 2 * Math.PI;
		final double newZ = z + (Math.cos(dir) + Math.sin(dir) - 1) * dist;
		return new Position3D(x + Math.cos(dir) * dist, y + Math.sin(dir) * dist, newZ, status);
	}
	
	@Override
	public double norm() {
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}
	
	@Override
	public String toString(int precision) {
		int mult = 1;
		for (int i = 0; i < precision; i++)
			mult *= 10;
		return "(" + ((double) ((int) (this.x * mult + 0.5)) / mult) + ", " + 
		((double) ((int) (this.y * mult + 0.5)) / mult) + ", " +
		((double) ((int) (this.z * mult + 0.5)) / mult) + ")";
	}
	
	@Override
	public boolean equals(Position _p) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D p = (Position3D)_p;
		return ((p.x == x) && (p.y == y) && (p.z == z));
	}
	   
    @Override
    public String getMovementStringPart() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(this.x);
        sb.append(" ");
        sb.append(this.y);
        sb.append(" ");
        sb.append(this.z);
        
        return sb.toString();
    }
    
    @Override
    public Position3D getWeightenedPosition(Position _w, double weight) {
        assert (_w instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D w = (Position3D)_w;
        return new Position3D(
                this.x * (1 - weight) + w.x * weight,
                this.y * (1 - weight) + w.y * weight,
                this.z * (1 - weight) + w.z * weight,
                this.status);
    }
    
    public Position3D newShiftedPosition(double _x, double _y, double _z) {
        return new Position3D(this.x + _x, this.y + _y, this.z + _z);
    }
    
    @Override
    public Position3D clone(double status) {
        return new Position3D(this.x, this.y, this.z, status);
    }
	
	/** Calculate angle between two vectors, their order being irrelevant.
	 * 	@return "Inner" angle between 0 and Pi. */
	public static double angle(Position _p, Position _q) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
        assert (_q instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D p = (Position3D)_p;
        Position3D q = (Position3D)_q;
		return Math.acos(scalarProduct(p, q) / (p.norm() * q.norm()));
	}
	
	/** Calculate angle, counter-clockwise from the first to the second vector.
	 * 	@return Angle between 0 and 2*Pi. */
	public static double angle2(Position _p, Position _q) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
        assert (_q instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D p = (Position3D)_p;
        Position3D q = (Position3D)_q;
		double a = angle(p, q);
		double o = angle(new Position3D(-p.y, p.x, p.z), q);
		if (o > Math.PI / 2)
			a = 2 * Math.PI - a;
		return a;
	}
	
	/** Difference between q and p ("how to reach q from p"). */
	public static Position3D diff(Position _p, Position _q) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
        assert (_q instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D p = (Position3D)_p;
        Position3D q = (Position3D)_q;
		return new Position3D(q.x - p.x, q.y - p.y, q.z - p.z);
	}
	
	public static double scalarProduct(Position _p, Position _q) {
        assert (_p instanceof Position3D) : "using 3D method with 2D object";
        assert (_q instanceof Position3D) : "using 3D method with 2D object";
        
        Position3D p = (Position3D)_p;
        Position3D q = (Position3D)_q;
		return p.x * q.x + p.y * q.y + p.z * q.z;
	}
}
