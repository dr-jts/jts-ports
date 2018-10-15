import org.locationtech.jts.geom.Geometry;
import org.opensphere.geometry.algorithm.ConcaveHull;

public class PortsOpenSphere {
  
  public static Geometry concaveHull(Geometry g, double threshold) {
    return ConcaveHull.concaveHull(g, threshold);
  }
}
