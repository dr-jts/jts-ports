package clipper;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

import clipper.Point.LongPoint;

public class JtsClipper {

  private static GeometryFactory geomFact = new GeometryFactory();
  
  public static Geometry intersection(Geometry a, Geometry b, int scale) {
    return executeClip(a, b, Clipper.ClipType.INTERSECTION, scale);
  }

  public static Geometry union(Geometry a, Geometry b, int scale) {
    return executeClip(a, b, Clipper.ClipType.UNION, scale);
  }
  
  private static Geometry executeClip(Geometry a, Geometry b, Clipper.ClipType op, int scale) {
    PrecisionModel pm = new PrecisionModel(scale);
    Paths ap = toPaths(a, pm);
    Paths bp = toPaths(b, pm);
    
    final DefaultClipper cp = new DefaultClipper( Clipper.STRICTLY_SIMPLE );
    cp.addPaths( ap, Clipper.PolyType.SUBJECT, true );
    cp.addPaths( bp, Clipper.PolyType.CLIP, true );

    final Paths resultPaths = new Paths();
    //Paths solution = new Paths();
    boolean success = cp.execute( op, resultPaths);
    
     Geometry result = fromPaths(resultPaths, pm);
    return result;
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

  private static CoordinateSequence toSequence(Path path, boolean ensureRing, PrecisionModel pm) {
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

  private static void copyPoint(Path path, int pi, PrecisionModel pm, CoordinateSequence seq, int i) {
    LongPoint p = path.get(pi);
    seq.setOrdinate(i, 0, p.x / pm.getScale());
    seq.setOrdinate(i, 1, p.y / pm.getScale());
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
