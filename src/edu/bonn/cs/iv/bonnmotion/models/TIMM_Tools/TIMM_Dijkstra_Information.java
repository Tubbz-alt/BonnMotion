package edu.bonn.cs.iv.bonnmotion.models.TIMM_Tools;

import edu.bonn.cs.iv.bonnmotion.Position;

import java.util.List;

/**
 * This class is used to store the information from a dijkstra run
 */
public class TIMM_Dijkstra_Information {
    private final TIMM_Vertex vertex;
    private TIMM_StatusType status;
    private double distance;
    private TIMM_Vertex predecessor;

    public TIMM_Dijkstra_Information(TIMM_Vertex vertex) {
        this.vertex = vertex;
        this.status = TIMM_StatusType.NOT_ACTIVE;
        this.distance = Double.MAX_VALUE;
        this.predecessor = null;
    }
    
    public TIMM_Vertex getVertex() {
        return vertex;
    }

    public String getIdentification() {
        return vertex.getIdentification();
    }

    public TIMM_StatusType getStatus() {
        return status;
    }

    public Position getPosition() {
        return vertex.getPosition();
    }

    public List<TIMM_Vertex> getNeighbor() {
        return vertex.getNeighbors();
    }

    public double getDistance() {
        return distance;
    }

    public TIMM_Vertex getPredecessor() {
        return predecessor;
    }

    public void setStatus(TIMM_StatusType status) {
        this.status = status;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setPredecessor(TIMM_Vertex predecessor) {
        this.predecessor = predecessor;
    }
}
