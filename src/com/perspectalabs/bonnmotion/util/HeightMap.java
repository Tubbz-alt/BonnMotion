/*******************************************************************************
 ** HeightMap utility functions for BonnMotion                                **
 ** Copyright (C) 2018 Perspecta Labs Inc.                                    **
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

package com.perspectalabs.bonnmotion.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osrConstants;

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.util.maps.PositionGeo;

/**
 * Maps between position and the height on a terrain map. The terrain map is
 * assumed to be raster image whose single raster band contains height values in
 * meters.
 * 
 * @author ygottlieb
 *
 */
public class HeightMap {

    /**
     * The open GDAL Dataset for the terrain map.
     */
    private Dataset dataset = null;
    private Band rasterBand = null;
    private CoordinateTransformation toWgs84 = null;
    private CoordinateTransformation fromWgs84 = null;

    private double[] datasetInvTransform = null;

    private double noDataValue = Double.NaN;

    private Position origin;

    /**
     * The scale of the map: how many meters per pixel in the raster image in
     * the x and y directions separately.
     */
    private double xScale = 1.0;
    private double yScale = 1.0;

    /**
     * Scaling for values read from the data
     */
    private double zScale = 1.0;
    private double zOffset = 0.0;

    // Register all drivers so that GDAL can parse the terrain map
    static {
        gdal.AllRegister();
    }

    private Position applyInvTransform(double x, double y) {

        double[] retvalX = new double[1];
        double[] retvalY = new double[1];

        gdal.ApplyGeoTransform(datasetInvTransform, x, y, retvalX, retvalY);

        return new Position(retvalX[0], retvalY[0]);
    }

    private Position transformPosition(CoordinateTransformation ct, double x,
            double y) {
        double[] retval = new double[3];

        retval[0] = x;
        retval[1] = y;
        retval[2] = 0.0;

        ct.TransformPoint(retval);

        return new Position(retval[0], retval[1]);
    }

    /**
     * Transform the position from one projection to another
     * 
     * @param ct
     *            The Coordinate Transformation between projections
     * @param position
     *            The position to transform
     * @return The position in the new projection
     */
    private Position transformPosition(CoordinateTransformation ct,
            Position position) {
        return transformPosition(ct, position.x, position.y);
    }
    
    private Position transformPosition(CoordinateTransformation ct,
            PositionGeo position) {
        return transformPosition(ct, position.x(), position.y());
    }    

    private class Corner {

        PositionGeo geo;
        int x;
        int y;

        public Corner(int x, int y) {
            this.x = x;
            this.y = y;
            this.geo = HeightMap.this.getPosition(x, y);
        }

        public Position getPosition() {
            double[] retvalX = new double[1];
            double[] retvalY = new double[1];

            gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, retvalX,
                    retvalY);

