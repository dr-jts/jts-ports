import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.OffsetCurveBuilder;
import org.locationtech.jts.geom.Geometry;

public class PortsGeoTools {
  
  public static Geometry clip(Geometry geom, Geometry box) {
    return GeometryClipper.clip(geom, box);
  }
  
  public static Geometry offsetCurve(Geometry g, double offset) {
    return OffsetCurveBuilder.offsetCurve(g, offset);
  }
}
