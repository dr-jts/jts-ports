import org.geotools.geometry.jts.GeometryClipper;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.OffsetCurveBuilder;
import org.geotools.polylabel.PolyLabeller;
import org.geotools.polylabelfast.PolyLabellerFast;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryMapper;
import org.locationtech.jts.geom.util.GeometryMapper.MapOp;
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
  
  @Metadata(description="Computes a near-optimal point for labelling")
  public static Geometry labelPoint(Geometry g, 
      @Metadata(title="Precision", description="Precision for point placement") double precision) {
    return PolyLabeller.getPolylabel(g, precision);
  }
  
  @Metadata(description="Computes a near-optimal point for labelling")
  public static Geometry labelPointAll(Geometry g, 
      @Metadata(title="Precision", description="Precision for point placement") double precision) {
    return GeometryMapper.map(g, new MapOp() {
      public Geometry map(Geometry g) {
        return PolyLabeller.getPolylabel(g, precision);
      }
    });
  }
  
  @Metadata(description="Computes near-optimal points for labelling")
  public static Geometry labelPointFastAll(Geometry g, 
      @Metadata(title="Precision", description="Precision for point placement") double precision) {
    return GeometryMapper.map(g, new MapOp() {
      public Geometry map(Geometry g) {
        return PolyLabellerFast.getPoint(g, precision);
      }
    });
  }
  
  @Metadata(description="Computes interior points")
  public static Geometry interiorPointAll(Geometry g) {
    return GeometryMapper.map(g, new MapOp() {
      public Geometry map(Geometry g) {
        return g.getInteriorPoint();
      }
    });
  }
}
