/**
 * 
 */
package com.perspectalabs.bonnmotion.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

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

    private PositionGeo transformPosition(CoordinateTransformation ct, double x,
            double y) {
        double[] retval = new double[3];

        retval[0] = x;
        retval[1] = y;
        retval[2] = 0.0;

        ct.TransformPoint(retval);

        return new PositionGeo(retval[0], retval[1]);
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
    private PositionGeo transformPosition(CoordinateTransformation ct,
            PositionGeo position) {
        return transformPosition(ct, position.x(), position.y());
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
        } else if (datasetProjection.GetAxisOrientation(null, 0) != osr.OAO_East) {
            throw new RuntimeException(
                    "Map " + path + " uses projection with first axis not towards NORTH.");                
        } else if (datasetProjection.GetAxisOrientation(null, 1) != osr.OAO_North) {
            throw new RuntimeException(
                    "Map " + path + " uses projection with second axis not towards EAST.");                
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

        int error = rasterBand.ReadRaster(
                Double.valueOf(Math.round(rasterX[0])).intValue(),
                Double.valueOf(Math.round(rasterY[0])).intValue(), 1, 1, read);

        if (error == gdalconst.CE_None) {
            if (read[0] != noDataValue) {
                retval = zScale * read[0] + zOffset;
            } else {
                System.err.println("HeightMap.getHeight(): warning using 0 for "
                        + x + ", " + y + ": "
                        + transformPosition(toWgs84, scaledX, scaledY));
            }
        } else if (error == gdalconst.CE_Warning) {
            System.err.println("HeightMap.getHeight(): warning no values for "
                    + x + ", " + y);
        } else {
            retval = Double.NaN;
        }

        return retval;
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
        PositionGeo retval = transformPosition(fromWgs84, position);

        Position checkRaster = applyInvTransform(retval.x(), retval.y());

        if (checkRaster.x < 0 || checkRaster.x > dataset.GetRasterXSize() || checkRaster.y < 0
                || checkRaster.y >= dataset.GetRasterYSize()) {
            throw new IllegalArgumentException("The geographic position "
                    + position + " is outside the height map");
        }

        return new Position(retval.x(), retval.y());
    }
    
    
    private PositionGeo getPosition(int x, int y) {
        double[] geoX = new double[1];
        double[] geoY = new double[1];

        gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, geoX, geoY);
        
        return transformPosition(toWgs84, new PositionGeo(geoX[0], geoY[0]));
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
     * @return the origin of the map: a position at the southwest corner of the map
     */
    private final Position getOrigin() {
        
        List<Corner> positions = Arrays.asList(
                new Corner(0, 0),
                new Corner(dataset.GetRasterXSize() - 1, dataset.GetRasterYSize() - 1),
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
}
