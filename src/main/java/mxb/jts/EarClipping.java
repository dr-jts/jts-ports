package mxb.jts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

/**
 * Demonstrates brute force approach to the ear clipping algorithm
 * to triangulate a polygon. 
 * 
 * This version attempts a general approach to holes.
 *
 * @author Michael Bedward
 */
public class EarClipping {
    private static final double EPS = 1.0E-4;

    private final GeometryFactory gf;
    private final Polygon inputPolygon;
    private Geometry ears;

    /**
     * Demonstrate the ear-clipping algorithm
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        WKTReader reader = new WKTReader();
        Polygon poly = null;

        /*
         * House with diamond hole
         *
        poly = (Polygon) reader.read(
                "POLYGON((0 0, 10 0, 10 5, 5 10, 0 5, 0 0), " +
                "(5 3, 7 5, 5 7, 3 5, 5 3))");
         *
         */

        /*
         * Martin's evil polygon
         */
        poly = (Polygon) reader.read(
                "POLYGON ((50 440, 50 50, 510 50, 510 440, 280 240, 50 440), "
                + "(105 230, 443 228, 106 208, 105 230), "
                + "(280 210, 260 190, 310 190, 280 210))");
        
        EarClipping clipper = new EarClipping(poly);
        Geometry ears = clipper.getResult();

