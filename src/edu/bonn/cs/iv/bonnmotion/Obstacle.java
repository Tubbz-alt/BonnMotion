package edu.bonn.cs.iv.bonnmotion;
import java.util.LinkedList;
import java.awt.Polygon;
import java.awt.geom.Line2D;

public class Obstacle extends Polygon {
	private static final long serialVersionUID = -259947914997848502L;

	double[] Vertices = null;	//every 2 entries are one vertice (x,y)
	Position[] PosVert = null;
	LinkedList<Line2D.Double> Edges = new LinkedList<Line2D.Double>();

	public Obstacle(double[] Vertices) {
		super();
		Position[] temp = new Position[Vertices.length/2];
		for(int i = 0; i < Vertices.length; i = i+2){
			this.addPoint((int)Vertices[i], (int)Vertices[i+1]);
			Position entry = new Position(Vertices[i], Vertices[i+1]);
			temp[i/2] = entry;
		}
		PosVert = temp;
		this.Vertices = Vertices;
		for(int i = 0; i < Vertices.length - 2; i = i + 2){
			Line2D.Double Edge = new Line2D.Double(Vertices[i], Vertices[i+1], Vertices[i+2], Vertices[i+3]);
			Edges.add(Edge);
		}
		Line2D.Double closingEdge = new Line2D.Double(Vertices[Vertices.length-2], Vertices[Vertices.length-1], Vertices[0], Vertices[1]);
		Edges.add(closingEdge);
	}

	public void print(){
		System.out.println("Vertices of Obstacle ");
		for(int i=0; i < Vertices.length; i++){
			System.out.print(Vertices[i] + ",");
			if (i == (Vertices.length - 1)){
				System.out.println("");
			}
		}
	}

	public boolean intersectsLine(double xstart, double ystart, double xstop, double ystop){
		Line2D.Double WayOfNode = new Line2D.Double(xstart, ystart, xstop, ystop);
		boolean intersection = false;
		for (int i = 0; i < Edges.size(); i++){
			intersection = WayOfNode.intersectsLine(Edges.get(i));
			if (intersection){
				return true;
			}
		}
		return false;
	}

	public int intersectsObstacle(double xstart, double ystart, double xstop, double ystop){
		Line2D.Double WayOfNode = new Line2D.Double(xstart, ystart, xstop, ystop);
		boolean intersection = false;
		int numintersections = 0;
		for (int i = 0; i < Edges.size(); i++){
			intersection = WayOfNode.intersectsLine(Edges.get(i));
			if (intersection){
				++numintersections;
			}
		}
		return numintersections;
	}

	public boolean throughObstacle(double xstart, double ystart, double xstop, double ystop){
		Line2D.Double WayOfNode = new Line2D.Double(xstart, ystart, xstop, ystop);
		boolean valid = false;
		double xonline = WayOfNode.x1 + ((WayOfNode.x2 - WayOfNode.x1)/2);
		double yonline = WayOfNode.y1 + ((WayOfNode.y2 - WayOfNode.y1)/2);
		if(this.contains(xonline, yonline)){
			valid = true;
		}
		else{
			valid = false;
		}
		return valid;
	}

	public boolean sameObstacle(Position vertice1, Position vertice2){
		boolean isvertice1 = false;
		boolean isvertice2 = false;
		for(int i = 0; i < PosVert.length; i++){
			if(vertice1.equals(PosVert[i])){
				isvertice1 = true;
			}
			if(vertice2.equals(PosVert[i])){
				isvertice2 = true;
			}
		}
		if(isvertice1 && isvertice2){
			return true;
		}
		return false;
	}

	public double minDistance(Position pos){
		double distance = 0.0;
		double mindist = Double.MAX_VALUE;
		for (int i = 0; i < Edges.size(); i++){
			distance = (Edges.get(i)).ptSegDist(pos.x, pos.y);
			if(distance < mindist){
				mindist = distance;
			}
		}
		return mindist;
	}

	public double[] getVertices(){
		return this.Vertices;
	}

	public Position[] getPosVertices(){
		return this.PosVert;
	}

	public LinkedList<Line2D.Double> getEdges(){
		return this.Edges;
	}

	public String VerticesToString(){
		String represent = new String();
		for(int i=0; i < Vertices.length; i++){
			Double help = new Double(Vertices[i]);
			represent = represent + " " + help.toString();
		}
		return represent;
	}