            return new Position(retvalX[0], retvalY[0]);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ") = " + geo;
        }
    }

    /**
     * @return the origin of the map: a position at the southwest corner of the
     *         map
     */
    private final Position getOrigin() {

        List<Corner> positions = Arrays.asList(new Corner(0, 0),
                new Corner(dataset.GetRasterXSize() - 1,
                        dataset.GetRasterYSize() - 1),
                new Corner(dataset.GetRasterXSize() - 1, 0),
                new Corner(0, dataset.GetRasterYSize() - 1));

        Comparator<Corner> minLatitude = new Comparator<Corner>() {

            @Override
            public int compare(Corner o1, Corner o2) {
                return Double.compare(o1.geo.lat(), o2.geo.lat());
            }

        };

        Comparator<Corner> minLongitude = new Comparator<Corner>() {

            @Override
            public int compare(Corner o1, Corner o2) {
                return Double.compare(o1.geo.lon(), o2.geo.lon());
            }

        };
        positions.sort(minLatitude);

        positions = Arrays.asList(positions.get(0), positions.get(1));
        positions.sort(minLongitude);

        return positions.get(0).getPosition();
    }

    /**
     * @param position
     *            the geographical position in WGS84
     * @return The projected position of the geographical position onto the
     *         height map's projection.
     * @throws IllegalArgumentException
     *             if the projected position is not in the raster
     *
     **/
    private Position getOrigin(PositionGeo position) {
        Position retval = transformPosition(fromWgs84, position);

        Position checkRaster = applyInvTransform(retval.x, retval.y);

        if (checkRaster.x < 0 || checkRaster.x > dataset.GetRasterXSize()
                || checkRaster.y < 0
                || checkRaster.y >= dataset.GetRasterYSize()) {
            throw new IllegalArgumentException("The geographic position "
                    + position + " is outside the height map");
        }

        return new Position(retval.x, retval.y);
    }

    private PositionGeo getPosition(int x, int y) {
        double[] geoX = new double[1];
        double[] geoY = new double[1];

        gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, geoX, geoY);

        Position position = transformPosition(toWgs84, geoX[0], geoY[0]); 
        
        return new PositionGeo(position.x, position.y);
    }

    /**
     * Create a height map from the terrain file
     * 
     * @param path
     *            The path to the terrain file
     */
    public HeightMap(String path, PositionGeo origin) {

        // Open the file
        dataset = gdal.Open(path);
        rasterBand = dataset.GetRasterBand(1);

        SpatialReference datasetProjection = new SpatialReference(
                dataset.GetProjection());

        if (!datasetProjection.GetLinearUnitsName().equalsIgnoreCase("Metre")) {
            throw new RuntimeException(
                    "Map " + path + " uses projection units not in meters");
        } else if (datasetProjection.GetAxisOrientation(null,
                0) != osrConstants.OAO_East) {
            throw new RuntimeException("Map " + path
                    + " uses projection with first axis not towards NORTH.");
        } else if (datasetProjection.GetAxisOrientation(null,
                1) != osrConstants.OAO_North) {
            throw new RuntimeException("Map " + path
                    + " uses projection with second axis not towards EAST.");
        }

        double[] datasetTransform = dataset.GetGeoTransform();

        datasetInvTransform = gdal.InvGeoTransform(datasetTransform);

        if (datasetInvTransform == null) {
            throw new RuntimeException(
                    "Map " + path + " uses non-invertable projection");
        }

        xScale = yScale = datasetProjection.GetLinearUnits();

        // Transform to WGS84 since PositionGeo.distance() uses that geodesic.
        SpatialReference wgs84 = new SpatialReference();
        wgs84.SetWellKnownGeogCS("WGS84");
        toWgs84 = CoordinateTransformation.CreateCoordinateTransformation(
                new SpatialReference(dataset.GetProjection()), wgs84);

        fromWgs84 = CoordinateTransformation.CreateCoordinateTransformation(
                wgs84, new SpatialReference(dataset.GetProjection()));

        Double[] read = new Double[1];
        rasterBand.GetNoDataValue(read);
        if (read[0] != null) {
            noDataValue = read[0];
        }

        read[0] = null;
        rasterBand.GetOffset(read);
        if (read[0] != null) {
            zOffset = read[0];
        }

        read[0] = null;
        rasterBand.GetScale(read);
        if (read[0] != null) {
            zScale = read[0];
        }

        if (origin != null) {
            this.origin = getOrigin(origin);
        } else {
            this.origin = getOrigin();
        }
    }

    /**
     * Get the height based on the terrain map at the given location.
     * 
     * @param position
     *            The position for which to get the height
     * @return @see {@link #getHeight(double, double)}
     */
    public double getHeight(Position position) {
        return getHeight(position.x, position.y);
    }

    /**
     * Get the height based on the terrain map at the given location.
     * 
     * @param x
     *            The distance in meters from the top "left" point of the
     *            terrain map along the X axis. ((0,0) to (x, 0) in the raster.)
     * @param y
     *            The distance in meters from the top "left" point of the
     *            terrain map along the Y axis. ((0,0) to (0, y) in the raster.)
     * @return The value of the raster map at that point. If the raster has
     *         value Integer.MIN_VALUE, returns 0
     */
    public double getHeight(double x, double y) {
        double retval = 0.0;

        double scaledX = origin.x + x / xScale;
        double scaledY = origin.y + y / yScale;

        double[] rasterX = new double[1];
        double[] rasterY = new double[1];

        gdal.ApplyGeoTransform(datasetInvTransform, scaledX, scaledY, rasterX,
                rasterY);

        double[] read = new double[1];

        int error = rasterBand.ReadRaster((int) (rasterX[0]),
                (int) (rasterY[0]), 1, 1, read);

        if (error == gdalconstConstants.CE_None) {
            if (read[0] != noDataValue) {
                retval = zScale * read[0] + zOffset;
            } else {
                System.err.println("HeightMap.getHeight(): warning using 0 for "
                        + x + ", " + y + ": "
                        + transformPosition(toWgs84, scaledX, scaledY));
            }
        } else if (error == gdalconstConstants.CE_Warning) {
            System.err.println("HeightMap.getHeight(): warning no values for "
                    + x + ", " + y);
        } else {
            retval = Double.NaN;
        }

        return retval;
    }

    private Position getRasterPoint(Position p) {
        double scaledX = origin.x + p.x / xScale;
        double scaledY = origin.y + p.y / yScale;

        double[] rasterX = new double[1];
        double[] rasterY = new double[1];

        gdal.ApplyGeoTransform(datasetInvTransform, scaledX, scaledY, rasterX,
                rasterY);

        return new Position(rasterX[0], rasterY[0]);
    }

    private Position getProjectionOffsetPoint(int x, int y) {
        double[] projectionX = new double[1];
        double[] projectionY = new double[1];

        gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, projectionX,
                projectionY);

        return new Position(projectionX[0] * xScale - origin.x,
                projectionY[0] * yScale - origin.y);
    }

    private List<LineSegment> getEdgeSegments(Position p1, Position p2) {
        List<LineSegment> retval = new ArrayList<LineSegment>();
        Position rasterP1 = getRasterPoint(p1);
        Position rasterP2 = getRasterPoint(p2);

        int minX = (int) (Math.min(rasterP1.x, rasterP2.x));
        int maxX = (int) (Math.max(rasterP1.x, rasterP2.x));
        int minY = (int) (Math.min(rasterP1.y, rasterP2.y));
        int maxY = (int) (Math.max(rasterP1.y, rasterP2.y));

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                Position tl = getProjectionOffsetPoint(x, y);
                Position tr = getProjectionOffsetPoint(x + 1, y);
                Position bl = getProjectionOffsetPoint(x, y + 1);

                retval.add(new LineSegment(tl, tr));
                retval.add(new LineSegment(tl, bl));
            }
        }

        return retval;
    }

    private void addEdgePoints(List<Position> edgePoints, final Position p1,
            final Position p2, List<LineSegment> edgeSegments) {
        LineSegment path = new LineSegment(p1, p2);

        for (LineSegment edgeSegment : edgeSegments) {
            try {
                Position edgePoint = path.GetIntersection(edgeSegment);
                edgePoint.z = Double.NaN;
                edgePoints.add(edgePoint);
            } catch (LineSegment.NoIntersectionException e) {
            } catch (LineSegment.ParallelLinesException e) {
            }
        }

        // Sort edge points
        Comparator<Position> edgePointSorter = new Comparator<Position>() {

            @Override
            public int compare(Position arg0, Position arg1) {
                int retval = 0;
                if (p1.x < p2.x) {
                    retval = Double.compare(arg0.x, arg1.x);
                } else {
                    retval = Double.compare(arg1.x, arg0.x);
                }

                if (retval == 0) {
                    if (p1.y < p2.y) {
                        retval = Double.compare(arg0.y, arg1.y);
                    } else {
                        retval = Double.compare(arg1.y, arg0.y);
                    }
                }

                return retval;
            }
        };
        edgePoints.sort(edgePointSorter);

    }

    /**
     * Get a list of positions on the path between p1 and p2 such that:
     * <ul>
     * <li>p1 and p2 are at the ends of the list</li>
     * <li>positions other than p1 and p2 are either at the edges of a raster
     * pixel (edge point) or at the midpoint between to positions at the edges
     * of a raster pixel (center point).</li>
     * <li>the height of each center point is the value in that raster
     * pixel</li>
     * <li>the height of each edge point is an average of the two adjoining
     * center points</li> </ual>
     * 
     * @param p1
     *            The position at the start of the path
     * @param p2
     *            The position at the end of the path
     * @return
     */
    public List<Position> getPath(Position p1, Position p2) {

        List<Position> retval = new ArrayList<Position>();

        addEdgePoints(retval, p1, p2, getEdgeSegments(p1, p2));

        List<Position> centerPoints = new ArrayList<Position>();

        for (int i = 0; i < retval.size() - 1; ++i) {

            Position midPoint = LineSegment.midPoint(retval.get(i),
                    retval.get(i + 1));
            midPoint.z = getHeight(midPoint);
            centerPoints.add(midPoint);
        }

        for (int i = centerPoints.size(); i > 0; --i) {
            retval.add(i, centerPoints.get(i - 1));
        }

        retval.add(0, p1);
        retval.add(p2);

        Position prev = null;

        for (Position position : retval) {

            if (prev != null && Double.isNaN(prev.z)
                    && !Double.isNaN(position.z)) {
                prev.z = position.z;
            }

            prev = position;
        }

        return retval;
    }

    /**
     * Compute the distance between p1 and p2 on the map including the
     * intermediate heights.
     * 
     * @param p1
     *            The start position
     * @param p2
     *            The end position of the path
     * @return The length of the path (as computed by
     *         {@link #getPath(Position, Position)}
     */
    public double getDistance(Position p1, Position p2) {
        return getLength(getPath(p1, p2));
    }

    /**
     * Compute the sum of the Euclidian distances between adjacent positions in
     * the list.
     * 
     * @param path
     *            The list of positions over which to compute the length.
     * @return Return the computed length of the path
     */
    public double getLength(List<Position> path) {
        double retval = 0.0;
        Position previous = null;

        for (Position position : path) {
            if (previous != null) {
                retval += previous.distance(position);
            }
            previous = position;
        }

        return retval;
    }
}
