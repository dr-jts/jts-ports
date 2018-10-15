import org.locationtech.jts.geom.Geometry;

import clipper.DefaultClipper;
import clipper.JtsClipper;
import clipper.Clipper;
import clipper.Paths;

public class PortsClipper {

  public static Geometry intersection(Geometry a, Geometry b, int scale) {
    return JtsClipper.intersection(a, b, scale);
  }

  public static Geometry union(Geometry a, Geometry b, int scale) {
    return JtsClipper.union(a, b, scale);
  }

}
