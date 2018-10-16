import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.OffsetCurveBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.geomfunction.Metadata;

public class PortsGeoTools {
  
  public static Geometry clip(Geometry geom, Geometry box) {
    return GeometryClipper.clip(geom, box);
  }
  
  public static Geometry offsetCurve(Geometry g, double offset) {
    return OffsetCurveBuilder.offsetCurve(g, offset);
  }
  
  public static Geometry smooth(Geometry g, 
      @Metadata(title="Smoothing fraction", description="Alpha value to smooth by (in [0, 1])") double alpha) {
    return JTS.smooth(g, alpha);
  }
}
