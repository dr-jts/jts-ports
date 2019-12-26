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

import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.distance.IndexedFacetDistance;


/**
 * Based on Vladimir Agafonkin's Algorithm https://www.mapbox.com/blog/polygon-center/
 * 
 * @author Ian Turton
 * @author Casper B�rgesen
 */
public class PolyLabellerFast {

  public static Point getPoint(Geometry polygon, double precision) {
    PolyLabellerFast labeller = new PolyLabellerFast(polygon, precision);
    return labeller.getPoint();
  }
  
  private Geometry polygon;
  private double precision;
  private IndexedPointInAreaLocator locater;
  private Geometry boundary;
  private IndexedFacetDistance indexedDistance;
  
  PolyLabellerFast(Geometry polygon, double precision) {
    this.polygon = polygon;
    this.precision = precision;
    locater = new IndexedPointInAreaLocator(polygon);
    boundary = polygon.getBoundary();
    indexedDistance = new IndexedFacetDistance(boundary);
  }
  
  private double distanceToPoly(Point p) {
    boolean inside = Location.EXTERIOR != locater.locate(p.getCoordinate());
    //double dist = DistanceOp.distance(p, boundary);
    double dist = indexedDistance.distance(p);

    // Points outside has a negative distance and thus will be weighted down later.
    return (inside ? 1 : -1) * dist;
  }
  
    public Point getPoint() {

        Geometry multiPolygon;
        if (polygon instanceof Polygon) {
            multiPolygon = polygon;
        } else if (polygon instanceof MultiPolygon) {
            multiPolygon = (MultiPolygon) polygon;
        } else {
            throw new IllegalStateException("Input polygon must be a Polygon or MultiPolygon");
        }

        if (polygon.isEmpty() || polygon.getArea() <= 0.0) {
            throw new IllegalStateException("Can not label empty geometries");
        }

        //TODO: can skip this check if better distance algorithm is used (VERY slow) 
        /*
        if (!polygon.isValid()) {
            throw new IllegalStateException("Can not label invalid geometries");
        }
*/
        
        // find the bounding box of the outer ring
        double minX, minY, maxX, maxY;
        Envelope env = multiPolygon.getEnvelopeInternal();
        minX = env.getMinX();
        maxX = env.getMaxX();
        minY = env.getMinY();
        maxY = env.getMaxY();
        double width = env.getWidth();
        double height = env.getHeight();
        double cellSize = Math.min(width, height);
        double h = cellSize / 2.0;

        // a priority queue of cells in order of their "potential" (max distance
        // to polygon)
        PriorityQueue<Cell> cellQueue = new PriorityQueue<>();

        // cover polygon with initial cells
        for (double x = minX; x < maxX; x += cellSize) {
            for (double y = minY; y < maxY; y += cellSize) {
                cellQueue.add(createCell(x + h, y + h, h));
            }
        }

        // take centroid as the first best guess
        Cell bestCell = getCentroidCell(multiPolygon);
        int numProbes = cellQueue.size();

        while (!cellQueue.isEmpty()) {
            // pick the most promising cell from the queue
            Cell cell = cellQueue.remove();

            // update the best cell if we found a better one
            if (cell.getD() > bestCell.getD()) {
                bestCell = cell;
                /*
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("found best " + (Math.round(1e4 * cell.getD()) / 1e4) + " after "
                            + numProbes + " probes");
                }
                */
            }

            // do not drill down further if there's no chance of a better
            // solution
            if (cell.getMax() - bestCell.getD() <= precision)
                continue;

            // split the cell into four cells
            h = cell.getH() / 2;
            cellQueue.add(createCell(cell.getX() - h, cell.getY() - h, h));
            cellQueue.add(createCell(cell.getX() + h, cell.getY() - h, h));
            cellQueue.add(createCell(cell.getX() - h, cell.getY() + h, h));
            cellQueue.add(createCell(cell.getX() + h, cell.getY() + h, h));
            numProbes += 4;
        }

        /*
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("num probes: " + numProbes);
            LOGGER.finer("best distance: " + bestCell.getD());
        }
        */

        return createPoint(bestCell);
    }

    Point createPoint(Cell cell) {
      return createPoint(cell.getX(), cell.getY());
    }  
    Point createPoint(double x, double y) {
      Coordinate coord = new Coordinate(x, y);
      return polygon.getFactory().createPoint(coord);
    }
    
    Cell createCell(double x, double y, double h) {
      return new Cell(x, y, h, distanceToPoly(createPoint(x, y)));
    }
    
    // get a cell centered on polygon centroid
    private Cell getCentroidCell(Geometry poly) {
        Point p = poly.getCentroid();
        return new Cell(p.getX(), p.getY(), 0, distanceToPoly(p));
    }


}