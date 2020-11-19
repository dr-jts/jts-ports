package test.perf.overlay;

import static org.locationtech.jts.operation.overlayng.OverlayNG.INTERSECTION;
import static org.locationtech.jts.operation.overlayng.OverlayNG.UNION;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.SineStarFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.overlayng.OverlayNG;

import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.OperatorIntersection;

import clipper.jts.JtsClipper;
import test.perf.PerformanceTestCase;
import test.perf.PerformanceTestRunner;

public class OverlayPerfTest 
extends PerformanceTestCase
{
  private static final int PREC_SCALE_FACTOR = 1000000;

  private static final int N_ITER = 1;
  
  static double ORG_X = 100;
  static double ORG_Y = ORG_X;
  static double SIZE = 2 * ORG_X;
  static int N_ARMS = 6;
  static double ARM_RATIO = 0.3;
  
  static int GRID_SIZE = 20;
  static double GRID_CELL_SIZE = SIZE / GRID_SIZE;
  
  static int NUM_CASES = GRID_SIZE * GRID_SIZE;
  
  private Geometry geomA;
  private Geometry[] geomB;

  private PrecisionModel precisionModel;
  
  public static void main(String args[]) {
    PerformanceTestRunner.run(OverlayPerfTest.class);
  }
  
  public OverlayPerfTest(String name) {
    super(name);
    setRunSize(new int[] { 100, 1000, 10000, 100000, 200000 });
    //setRunSize(new int[] { 200000 });
    setRunIterations(N_ITER);
  }

  public void setUp()
  {
    System.out.println("OverlaySR perf test");
    System.out.println("SineStar: origin: ("
        + ORG_X + ", " + ORG_Y + ")  size: " + SIZE
        + "  # arms: " + N_ARMS + "  arm ratio: " + ARM_RATIO);   
    System.out.println("# Iterations: " + N_ITER);
    System.out.println("# B geoms: " + NUM_CASES);
    System.out.println("Precision scale: " + PREC_SCALE_FACTOR);
  }
  
  public void startRun(int npts)
  {
    precisionModel = new PrecisionModel(PREC_SCALE_FACTOR);

    geomA = SineStarFactory.create(new Coordinate(ORG_X, ORG_Y), SIZE, npts, N_ARMS, ARM_RATIO);

    int nptsB = npts / NUM_CASES;
    if (nptsB < 10 ) nptsB = 10;
    
    geomB =  createTestGeoms(NUM_CASES, nptsB);

    System.out.println("\n-------  Running with A: # pts = " + npts + "   B # pts = " +  nptsB);
    
    if (npts == 999) {
      System.out.println(geomA);
      
      for (Geometry g : geomB) {
        System.out.println(g);
      }
    }

  }
  
  private Geometry[] createTestGeoms(int nGeoms, int npts) {
    Geometry[] geoms = new Geometry[ NUM_CASES ];
    int index = 0;
    for (int i = 0; i < GRID_SIZE; i++) {
      for (int j = 0; j < GRID_SIZE; j++) {
        double x = GRID_CELL_SIZE/2 + i * GRID_CELL_SIZE;
        double y = GRID_CELL_SIZE/2 + j * GRID_CELL_SIZE;
        Geometry geom = SineStarFactory.create(new Coordinate(x, y), GRID_CELL_SIZE, npts, N_ARMS, ARM_RATIO);
        geoms[index++] = geom;
      }
    }
    return geoms;
  }
  
  public void runUnionNG()
  {
    for (Geometry b : geomB) {
      OverlayNG.overlay(geomA, b, UNION, precisionModel);
    }
  }
  
  public void runIntersectionNG()
  {
    for (Geometry b : geomB) {
      OverlayNG.overlay(geomA, b, INTERSECTION, precisionModel);
    }
  }  
  
  public void runUnionClipper()
  {
    for (Geometry b : geomB) {
      JtsClipper.union(geomA, b, (int) precisionModel.getScale());
    }
  }  
  
  public void runIntersectionClipper()
  {
    for (Geometry b : geomB) {
      JtsClipper.intersection(geomA, b, (int) precisionModel.getScale());
    }
  }  
  
  public void runIntersectionEsri()
  {
    for (Geometry b : geomB) {
      intersectionEsri(geomA, b);
    }
  }  
  
  public static Geometry intersectionEsri(Geometry g1, Geometry g2) {
    com.esri.core.geometry.Geometry egeom1 = fromJTS(g1);
    com.esri.core.geometry.Geometry egeom2 = fromJTS(g2);
    com.esri.core.geometry.Geometry result = OperatorIntersection.local().execute(egeom1, egeom2, null, null);
    return toJTS(result);
  }
  
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
