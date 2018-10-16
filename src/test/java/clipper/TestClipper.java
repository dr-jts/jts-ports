package clipper;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import clipper.jts.JtsClipper;

public class TestClipper {

  public static void main(String[] args) {
    testSimple();
    testMultiResult();
    testMultiUnaryUnion();
  }

  private static void testSimple() {
    Geometry a = read("POLYGON ((100 100, 100 200, 200 200, 200 100, 100 100))");
    Geometry b = read("POLYGON ((250 250, 250 150, 150 150, 150 250, 250 250))");
    Geometry res = JtsClipper.intersection(a, b, 1);
    System.out.println(res);
  }
  
  private static void testMultiResult() {
    Geometry a = read("POLYGON ((100 300, 230 300, 230 230, 130 230, 131 175, 189 175, 190 100, 100 100, 100 300))");
    Geometry b = read("POLYGON ((250 250, 250 150, 150 150, 150 250, 250 250))");
    Geometry res = JtsClipper.intersection(a, b, 1);
    System.out.println(res);
  }
  
  private static void testMultiUnaryUnion() {
    Geometry a = read("MULTIPOLYGON (((100 200, 200 200, 200 100, 100 100, 100 200)), ((300 200, 400 200, 400 100, 300 100, 300 200)))");
    Geometry res = JtsClipper.union(a, null, 1);
    System.out.println(res);
  }
  
  public static Geometry read(String wkt) {
    WKTReader reader = new WKTReader();
    try {
      return reader.read(wkt);
    } catch (ParseException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
