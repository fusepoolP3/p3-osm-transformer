package eu.fusepool.p3.osm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.jayway.restassured.RestAssured;

public class OsmXmlParserTest {
    
    OsmXmlParser parser;    
    String MOCK_OSM_SERVER_DATA = "osm-xml-example.osm";
    private static int mockPort = 0;
    private String mockOsmServerData;
    private String mockOsmDataProviderBaseUri;
    
    @BeforeClass
	public static void setMockPort() {
		mockPort = findFreePort();
		
	}

    @Before
    public void setUp() throws Exception {
    	// load the data for the mock server    	
    	mockOsmServerData = IOUtils.toString( getClass().getResourceAsStream(MOCK_OSM_SERVER_DATA) ).replace("\\r|\\n", "");
    	mockOsmDataProviderBaseUri = "http://localhost:" + mockPort;
    	RestAssured.baseURI = mockOsmDataProviderBaseUri;
    	// Set up a service in the mock server to respond to a get request that must be sent by the parser
        // to fetch the data.
    	stubFor(get(urlEqualTo("/data/" + MOCK_OSM_SERVER_DATA))
    	                .willReturn(aResponse()
    	                .withStatus(HttpStatus.SC_OK)
    	                .withHeader("Content-Type", "text/xml")
    	                .withBody(mockOsmServerData)));    	    	       
    }
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);
    
    
    @Test
    public void testProcessXmlBinary() throws IOException {
        final int numberOfRuns = 10;
        double totalTime = 0.0;
        double time = 0.0;                
        
        String osmDataUrl = RestAssured.baseURI + "/data/" + MOCK_OSM_SERVER_DATA ;        
        parser = new OsmXmlParser(osmDataUrl);
        
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
    
    
    @Test
    public void testTransform() throws IOException {
    	String osmDataUrl = RestAssured.baseURI + "/data/" + MOCK_OSM_SERVER_DATA ;        
        parser = new OsmXmlParser(osmDataUrl);
        long startTime = System.currentTimeMillis();
        System.out.println("transform() Start time: " + startTime);
        TripleCollection graph = parser.transform();
        Iterator<Triple> igraph = graph.iterator();
        while(igraph.hasNext()){
            Triple t = igraph.next();
            System.out.println(t.getSubject() + " " + t.getPredicate() + " " + t.getObject());
        }
        
        long stopTime = System.currentTimeMillis();
        System.out.println("transform()  Stop time: " + stopTime);
        double time = (stopTime - startTime) / 1000.0;
        System.out.println("transform() Elapsed time: " + time + " sec." );
        System.out.println();
                
    }
    
    public static int findFreePort() {
        int port = 0;
        try (ServerSocket server = new ServerSocket(0);) {
            port = server.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("unable to find a free port");
        }
        return port;
    }
    

}
