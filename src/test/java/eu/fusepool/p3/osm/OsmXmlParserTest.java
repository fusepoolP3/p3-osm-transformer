package eu.fusepool.p3.osm;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.junit.Before;
import org.junit.Test;

public class OsmXmlParserTest {
    
    OsmXmlParser parser;
    String testFile1 = "file:///home/luigi/projects/xslt/giglio_island.osm";
    String testFile2 = "file:///home/luigi/projects/xslt/trento_italy.osm";
    String testFile3 = "file:///home/luigi/projects/xslt/osm-xml-example.osm";

    @Before
    public void setUp() throws Exception {
        parser = new OsmXmlParser(testFile1); 
    }

    /*
    @Test
    public void testProcessXml() {
        long startTime = System.currentTimeMillis();
        System.out.println("processXml() Start time: " + startTime);
        parser.processXml();
        long stopTime = System.currentTimeMillis();
        System.out.println("processXml()  Stop time: " + stopTime);
        double time = (stopTime - startTime) / 1000.0;
        System.out.println("processXml() Elapsed time: " + time + " sec." );
        System.out.println();
                
    }
    */
    
    @Test
    public void testProcessXmlBinary() {
        final int numberOfRuns = 10;
        double totalTime = 0.0;
        double time = 0.0;
        for(int i = 0; i < 10; i++){
            long startTime = System.currentTimeMillis();
            System.out.println("processXmlBinary() Start time: " + startTime);
            parser.processXmlBinary();
            long stopTime = System.currentTimeMillis();
            System.out.println("processXmlBinary()  Stop time: " + stopTime);
            time = (stopTime - startTime) / 1000.0;
            totalTime += time;
            System.out.println("processXmlBinary() Elapsed time: " + time + " sec." );
            System.out.println();
        }
        double meanTime = totalTime / numberOfRuns;
        System.out.println("Mean execution time: " + meanTime);
    }

}
