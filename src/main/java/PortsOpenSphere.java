import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.geomfunction.Metadata;
import org.opensphere.geometry.algorithm.ConcaveHull;

public class PortsOpenSphere {
  
  public static Geometry concaveHull(Geometry g, 
      @Metadata(title="Max Length") double threshold) {
    return ConcaveHull.concaveHull(g, threshold);
  }
}
