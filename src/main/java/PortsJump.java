import org.locationtech.jts.geom.Geometry;
import org.locationtech.jtstest.geomfunction.Metadata;
import org.opensphere.geometry.algorithm.ConcaveHull;

import jump.geom.MakeValidOp;

public class PortsJump {
  
  public static Geometry makeValid(Geometry g) {
    MakeValidOp op = new MakeValidOp();
    return op.makeValid(g);
  }
}
