package esrigeom;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorBuffer;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorImportFromWkt;

public class TestEsriGeometry {
  public static void main(String[] args) {
    testBuffer();
  }

  private static void testBuffer() {
    String wkt = "POLYGON ((190 110, 177 121, 110 290, 390 330, 526 45, 310 180, 190 110))";
    
    Geometry g = OperatorImportFromWkt.local().execute(0, Geometry.Type.Unknown, wkt, null);
    
    Geometry gBuffer = OperatorBuffer.local().execute(g, null, 10, null);
    
    String wktBuf = OperatorExportToWkt.local().execute(0, gBuffer, null);
    System.out.println(wktBuf);
  }
}