        for (int i = 0; i < ears.getNumGeometries(); i++) {
            System.out.println(ears.getGeometryN(i));
        }
    }

    /**
     * Constructor
     *
     * @param inputPolygon the input polygon
     */
    public EarClipping(Polygon inputPolygon) {
        gf = new GeometryFactory();
        this.inputPolygon = inputPolygon;
    }

    /**
     * Get the result triangular polygons.
     *
     * @return triangles as a GeometryCollection
     */
    public Geometry getResult() {
        if (ears == null) {
            ears = triangulate();
        }

        return ears;
    }

    /**
     * Perform the triangulation
     *
     * @return GeometryCollection of triangular polygons
     */
    private Geometry triangulate() {
        List<Polygon> earList = new ArrayList<Polygon>();
        List<Coordinate> coords = connectHoles();

        removeColinearVertices(coords);
        int N = coords.size() - 1;

        boolean finished = false;
        boolean found = false;
        int k0 = 0;
        do {
            int k1 = (k0 + 1) % N;
            int k2 = (k1 + 1) % N;
            LineString ls = gf.createLineString(new Coordinate[] {coords.get(k0), coords.get(k2)});
            
            found = false;
            if (inputPolygon.covers(ls)) {
                Polygon ear = gf.createPolygon(gf.createLinearRing(
                        new Coordinate[]{coords.get(k0), coords.get(k1), coords.get(k2), coords.get(k0)}),
                        null);

                if (inputPolygon.covers(ear)) {
                    found = true;
                    for (Polygon p : earList) {
                        if (ear.overlaps(p)) {
                            // overlap between candidate ear and previous ear
                            found = false;
                            break;
                        }
                    }

                    if (found) {
                        earList.add(ear);
                        coords.remove(k1);
                        removeColinearVertices(coords);
                        N = coords.size() - 1;
                        k0 = 0;
                        if (N == 3) {  // triangle
                            earList.add(gf.createPolygon(gf.createLinearRing(coords.toArray(new Coordinate[4])), null));
                            finished = true;
                        }
                    }
                }
            }

            if (!found) {
                k0++;

                if (k0 == N) {
                    throw new IllegalStateException("Algorithm failed");
                }
            }

        } while (!finished);

        return gf.createGeometryCollection(earList.toArray(new Geometry[0]));
    }

    /**
     * Remove co-linear vertices. TopologyPreservingSimplifier could be
     * used for this but that seems like over-kill.
     *
     * @param coords current shell vertices
     * @return coordinates with any co-linear vertices removed
     */
    private void removeColinearVertices(List<Coordinate> coords) {
        int N = coords.size() - 1;
        List<Coordinate> coordList = new ArrayList<Coordinate>();

        for (int j = 1; j <= N; ) {
            int i = (j - 1) % N;
            int k = (j + 1) % N;
            if (Math.abs(Math.PI - Angle.angleBetween(coords.get(i), coords.get(j), coords.get(k))) < EPS) {
                coords.remove(j);
                N-- ;
            } else {
                j++ ;
            }
        }
    }

    /**
     * Connect any holes in the input polygon to the exterior ring to
     * form a self-intersecting shell. Holes are added from the lowest
     * upwards. As the resulting shell develops, a hole might be added
     * to what was originally another hole.
     *
     * @return a new polygon with holes (if any) connected to the exterior
     *         ring
     */
    private List<Coordinate> connectHoles() {
        // defensively copy the input polygon
        Polygon poly = (Polygon) inputPolygon.clone();
        poly.normalize();

        List<Geometry> orderedHoles = getOrderedHoles(poly);

        Coordinate[] coords = poly.getExteriorRing().getCoordinates();
        List<Coordinate> shellCoords = new ArrayList<Coordinate>();
        shellCoords.addAll(Arrays.asList(coords));

        for (int i = 0; i < orderedHoles.size(); i++) {
            joinHoleToShell(shellCoords, orderedHoles.get(i));
        }

        return shellCoords;
    }

    /**
     * Returns a list of holes in the input polygon (if any) ordered
     * by y coordinate with ties broken using x coordinate.
     *
     * @param poly input polygon
     * @return a list of Geometry objects representing the ordered holes
     *         (may be empty)
     */
    private List<Geometry> getOrderedHoles(final Polygon poly) {
        List<Geometry> holes = new ArrayList<Geometry>();
        List<IndexedEnvelope> bounds = new ArrayList<IndexedEnvelope>();

        if (poly.getNumInteriorRing() > 0) {
            for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                bounds.add( new IndexedEnvelope(i, poly.getInteriorRingN(i).getEnvelopeInternal()) );
            }

            Collections.sort(bounds, new IndexedEnvelopeComparator());

            for (int i = 0; i < bounds.size(); i++) {
                holes.add(poly.getInteriorRingN(bounds.get(i).index));
            }
        }

        return holes;
    }

    /**
     * Join a given hole to the current shell. The hole coordinates are
     * inserted into the list of shell coordinates.
     *
     * @param shellCoords current shell coordinates
     * @param hole the hole to join
     */
    private void joinHoleToShell(List<Coordinate> shellCoords, Geometry hole) {
        double minD2 = Double.MAX_VALUE;
        int shellVertexIndex = -1;

        final int Ns = shellCoords.size() - 1;

        final int holeVertexIndex = getLowestVertex(hole);
        final Coordinate[] holeCoords = hole.getCoordinates();

        final Coordinate ch = holeCoords[holeVertexIndex];
        List<IndexedDouble> distanceList = new ArrayList<IndexedDouble>();

        /*
         * Note: it's important to scan the shell vertices in reverse so
         * that if a hole ends up being joined to what was originally
         * another hole, the previous hole's coordinates appear in the shell
         * before the new hole's coordinates (otherwise the triangulation
         * algorithm tends to get stuck).
         */
        for (int i = Ns - 1; i >= 0; i--) {
            Coordinate cs = shellCoords.get(i);
            double d2 = (ch.x - cs.x) * (ch.x - cs.x) + (ch.y - cs.y) * (ch.y - cs.y);
            if (d2 < minD2) {
                minD2 = d2;
                shellVertexIndex = i;
            }
            distanceList.add(new IndexedDouble(i, d2));
        }
        
        /*
         * Try a quick join: if the closest shell vertex is reachable without
         * crossing any holes.
         */
        LineString join = gf.createLineString(new Coordinate[]{ch, shellCoords.get(shellVertexIndex)});
        if (inputPolygon.covers(join)) {
            doJoinHole(shellCoords, shellVertexIndex, holeCoords, holeVertexIndex);
            return;
        }

        /*
         * Quick join didn't work. Sort the shell coords on distance to the
         * hole vertex nnd choose the closest reachable one.
         */
        Collections.sort(distanceList, new IndexedDoubleComparator());
        for (int i = 1; i < distanceList.size(); i++) {
            join = gf.createLineString(new Coordinate[] {ch, shellCoords.get(distanceList.get(i).index)});
            if (inputPolygon.covers(join)) {
                shellVertexIndex = distanceList.get(i).index;
                doJoinHole(shellCoords, shellVertexIndex, holeCoords, holeVertexIndex);
                return;
            }
        }

        throw new IllegalStateException("Failed to join hole to shell");
    }

    /**
     * Helper method for joinHoleToShell. Insert the hole coordinates into
     * the shell coordinate list.
     *
     * @param shellCoords list of current shell coordinates
     * @param shellVertexIndex insertion point in the shell coordinate list
     * @param holeCoords array of hole coordinates
     * @param holeVertexIndex attachment point of hole
     */
    private void doJoinHole(List<Coordinate> shellCoords, int shellVertexIndex, Coordinate[] holeCoords, int holeVertexIndex) {
        List<Coordinate> toAdd = new ArrayList<Coordinate>();
        toAdd.add(new Coordinate(shellCoords.get(shellVertexIndex)));

        final int N = holeCoords.length - 1;
        int i = holeVertexIndex;
        do {
            toAdd.add(new Coordinate(holeCoords[i]));
            i = (i + 1) % N;
        } while (i != holeVertexIndex);

        toAdd.add(new Coordinate(holeCoords[holeVertexIndex]));
        shellCoords.addAll(shellVertexIndex, toAdd);
    }

    /**
     * Return the index of the lowest vertex
     *
     * @param geom input geometry
     * @return index of the first vertex found at lowest point
     *         of the geometry
     */
    private int getLowestVertex(Geometry geom) {
        Coordinate[] coords = geom.getCoordinates();
        double minY = geom.getEnvelopeInternal().getMinY();
        for (int i = 0; i < coords.length; i++) {
            if (Math.abs(coords[i].y - minY) < EPS) {
                return i;
            }
        }

        throw new IllegalStateException("Failed to find lowest vertex");
    }

    private static class IndexedEnvelope {
        int index;
        Envelope envelope;

        public IndexedEnvelope(int i, Envelope env) { index = i; envelope = env; }
    }

    private static class IndexedEnvelopeComparator implements Comparator<IndexedEnvelope> {
        public int compare(IndexedEnvelope o1, IndexedEnvelope o2) {
            double delta = o1.envelope.getMinY() - o2.envelope.getMinY();
            if (Math.abs(delta) < EPS) {
                delta = o1.envelope.getMinX() - o2.envelope.getMinX();
                if (Math.abs(delta) < EPS) {
                    return 0;
                }
            }
            return (delta > 0 ? 1 : -1);
        }
    }

    private static class IndexedDouble {
        int index;
        double value;

        public IndexedDouble(int i, double v) { index = i; value = v; }
    }

    private static class IndexedDoubleComparator implements Comparator<IndexedDouble> {
        public int compare(IndexedDouble o1, IndexedDouble o2) {
            double delta = o1.value - o2.value;
            if (Math.abs(delta) < EPS) {
                    return 0;
            }
            return (delta > 0 ? 1 : -1);
        }
    }

}