	public boolean isVertice(Position totest){
		for(int i = 0; i < PosVert.length; i++){
			if(PosVert[i].equals(totest)){
				return true;
			}
		}
		return false;
	}

	//compute CObstacle for circular robot and radius dist
	public Obstacle computeCObstacle(double dist, int circlevertices){
		Polygon robotpos = new Polygon();
		double angle = Math.toRadians(360/circlevertices);
		Polygon tempobstacle = new Polygon();
		Position[] circlepos = new Position[circlevertices];
		for(int i = 0; i < (int)circlevertices; i++){
			circlepos[i] = new Position(dist * Math.cos(i*angle), dist * Math.sin(i*angle));
			robotpos.addPoint((int)Math.rint(circlepos[i].x), (int)Math.rint(circlepos[i].y));
		}
		tempobstacle = MinkowskiSum(robotpos);
		double[] temp = new double[2*tempobstacle.npoints];
		for(int i = 0; i < tempobstacle.npoints; i++){
			temp[2*i] = tempobstacle.xpoints[i];
			temp[2*i+1] = tempobstacle.ypoints[i];
		}
		Obstacle obs = new Obstacle(temp);
		return obs;
	}

	//compute Minkowski sum for given polygon robotpos
	public Polygon MinkowskiSum(Polygon robotpos){
		Polygon newobs = new Polygon();
		LinkedList<Position> positions = new LinkedList<Position>();
		boolean alreadyInserted = false;
		for(int i = 0; i < PosVert.length; i++){
			for(int j = 0; j < robotpos.npoints; j++){
				//because -R(0,0) here - instead of +
				Position pos = new Position(PosVert[i].x - robotpos.xpoints[j], PosVert[i].y - robotpos.ypoints[j]);
				alreadyInserted = false;
				for(int p = 0; p < positions.size(); p++){
					if((positions.get(p)).equals(pos)){
						alreadyInserted = true;
					}
				}
				if(!alreadyInserted){
					positions.add(pos);
				}
			}
		}
		for(int i = 0; i < 3; i++){
			newobs.addPoint(((int)((Position)positions.get(i)).x), ((int)((Position)positions.get(i)).y));
		}
		for(int i = 3; i < positions.size(); i++){
			LinkedList<Line2D.Double> temp = new LinkedList<Line2D.Double>();
			boolean startset = false;
			int start = 0;
			int stop = 0;
			double startposx = 0.0;
			double startposy = 0.0;
			double stopposx = 0.0;
			double stopposy = 0.0;
			LinkedList<Integer> xcoordinates = new LinkedList<Integer>();
			LinkedList<Integer> ycoordinates = new LinkedList<Integer>();
			if(!newobs.contains(((Position)positions.get(i)).x, ((Position)positions.get(i)).y)){
				temp = computeTangents(((Position)positions.get(i)), newobs);
				if(temp.size() != 2){
					System.out.println("Error");
					System.exit(0);
				}
				for(int j = 0; j < newobs.npoints; j++){
					for(int p = 0; p < temp.size(); p++){
						if(!startset){
							if(((Line2D.Double)temp.get(p)).x1 == newobs.xpoints[j] && ((Line2D.Double)temp.get(p)).y1 == newobs.ypoints[j]){
								start = j;
								startset = true;
								startposx = newobs.xpoints[j];
								startposy = newobs.ypoints[j];
							}
							if(((Line2D.Double)temp.get(p)).x2 == newobs.xpoints[j] && ((Line2D.Double)temp.get(p)).y2 == newobs.ypoints[j]){
								start = j;
								startset = true;
								startposx = newobs.xpoints[j];
								startposy = newobs.ypoints[j];
							}
						}
						else{
							if(((Line2D.Double)temp.get(p)).x1 == newobs.xpoints[j] && ((Line2D.Double)temp.get(p)).y1 == newobs.ypoints[j]){
								stop = j;
								stopposx = newobs.xpoints[j];
								stopposy = newobs.ypoints[j];
							}
							if(((Line2D.Double)temp.get(p)).x2 == newobs.xpoints[j] && ((Line2D.Double)temp.get(p)).y2 == newobs.ypoints[j]){
								stop = j;
								stopposx = newobs.xpoints[j];
								stopposy = newobs.ypoints[j];
							}

						}
					}
				}
				Line2D.Double line = new Line2D.Double(startposx, startposy, stopposx, stopposy);
				int location = line.relativeCCW(((Position)positions.get(i)).x, ((Position)positions.get(i)).y);
				if(location == 1){
					for(int j = 0; j <= start; j++){
						Integer help = new Integer(newobs.xpoints[j]);
						Integer help2 = new Integer(newobs.ypoints[j]);
						xcoordinates.add(help);
						ycoordinates.add(help2);
					}
					Integer intobj = new Integer(((int)((Position)positions.get(i)).x));
					Integer intobj2 = new Integer(((int)((Position)positions.get(i)).y));
					xcoordinates.add(intobj);
					ycoordinates.add(intobj2);
					for(int j = stop; j < newobs.npoints; j++){
						Integer help = new Integer(newobs.xpoints[j]);
						Integer help2 = new Integer(newobs.ypoints[j]);
						xcoordinates.add(help);
						ycoordinates.add(help2);
					}
				}
				else{
					if(location == -1){
						for(int j = 0; j <= stop; j++){
							Integer help = new Integer(newobs.xpoints[j]);
							Integer help2 = new Integer(newobs.ypoints[j]);
							xcoordinates.add(help);
							ycoordinates.add(help2);
						}
						Integer intobj = new Integer(((int)((Position)positions.get(i)).x));
						Integer intobj2 = new Integer(((int)((Position)positions.get(i)).y));
						xcoordinates.add(intobj);
						ycoordinates.add(intobj2);
					}
					else{
						if(location == 0){
							for(int j = 0; j <= start; j++){
								Integer help = new Integer(newobs.xpoints[j]);
								Integer help2 = new Integer(newobs.ypoints[j]);
								xcoordinates.add(help);
								ycoordinates.add(help2);
							}
							Integer intobj = new Integer(((int)((Position)positions.get(i)).x));
							Integer intobj2 = new Integer(((int)((Position)positions.get(i)).y));
							xcoordinates.add(intobj);
							ycoordinates.add(intobj2);
							for(int j = stop; j < newobs.npoints; j++){
								Integer help = new Integer(newobs.xpoints[j]);
								Integer help2 = new Integer(newobs.ypoints[j]);
								xcoordinates.add(help);
								ycoordinates.add(help2);
							}
						}
					}
				}
				newobs.reset();
				for(int j = 0; j < xcoordinates.size(); j++){
					newobs.addPoint(((Integer)xcoordinates.get(j)).intValue(), ((Integer)ycoordinates.get(j)).intValue());
				}
			}
			else{
				//System.out.println("drinnen");
			}
		}
		return newobs;
	}

