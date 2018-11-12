/**
 * 
 */
package com.perspectalabs.bonnmotion.util;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

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
    /**
     * The scale of the map: how many meters per pixel in the raster image in
     * the x and y directions separately.
     */
    private double xScale = 1.0;
    private double yScale = 1.0;

    // Register all drivers so that GDAL can parse the terrain map
    static {
        gdal.AllRegister();
    }

    /**
     * 
     * @param x
     *            The X-coordinate in meters
     * @param y
     *            The Y-coordinate in meters
     * @return The geographical position (latitude/longitude) corresponding to
     *         the X,Y position
     */
    private PositionGeo getPositionGeo(int x, int y) {

        double[] lat = new double[1];
        double[] lon = new double[1];

        gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, lon, lat);

        return new PositionGeo(lon[0], lat[0]);
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
        double[] retval = new double[3];

        retval[0] = position.x();
        retval[1] = position.y();
        retval[2] = 0.0;

        ct.TransformPoint(retval);

        return new PositionGeo(retval[0], retval[1]);
    }

    /**
     * Create a height map from the terrain file
     * 
     * @param path
     *            The path to the terrain file
     */
    public HeightMap(String path) {

        // Open the file
        dataset = gdal.Open(path);

        // Compute the scale: get the top left of the map and the bottom right,
        // compute the X distance in meters and Y distance in meters, divide by
        // the dimension of the map.
        PositionGeo topLeft = getPositionGeo(0, 0);
        PositionGeo bottomRight = getPositionGeo(dataset.getRasterXSize(),
                dataset.GetRasterYSize());

        // Transform to WGS84 since PositionGeo.distance() uses that geodesic.
        SpatialReference wgs84 = new SpatialReference();
        wgs84.SetWellKnownGeogCS("WGS84");
        CoordinateTransformation coordiateTransformation = CoordinateTransformation
                .CreateCoordinateTransformation(
                        new SpatialReference(dataset.GetProjection()), wgs84);

        PositionGeo topLeftProjected = transformPosition(
                coordiateTransformation, topLeft);

        PositionGeo bottomRightProjected = transformPosition(
                coordiateTransformation, bottomRight);

        xScale = topLeftProjected.distanceX(bottomRightProjected.x())
                / dataset.GetRasterXSize();

        yScale = topLeftProjected.distanceY(bottomRightProjected.y())
                / dataset.GetRasterYSize();
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

        int scaledX = Double.valueOf(Math.round(x / xScale)).intValue();
        int scaledY = Double.valueOf(Math.round(y / yScale)).intValue();

        int[] read = new int[1];

        Band rasterBand = dataset.GetRasterBand(1);

        int error = rasterBand.ReadRaster(scaledX, scaledY, 1, 1, read);

        if (error == gdalconst.CE_None) {
            if (read[0] != Integer.MIN_VALUE) {
                retval = read[0];
            } else {
                System.err.println("HeightMap.getHeight(): warning using 0 for "
                        + x + ", " + y);
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
     * @return the size of the terrain map in meters along the X-axis
     */
    public double getX() {
        return dataset.GetRasterXSize() * xScale;
    }

    /**
     * @return the size of the terrain map in meters along the Y-axis
     */    
    public double getY() {
        return dataset.GetRasterYSize() * yScale;
    }
}
