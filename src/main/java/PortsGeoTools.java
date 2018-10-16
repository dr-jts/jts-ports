import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.OffsetCurveBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.geomfunction.Metadata;

public class PortsGeoTools {
  
  @Metadata(description="Clips a geometry to a rectangle.")
  public static Geometry clipToRectangle(Geometry geom, Geometry box) {
    return GeometryClipper.clip(geom, box);
  }
  
  public static Geometry offsetCurve(Geometry g, double offset) {
    return OffsetCurveBuilder.offsetCurve(g, offset);
  }
  
  @Metadata(description="Smooths a geometry by inserting points along Bezier splines.")
  public static Geometry smooth(Geometry g, 
      @Metadata(title="Smoothing fraction", description="Alpha value to smooth by (in [0, 1] )") double alpha) {
    return JTS.smooth(g, alpha);
  }
}
