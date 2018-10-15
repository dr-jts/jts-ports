import org.locationtech.jts.geom.Geometry;

import jaitools.jts.PolygonSmoother;

public class PortsJaiTools {
  
  public static Geometry smooth(Geometry geom, double alpha) {
    return PolygonSmoother.smooth(geom, alpha);
  }
}
