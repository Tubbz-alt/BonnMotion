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
 * @author ygottlieb
 *
 */
public class HeightMap {

    private Dataset dataset = null;
    private double xScale = 1.0;
    private double yScale = 1.0;
    
    static {
        gdal.AllRegister();
    }

    private PositionGeo getPositionGeo(int x, int y) {
        
        double[] lat = new double[1];
        double[] lon = new double[1];
        
        gdal.ApplyGeoTransform(dataset.GetGeoTransform(), x, y, lon, lat);
        
        return new PositionGeo(lon[0], lat[0]);
        
    }
    
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
     * Create a height map from 
     * @param path
     * @param xGeoOffset
     * @param yGeoOffset
     */
    public HeightMap(String path) {
        
        dataset = gdal.Open(path);
        
        PositionGeo topLeft = getPositionGeo(0, 0);
        PositionGeo bottomRight = getPositionGeo(dataset.getRasterXSize(),
                dataset.GetRasterYSize());

        SpatialReference wgs84 = new SpatialReference();
        wgs84.SetWellKnownGeogCS("WGS84");
        CoordinateTransformation coordiateTransformation
            = CoordinateTransformation.CreateCoordinateTransformation(
                    new SpatialReference(dataset.GetProjection()), wgs84);

        PositionGeo topLeftProjected 
            = transformPosition(coordiateTransformation, topLeft);

        PositionGeo bottomRightProjected 
            = transformPosition(coordiateTransformation, bottomRight);
        
        xScale = topLeftProjected.distanceX(bottomRightProjected.x()) 
                / dataset.GetRasterXSize();

        yScale = topLeftProjected.distanceY(bottomRightProjected.y())
                / dataset.GetRasterYSize();
    }
    
    public double getHeight(Position position) {
        return getHeight(position.x, position.y); 
    }
    
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
                retval = 0;
            }
        } else if (error == gdalconst.CE_Warning) {
            System.err.println("HeightMap.getHeight(): warning no values for "
                    + x + ", " + y);
        } else {
            retval = Double.NaN;
        }
        
        return retval;
    }
    
    public double getX() {
        return dataset.GetRasterXSize() * xScale;
    }
    
    public double getY() {
        return dataset.GetRasterYSize() * yScale;
    }    
}
