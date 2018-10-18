/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *    
 */
package org.geotools.polylabelfast;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;




/**
 * Based on Vladimir Agafonkin's Algorithm https://www.mapbox.com/blog/polygon-center/
 * 
 * Implementation of quadtree cells for "Pole of inaccessibility". 
 * 
 * @author Ian Turton
 * @author Casper Børgesen
 * 
 */
public class Cell implements Comparable<Cell> {

    private static final double SQRT2 = 1.4142135623730951;

    private double x;

    private double y;

    private double h;

    private double d;

    private double max;

    private Point p;

    Cell(double x, double y, double h, double distanceToPolygon) {

        this.x = x; // cell center x
        this.y = y; // cell center y
        this. h = h; // half the cell size
        //p = polygon.getFactory().createPoint(new Coordinate(x,y));

        // distance from cell center to polygon
        d = distanceToPolygon; 
        
        // max distance to polygon within a cell
        this.setMax(this.getD() + this.getH() * SQRT2); 
    }

    @Override
    public int compareTo(Cell o) {

        return (int) (o.getMax() - getMax());
    }

    /*
    public Point getPoint() {
        return p;
    }
*/
    
    /*
    // signed distance from point to polygon outline (negative if point is
    // outside)
    private static double pointToPolygonDist(Point point, Geometry polygon) {
        boolean inside = polygon.contains(point);
        double dist = DistanceOp.distance(point, polygon.getBoundary());

        // Points outside has a negative distance and thus will be weighted down later.
        return (inside ? 1 : -1) * dist;
    }
    */

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getD() {
        return d;
    }


    public double getH() {
        return h;
    }


    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }

}