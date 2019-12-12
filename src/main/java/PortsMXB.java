import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import mxb.jts.EarClipping;

public class PortsMXB {
  
  public static Geometry triangulateEarClipping(Geometry geom) {
    EarClipping ec = new EarClipping((Polygon) geom);
    return ec.getResult();
    
  }
}
