package org.geotools.geometry.jts;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

public class JTS {

    /**
     * Creates a {@link CoordinateSequence} using the provided factory confirming the provided size
     * and dimension are respected.
     * <p>
     * If the requested dimension is larger than the CoordinateSequence implementation can provide,
     * then a sequence of maximum possible dimension should be created. An error should not be
     * thrown.
     * </p>
     * <p>
     * This method is functionally identical to calling csFactory.create(size,dim) - it contains
     * additional logic to work around a limitation on the commonly used
     * CoordinateArraySequenceFactory.</p>
     * 
     * @param size the number of coordinates in the sequence
     * @param dimension the dimension of the coordinates in the sequence
     */
    public static CoordinateSequence createCS(CoordinateSequenceFactory csFactory, int size, int dimension ) {
        CoordinateSequence cs;
        if( csFactory instanceof CoordinateArraySequenceFactory && dimension == 1) {
            // work around JTS 1.14 CoordinateArraySequenceFactory regression ignoring provided dimension
            cs = new CoordinateArraySequence(size,dimension);
        }
        else {
            cs = csFactory.create(size, dimension);
        }
        if( cs.getDimension() != dimension ) {
            // illegal state error, try and fix
            throw new IllegalStateException("Unable to use"+csFactory+" to produce CoordinateSequence with dimension "+dimension);
        }
        return cs;
    }
    
    /**
     * Creates a smoothed copy of the input Geometry. This is only useful for polygonal and lineal
     * geometries. Point objects will be returned unchanged. The smoothing algorithm inserts new
     * vertices which are positioned using Bezier splines. All vertices of the input Geometry will
     * be present in the output Geometry.
     *
     * <p>The {@code fit} parameter controls how tightly the smoothed lines conform to the input
     * line segments, with a value of 1 being tightest and 0 being loosest. Values outside this
     * range will be adjusted up or down as required.
     *
     * <p>The input Geometry can be a simple type (e.g. LineString, Polygon), a multi-type (e.g.
     * MultiLineString, MultiPolygon) or a GeometryCollection. The returned object will be of the
     * same type.
     *
     * @param geom the input geometry
     * @param fit tightness of fit from 0 (loose) to 1 (tight)
     * @return a new Geometry object of the same class as {@code geom}
     * @throws IllegalArgumentException if {@code geom} is {@code null}
     */
    public static Geometry smooth(final Geometry geom, double fit) {
        return smooth(geom, fit, new GeometryFactory());
    }

    /**
     * Creates a smoothed copy of the input Geometry. This is only useful for polygonal and lineal
     * geometries. Point objects will be returned unchanged. The smoothing algorithm inserts new
     * vertices which are positioned using Bezier splines. All vertices of the input Geometry will
     * be present in the output Geometry.
     *
     * <p>The {@code fit} parameter controls how tightly the smoothed lines conform to the input
     * line segments, with a value of 1 being tightest and 0 being loosest. Values outside this
     * range will be adjusted up or down as required.
     *
     * <p>The input Geometry can be a simple type (e.g. LineString, Polygon), a multi-type (e.g.
     * MultiLineString, MultiPolygon) or a GeometryCollection. The returned object will be of the
     * same type.
     *
     * @param geom the input geometry
     * @param fit tightness of fit from 0 (loose) to 1 (tight)
     * @param factory the GeometryFactory to use for creating smoothed objects
     * @return a new Geometry object of the same class as {@code geom}
     * @throws IllegalArgumentException if either {@code geom} or {@code factory} is {@code null}
     */
    public static Geometry smooth(final Geometry geom, double fit, final GeometryFactory factory) {

        //ensureNonNull("geom", geom);
        //ensureNonNull("factory", factory);

        // Adjust fit if necessary
        fit = Math.max(0.0, Math.min(1.0, fit));
        return smooth(geom, fit, factory, new GeometrySmoother(factory));
    }

    private static Geometry smooth(
            final Geometry geom,
            final double fit,
            final GeometryFactory factory,
            GeometrySmoother smoother) {

        switch (Geometries.get(geom)) {
            case POINT:
            case MULTIPOINT:
                // For points, just return the input geometry
                return geom;

            case LINESTRING:
                // This handles open and closed lines (LinearRings)
                return smoothLineString(factory, smoother, geom, fit);

            case MULTILINESTRING:
                return smoothMultiLineString(factory, smoother, geom, fit);

            case POLYGON:
                return smoother.smooth((Polygon) geom, fit);

            case MULTIPOLYGON:
                return smoothMultiPolygon(factory, smoother, geom, fit);

            case GEOMETRYCOLLECTION:
                return smoothGeometryCollection(factory, smoother, geom, fit);

            default:
                throw new UnsupportedOperationException(
                        "No smoothing method available for " + geom.getGeometryType());
        }
    }

    private static Geometry smoothLineString(
            GeometryFactory factory, GeometrySmoother smoother, Geometry geom, double fit) {

        if (geom instanceof LinearRing) {
            // Treat as a Polygon
            Polygon poly = factory.createPolygon((LinearRing) geom, null);
            Polygon smoothed = smoother.smooth(poly, fit);
            return smoothed.getExteriorRing();

        } else {
            return smoother.smooth((LineString) geom, fit);
        }
    }

    private static Geometry smoothMultiLineString(
            GeometryFactory factory, GeometrySmoother smoother, Geometry geom, double fit) {

        final int N = geom.getNumGeometries();
        LineString[] smoothed = new LineString[N];

        for (int i = 0; i < N; i++) {
            smoothed[i] =
                    (LineString) smoothLineString(factory, smoother, geom.getGeometryN(i), fit);
        }

        return factory.createMultiLineString(smoothed);
    }

    private static Geometry smoothMultiPolygon(
            GeometryFactory factory, GeometrySmoother smoother, Geometry geom, double fit) {

        final int N = geom.getNumGeometries();
        Polygon[] smoothed = new Polygon[N];

        for (int i = 0; i < N; i++) {
            smoothed[i] = smoother.smooth((Polygon) geom.getGeometryN(i), fit);
        }

        return factory.createMultiPolygon(smoothed);
    }

    private static Geometry smoothGeometryCollection(
            GeometryFactory factory, GeometrySmoother smoother, Geometry geom, double fit) {

        final int N = geom.getNumGeometries();
        Geometry[] smoothed = new Geometry[N];

        for (int i = 0; i < N; i++) {
            smoothed[i] = smooth(geom.getGeometryN(i), fit, factory, smoother);
        }

        return factory.createGeometryCollection(smoothed);
    }
}
