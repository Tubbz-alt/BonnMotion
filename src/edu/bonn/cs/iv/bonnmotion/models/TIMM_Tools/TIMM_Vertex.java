package edu.bonn.cs.iv.bonnmotion.models.TIMM_Tools;

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.models.TIMM;

import java.util.ArrayList;
import java.util.List;

public class TIMM_Vertex {
    private double firstVisit = Double.MAX_VALUE;
    private Position position;
    private final String identification;
    private List<TIMM_Vertex> neighbors;
    private TIMM_VertexType vType;
    private final TIMM_Settings settings;
    private final TIMM timm;

    public TIMM_Vertex(String _identification, TIMM_Settings _settings, TIMM _timm) {
        this.identification = _identification;   
        this.settings = _settings;
        this.timm = _timm;
    }
    
    public TIMM_Vertex(Position _vertexPosition, String _identification, List<TIMM_Vertex> neighbors, TIMM_VertexType type,
            TIMM_Settings _settings, TIMM _timm) {
        this.position = _vertexPosition;
        this.identification = _identification;
        this.neighbors = (neighbors == null) ? new ArrayList<TIMM_Vertex>() : neighbors;
        this.vType = type;
        if (vType == null) {
            throw new RuntimeException("Error: TIMM_Vertex. TIMM_VertexType cannot be null");
        }
        this.settings = _settings;
        this.timm = _timm;
    }

    public List<TIMM_Vertex> getNeighbors() {
        return neighbors;
    }

    public String getIdentification() {
        return identification;
    }

    public Position getPosition() {
        return position;
    }

    /**
     * Method returns if this vertex is already visited at a given time.
     * @param time
     * @return
     *          true if already visited
     */
    public boolean isVisited(double time) {
        return (time >= firstVisit) ? true : false;
    }

    public double getFirstVisit() {
        return firstVisit;
    }

    /**
     * This method gets the time a node can be at the vertex or in front of the door.
     * If there is time needed to open a door this time is added to the
     * arrival.
     * 
     * @param time
     *              time a node can be at the vertex or in front of the door.
     * @return
     *              the time the node will actually be at the vertex (if the vertex is
     *              a door it is open now)
     */
    public double setNodeReachsVertex(double time) {
        if (time < firstVisit) {
            firstVisit = time;
        }

        if (vType == TIMM_VertexType.DOOR) {
            final double opendoor = this.openingDoor();

            if (TIMM.DEBUG) {
                System.out.println(String.format("Node reaches door and waites until %f", firstVisit + opendoor));
            }
            return firstVisit + opendoor;
        }
        else {
            return firstVisit;
        }
    }

    public void setIsVisited(double time) {
        firstVisit = time;
    }

    private double openingDoor() {
        return (settings.getDoorOpeningAndSecuring() + Math.pow(-1, timm.randomNextInt(2)) * timm.randomNextDouble()
                * Math.sqrt(settings.getDoorOpeningAndSecuringVariance()));
    }
    
    public void overwriteFields(TIMM_Vertex v) {
        this.position = v.position;
        this.neighbors = v.neighbors;
        this.vType = v.vType;
    }
    
    public String toString() {
        return identification;
    }
}