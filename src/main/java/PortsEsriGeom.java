import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.esri.core.geometry.OperatorBuffer;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorIntersection;
import com.esri.core.geometry.ogc.OGCGeometry;

public class PortsEsriGeom {

  /**
   * Used for timing comparisons.
   * 
   * @param g
   * @return
   */
  public static Geometry noOp(Geometry g) {
    com.esri.core.geometry.Geometry egeom = fromJTS(g);
    return toJTS(egeom);
  }
  
  public static Geometry buffer(Geometry g, double dist) {
    com.esri.core.geometry.Geometry egeom = fromJTS(g);
    com.esri.core.geometry.Geometry result = OperatorBuffer.local().execute(egeom, null, dist, null);
    return toJTS(result);
  }
  
  public static Geometry intersection(Geometry g1, Geometry g2) {
    com.esri.core.geometry.Geometry egeom1 = fromJTS(g1);
    com.esri.core.geometry.Geometry egeom2 = fromJTS(g2);
    com.esri.core.geometry.Geometry result = OperatorIntersection.local().execute(egeom1, egeom2, null, null);
    return toJTS(result);
  }
  
  public static boolean isSimple(Geometry geom) {
    com.esri.core.geometry.Geometry egeom1 = fromJTS(geom);
    OGCGeometry ogcGeom = OGCGeometry.createFromEsriGeometry(egeom1, null);
    return ogcGeom.isSimple();
  }
  
  public static Geometry makeSimple(Geometry geom) {
    com.esri.core.geometry.Geometry egeom1 = fromJTS(geom);
    OGCGeometry ogcGeom = OGCGeometry.createFromEsriGeometry(egeom1, null);
    OGCGeometry gSimp = ogcGeom.makeSimple();
    return toJTS(gSimp.getEsriGeometry());
  }
  
  //================================================
  
  private static com.esri.core.geometry.Geometry fromJTS(Geometry geom) {
    // TODO make this less hacky
    String wkt = geom.toString();
    com.esri.core.geometry.Geometry egeom = OperatorImportFromWkt.local().execute(0, com.esri.core.geometry.Geometry.Type.Unknown, wkt, null);
    return egeom;
  }
  
  private static Geometry toJTS(com.esri.core.geometry.Geometry egeom) {
    String wkt = OperatorExportToWkt.local().execute(0, egeom, null);
    WKTReader rdr = new WKTReader();
    Geometry geom = null;
    try {
      geom = rdr.read(wkt);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return geom;
  }
}
