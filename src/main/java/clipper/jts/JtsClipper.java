package clipper.jts;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

import clipper.Clipper;
import clipper.ClipperOffset;
import clipper.DefaultClipper;
import clipper.Path;
import clipper.Paths;
import clipper.Point;
import clipper.Point.LongPoint;
import clipper.PolyNode;
import clipper.PolyTree;

public class JtsClipper {

  private static GeometryFactory geomFact = new GeometryFactory();
  
  public static Geometry intersection(Geometry a, Geometry b, int scale) {
    return executeClip(a, b, Clipper.ClipType.INTERSECTION, scale);
  }

  public static Geometry union(Geometry a, Geometry b, int scale) {
    return executeClip(a, b, Clipper.ClipType.UNION, scale);
  }
  
  public static Geometry offset(Geometry a, int scale, double distance) {
    PrecisionModel pm = new PrecisionModel(scale);
    Paths ap = toPaths(a, pm);
    
    final ClipperOffset cp = new ClipperOffset( );
    cp.addPaths( ap, Clipper.JoinType.ROUND, Clipper.EndType.CLOSED_POLYGON );

    final PolyTree resultPaths = new PolyTree();
    cp.execute(resultPaths, distance * scale);
    // TODO: check success
    Geometry result = fromPolyTree(resultPaths, pm);
    
    return result;
  }
  
  private static Geometry executeClip(Geometry a, Geometry b, Clipper.ClipType op, int scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    Paths ap = toPaths(a, pm);
    Paths bp = toPaths(b, pm);
    
    final DefaultClipper cp = new DefaultClipper( Clipper.STRICTLY_SIMPLE );
    cp.addPaths( ap, Clipper.PolyType.SUBJECT, true );
    cp.addPaths( bp, Clipper.PolyType.CLIP, true );

    /*
    final Paths resultPaths = new Paths();
    boolean success = cp.execute( op, resultPaths);
    Geometry result = fromPaths(resultPaths, pm);
    */
    
    final PolyTree resultPaths = new PolyTree();
    boolean success = cp.execute( op, resultPaths);
    // TODO: check success
    Geometry result = fromPolyTree(resultPaths, pm);
    
    return result;
  }

  private static Geometry fromPolyTree(PolyTree tree, PrecisionModel pm) {
    List<Polygon> polys = new ArrayList<Polygon>();
    List<PolyNode> childs = tree.getChilds();
    for (PolyNode ch : childs) {
      fromPolygon(ch, pm, polys);
    }
    if (polys.size() == 1) {
      return polys.get(0);
    }
    return geomFact.createMultiPolygon(GeometryFactory.toPolygonArray(polys));
  }

  private static void fromPolygon(PolyNode ch, PrecisionModel pm, List<Polygon> polys) {
    List<LongPoint> path = ch.getContour();

    // shell
    // assert: ch.isHole() == true;
    LinearRing shell = toRing(path, pm);
    LinearRing[] holes = fromPolygonHoles(ch, pm);
    Polygon poly = geomFact.createPolygon(shell, holes);
    polys.add(poly);

    // TODO: iterate over polygons inside holes

  }

 
  private static LinearRing[] fromPolygonHoles(PolyNode poly, PrecisionModel pm) {
    List<PolyNode> holes = poly.getChilds();
    if (holes.size() == 0) return null;
    
    LinearRing[] rings = new LinearRing[holes.size()];
    int i = 0;
    for (PolyNode hole : holes) {
      rings[i++] = toRing(hole.getContour(), pm);
    }
    return rings;
  }

  private static LinearRing toRing(List<LongPoint> path,PrecisionModel pm) {
    int n = path.size();
    int ptsNum = n;
      ptsNum++;
    CoordinateSequence seq = CoordinateArraySequenceFactory.instance().create(ptsNum, 2);
    
    for (int i = 0; i < n; i++) {
      copyPoint(path, i, pm, seq, i);
    }
    copyPoint(path, 0, pm, seq, n);
    return geomFact.createLinearRing(seq);
  }
  
  private static Geometry fromPaths(Paths resultPaths, PrecisionModel pm) {
    int n = resultPaths.size();
    Polygon[] polys = new Polygon[n];
    for (int i = 0 ; i < n; i++) {
      Path path = resultPaths.get(i);
      CoordinateSequence seq = toSequence(path, true, pm);
      Polygon poly = geomFact.createPolygon(seq);
      polys[i] = poly;
    }
    return geomFact.createMultiPolygon(polys);
  }
  
  private static CoordinateSequence toSequence(List<LongPoint> path, boolean ensureRing, PrecisionModel pm) {
    int n = path.size();
    int ptsNum = n;
    if (ensureRing) {
      ptsNum++;
    }
    CoordinateSequence seq = CoordinateArraySequenceFactory.instance().create(ptsNum, 2);
    
    for (int i = 0; i < n; i++) {
      copyPoint(path, i, pm, seq, i);
    }
    if (ensureRing) {
      copyPoint(path, 0, pm, seq, n);
    }
    return seq;
  }

  private static void copyPoint(List<LongPoint> path, int pi, PrecisionModel pm, CoordinateSequence seq, int i) {
    LongPoint p = path.get(pi);
    seq.setOrdinate(i, 0, p.getX() / pm.getScale());
    seq.setOrdinate(i, 1, p.getY() / pm.getScale());
  }

  private static Paths toPaths(Geometry a, PrecisionModel pm) {
    Polygon poly = (Polygon) a;
    LineString shell = poly.getExteriorRing();
    Path p = toPath(shell, pm);
    Paths pp = new Paths();
    pp.add(p);
    return pp;
  }

  private static Path toPath(LineString shell, PrecisionModel pm) {
    Path path = new Path(shell.getNumPoints());
    
    CoordinateSequence seq = shell.getCoordinateSequence();
    for (int i = 0; i < seq.size(); i++) {
      long x = (int) Math.round(pm.getScale() * seq.getOrdinate(i, 0));
      long y = (int) Math.round(pm.getScale() * seq.getOrdinate(i, 1));
      LongPoint lp = new Point.LongPoint(x, y);
      path.add(lp);
    }
    return path;
  }
}
