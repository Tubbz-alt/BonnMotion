/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (c) 2013 University of Osnabrueck                               **
 ** Code: Matthias Schwamborn                                                 **
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

package edu.bonn.cs.iv.util.maps;

import java.awt.geom.Point2D;

public class CoordinateTransformation {
	
    public static enum proj4lib {
    	JAVAPROJ, // OLD/OBSOLETE
    	PROJ4J    // NEW
    };
    
    private proj4lib useLib = proj4lib.PROJ4J;
    private String projCRS = null;
    private String wgs84CRS = "epsg:4326";
    
    private com.jhlabs.map.proj.Projection proj_old = null;
    private org.osgeo.proj4j.BasicCoordinateTransform proj_new = null;
    private org.osgeo.proj4j.BasicCoordinateTransform proj_new_inv = null;
        
    public CoordinateTransformation(String p, proj4lib l) {
    	projCRS = p;
    	useLib = l;
    	
    	initProjections();
    }
    
    public CoordinateTransformation(String p) {
    	projCRS = p;
    	
    	initProjections();
    }
    
    private void initProjections() {
    	// WGS84 CRS implicitly included
    	proj_old = com.jhlabs.map.proj.ProjectionFactory.getNamedPROJ4CoordinateSystem(projCRS);
    	
    	org.osgeo.proj4j.CRSFactory crsf = new org.osgeo.proj4j.CRSFactory();
    	proj_new = new org.osgeo.proj4j.BasicCoordinateTransform(crsf.createFromName(wgs84CRS), crsf.createFromName(projCRS));
    	proj_new_inv = new org.osgeo.proj4j.BasicCoordinateTransform(crsf.createFromName(projCRS), crsf.createFromName(wgs84CRS));
    }
    
    public Point2D.Double transform(double x, double y) {
    	return transform(x, y, false);
    }
    
    public Point2D.Double transform_inverse(double x, double y) {
    	return transform(x, y, true);
    }
    
    private Point2D.Double transform(double x, double y, boolean inverse) {
    	if (useLib == proj4lib.JAVAPROJ) {
    		return transform_old(x, y, inverse);
    	} else if (useLib == proj4lib.PROJ4J) {
    		return transform_new(x, y, inverse);
    	} else {
    		return new Point2D.Double(x, y);
    	}
    }
    
    public String getProj4Description() {
    	if (useLib == proj4lib.JAVAPROJ) {
    		return proj_old.getPROJ4Description();
    	} else if (useLib == proj4lib.PROJ4J) {
    		return proj_new.getTargetCRS().getParameterString();
    	} else {
    		return "N/A";
    	}
    }
    
    private Point2D.Double transform_old(double x, double y, boolean inverse) {
    	Point2D.Double src = new Point2D.Double(x, y);
    	Point2D.Double dst = new Point2D.Double();
    	
    	if (!inverse) {
    		proj_old.transform(src, dst);
    	} else {
    		proj_old.inverseTransform(src, dst);
    	}
    	
    	return dst;
    }
    
    private Point2D.Double transform_new(double x, double y, boolean inverse) {
    	org.osgeo.proj4j.ProjCoordinate src = new org.osgeo.proj4j.ProjCoordinate(x, y);
    	org.osgeo.proj4j.ProjCoordinate dst = new org.osgeo.proj4j.ProjCoordinate();
    	
    	if (!inverse) {
    		proj_new.transform(src, dst);
    	} else {
    		proj_new_inv.transform(src, dst);
    	}
    	
    	return new Point2D.Double(dst.x, dst.y);
    }
}
