import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import earcut4j.Earcut;

public class PortsEarcut {
  public static Geometry triangulate(Geometry poly) {
    double[] data = extractData(poly);
    List<Integer> tris = Earcut.earcut(data);
    return createTriangles(data, tris);
  }

  
  private static Geometry createTriangles(double[] data, List<Integer> tris) {
    List<Geometry> triGeoms = new ArrayList<Geometry>();
    GeometryFactory geomFact = new GeometryFactory();
    int ntri = tris.size() / 3;
    for (int i = 0; i < ntri; i++) {
      int index = 3 * i;
      triGeoms.add(createTriangle(data,
          tris.get(index),
          tris.get(index + 1),
          tris.get(index + 2),
          geomFact)
          );
    }
    return geomFact.buildGeometry(triGeoms);
  }

  private static Geometry createTriangle(double[] data, 
      Integer i0, Integer i1, Integer i2, GeometryFactory geomFact) {
    Coordinate p0 = new Coordinate(data[2 * i0], data[2 * i0+1]);
    Coordinate p1 = new Coordinate(data[2 * i1], data[2 * i1+1]);
    Coordinate p2 = new Coordinate(data[2 * i2], data[2 * i2+1]);
    return geomFact.createPolygon(new Coordinate[] {
        p0, p1, p2, p0.copy()
    });
  }

  private static double[] extractData(Geometry geom) {
    Polygon poly = (Polygon) geom;
    LinearRing ring = poly.getExteriorRing();
    Coordinate[] coords = ring.getCoordinates();
    int dataSize = 2 * (coords.length - 1);
    double[] data = new double[dataSize];
    int idata = 0;
    for (int i = 0; i < coords.length - 1; i++) {
      Coordinate p = coords[i];
      data[idata++] = p.getX();
      data[idata++] = p.getY();
    }
    return data;
  }
}
