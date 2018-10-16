import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.geomfunction.Metadata;

import clipper.DefaultClipper;
import clipper.Clipper;
import clipper.Paths;
import clipper.jts.JtsClipper;

public class PortsClipper {

  public static Geometry intersection(Geometry a, Geometry b, 
      @Metadata(title="Scale factor") int scale) {
    return JtsClipper.intersection(a, b, scale);
  }

  public static Geometry union(Geometry a, Geometry b, 
      @Metadata(title="Scale factor") int scale) {
    return JtsClipper.union(a, b, scale);
  }
  
  public static Geometry unaryUnion(Geometry a, 
      @Metadata(title="Scale factor") int scale) {
    return JtsClipper.unaryUnion(a, scale);
  }

  public static Geometry offset(Geometry a, 
      @Metadata(title="Scale factor") int scale, 
      @Metadata(title="Distance") double distance) {
    return JtsClipper.offset(a, scale, distance);
  }
}
