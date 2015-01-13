package eu.fusepool.p3.osm;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.junit.Before;
import org.junit.Test;

public class OsmXmlParserTest {
    
    OsmXmlParser parser;
    String testFile1 = "file:///home/luigi/projects/fusepoolp3/p3-osm-transformer/src/test/resources/eu/fusepool/p3/osm/giglio_island.osm";
    String testFile2 = "file:///home/luigi/projects/xslt/trento_italy.osm";

    @Before
    public void setUp() throws Exception {
        parser = new OsmXmlParser(testFile2); 
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testTransform() {
        System.out.println("Start time: " + System.currentTimeMillis());
        TripleCollection rdf = parser.transform();
        int size = rdf.size();
        Assert.assertTrue(size > 0);
        System.out.println("Stop time: " + System.currentTimeMillis());
    }

}
