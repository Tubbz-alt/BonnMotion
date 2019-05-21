package edu.bonn.cs.iv.bonnmotion;

import edu.bonn.cs.iv.util.maps.PositionGeo;

/**
 * Stores information that the user defined within the configuration
 * file.
 */

public final class StationaryNode {
    private final int id;
    private PositionGeo position;
    private final Double altitude;


    private StationaryNode() {
        this.id = 0;
        this.position = new PositionGeo(0, 0);
        this.altitude = 0.0;
    }

    private StationaryNode(int id, PositionGeo position, Double altitude) {
        this.id = id;
        this.position = position;
        this.altitude = altitude;
    }

    public static StationaryNode createNode(int id, PositionGeo position, Double altitude) {
        return new StationaryNode(id, position, altitude);
    }

    public int getId() {
        return this.id;
    }

    public PositionGeo getPosition() {
        return this.position;
    }

    public Double getAltitude() {
        return this.altitude;
    }

    public static int compareById(StationaryNode a, StationaryNode b) {
        return new Integer(a.getId()).compareTo(new Integer(b.getId()));
    }
}