	//comupte tangents for given point p and polygon newobs
	public LinkedList<Line2D.Double> computeTangents(Position p, Polygon newobs){
		LinkedList<Line2D.Double> newobsedges = new LinkedList<Line2D.Double>();
		LinkedList<Line2D.Double> tangents = new LinkedList<Line2D.Double>();
		LinkedList<Line2D.Double> collinearpoints = new LinkedList<Line2D.Double>();
		LinkedList<Line2D.Double> toremove = new LinkedList<Line2D.Double>();
		int factor = this.getBounds().width + this.getBounds().height;
		double minimum = Double.MAX_VALUE;
		double tmp = 0.0;
		int index = 0;
		factor = 1000;
		for(int i = 0; i < newobs.npoints; i++){
			Line2D.Double temp = new Line2D.Double(newobs.xpoints[i], newobs.ypoints[i], newobs.xpoints[(i+1) % newobs.npoints], newobs.ypoints[(i+1) % newobs.npoints]);
			newobsedges.add(temp);
		}
		for(int i = 0; i < newobsedges.size(); i++){
			if(((Line2D.Double)newobsedges.get(i)).ptSegDist(p.x, p.y) == 0){
				Line2D.Double tangent1 = new Line2D.Double(((Line2D.Double)newobsedges.get(i)).x1, ((Line2D.Double)newobsedges.get(i)).y1, p.x, p.y);
				Line2D.Double tangent2 = new Line2D.Double(p.x,p.y,((Line2D.Double)newobsedges.get(i)).x2, ((Line2D.Double)newobsedges.get(i)).y2);
				tangents.add(tangent1);
				tangents.add(tangent2);
				return tangents;
			}
		}
		for(int i = 0; i < newobs.npoints; i++){
			Line2D.Double line = new Line2D.Double(p.x, p.y, newobs.xpoints[i], newobs.ypoints[i]);
			Line2D.Double ray = new Line2D.Double(p.x, p.y, p.x + factor * (newobs.xpoints[i] - p.x), p.y + factor * (newobs.ypoints[i] - p.y));
			int numintersections = 0;
			for (int j = 0; j < newobsedges.size(); j++){
				boolean intersection = false;
				intersection = ray.intersectsLine((Line2D.Double)newobsedges.get(j));
				if (intersection){
					numintersections++;
				}
				if(ray.ptSegDist(((Line2D.Double)newobsedges.get(j)).x1, ((Line2D.Double)newobsedges.get(j)).y1) == 0.0 && ray.ptSegDist(((Line2D.Double)newobsedges.get(j)).x2, ((Line2D.Double)newobsedges.get(j)).y2) == 0.0){
					collinearpoints.add(line);
				}
			}
			if(numintersections == 2){
				tangents.add(line);
			}
		}
		for(int i = 0; i < collinearpoints.size(); i++){
			Position pos1 = new Position(((Line2D.Double)collinearpoints.get(i)).x1, ((Line2D.Double)collinearpoints.get(i)).y1);
			Position pos2 = new Position(((Line2D.Double)collinearpoints.get(i)).x2, ((Line2D.Double)collinearpoints.get(i)).y2);
			tmp = pos1.distance(pos2);
			if(tmp < minimum){
				index = i;
				minimum = tmp;
			}
		}	
		if(collinearpoints.size() != 0){
			for(int i = 0; i < tangents.size(); i++){
				if(((Line2D.Double)tangents.get(i)).ptSegDist(((Line2D.Double)collinearpoints.get(index)).x1, ((Line2D.Double)collinearpoints.get(index)).y1) == 0 && ((Line2D.Double)tangents.get(i)).ptSegDist(((Line2D.Double)collinearpoints.get(index)).x2, ((Line2D.Double)collinearpoints.get(index)).y2) == 0){
				}
			}
			tangents.add(((Line2D.Double)collinearpoints.get(index)));
			Line2D.Double ray = new Line2D.Double(((Line2D.Double)collinearpoints.get(index)).x2 + factor * (((Line2D.Double)collinearpoints.get(index)).x1 - ((Line2D.Double)collinearpoints.get(index)).x2), ((Line2D.Double)collinearpoints.get(index)).y2 + factor * (((Line2D.Double)collinearpoints.get(index)).y1 - ((Line2D.Double)collinearpoints.get(index)).y2), ((Line2D.Double)collinearpoints.get(index)).x1 + factor * (((Line2D.Double)collinearpoints.get(index)).x2 - ((Line2D.Double)collinearpoints.get(index)).x1), ((Line2D.Double)collinearpoints.get(index)).y1 + factor * (((Line2D.Double)collinearpoints.get(index)).y2 - ((Line2D.Double)collinearpoints.get(index)).y1));
			for(int i = 0; i < collinearpoints.size(); i++){
				if(ray.ptSegDist(((Line2D.Double)collinearpoints.get(i)).x1, ((Line2D.Double)collinearpoints.get(i)).y1) == 0.0 && ray.ptSegDist(((Line2D.Double)collinearpoints.get(i)).x2, ((Line2D.Double)collinearpoints.get(i)).y2) == 0.0){
					toremove.add(collinearpoints.get(i));
				}
			}
			for(int i = 0; i < toremove.size(); i++){
				collinearpoints.remove(((Object)toremove.get(i)));
			}
			if(collinearpoints.size() != 0 && tangents.size() != 2){
				minimum = Double.MAX_VALUE;
				index = 0;
				for(int i = 0; i < collinearpoints.size(); i++){
					Position pos1 = new Position(((Line2D.Double)collinearpoints.get(i)).x1, ((Line2D.Double)collinearpoints.get(i)).y1);
					Position pos2 = new Position(((Line2D.Double)collinearpoints.get(i)).x2, ((Line2D.Double)collinearpoints.get(i)).y2);
					tmp = pos1.distance(pos2);
					if(tmp < minimum){
						index = i;
						minimum = tmp;
					}
				}
				tangents.add(((Line2D.Double)collinearpoints.get(index)));
			}
			// Commented out because pos1 and pos2 are never read and collinearpoints.get has no sideeffects...
			/*for(int i = 0; i < collinearpoints.size(); i++){
						Position pos1 = new Position(((Line2D.Double)collinearpoints.get(i)).x1, ((Line2D.Double)collinearpoints.get(i)).y1);
						Position pos2 = new Position(((Line2D.Double)collinearpoints.get(i)).x2, ((Line2D.Double)collinearpoints.get(i)).y2);
				}*/
		}
		return tangents;
	}
}
