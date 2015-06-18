package eu.fusepool.p3.osm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.RestAssured;

import eu.fusepool.p3.transformer.client.Transformer;
import eu.fusepool.p3.transformer.client.TransformerClientImpl;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import eu.fusepool.p3.transformer.server.TransformerServer;



public class OsmRdfTransformerTest {
		  
    final String XML_DATA = "osm-xml-example.osm"; // data to search for
	//final String XML_DATA = "giglio_island.osm.osm"; // data to search for
    private static final UriRef geosparql_geometry = new UriRef("http://www.opengis.net/ont/geosparql#geometry");
    final static String INPUT_DATA_MIME_TYPE = "text/turtle"; //MIME type of the data sent by the client
    final static String TRANSFORMER_MIME_TYPE = "text/turtle"; // MIME type of the transformer output
    // client input data to geocode
    final String INPUT_DATA = "input_data.ttl";
	
    private static int dataServerPort = 0;
    private static int transformerServerPort = 0;
    private byte[] xmlData;
    private byte[] inputData;
    private String transformerBaseUri;
    
    private static MimeType transformerMimeType;
    private static MimeType inputDataMimeType;
    static {
        try {
        	transformerMimeType = new MimeType(TRANSFORMER_MIME_TYPE);
        	inputDataMimeType = new MimeType(INPUT_DATA_MIME_TYPE);
        } catch (MimeTypeParseException ex) {
            Logger.getLogger(OsmRdfTransformerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @BeforeClass
	public static void setDataServerPort() {
    	dataServerPort = findFreePort();		
	}

	@Before
	public void setUp() throws Exception {
		// load the input data 
		inputData = IOUtils.toByteArray(getClass().getResourceAsStream(INPUT_DATA));
		//load the xml data
		xmlData = IOUtils.toByteArray(getClass().getResourceAsStream(XML_DATA));
		// set up the transformer
		transformerServerPort = findFreePort();
		transformerBaseUri = "http://localhost:" + transformerServerPort + "/";
		RestAssured.baseURI = transformerBaseUri;
		TransformerServer server = new TransformerServer(transformerServerPort, false);
		server.start( new OsmTransformerFactory() );
	}
	
	@Rule
    public WireMockRule wireMockRule = new WireMockRule(dataServerPort);

	@Test
	public void testXML() throws Exception {
		Transformer t = new TransformerClientImpl(setUpDataServer());
		// the transformer fetches the xml data from the data server, geocode the address 
		// and sends the RDF result to the client
        {
            Entity response = t.transform(new WritingEntity() {

                @Override
                public MimeType getType() {
                    return inputDataMimeType;
                }

                @Override
                public void writeData(OutputStream out) throws IOException {
                    out.write(inputData);
                }
                
            }, transformerMimeType );

            // the client receives the response from the transformer
            Assert.assertEquals("Wrong media Type of response", TRANSFORMER_MIME_TYPE, response.getType().toString());  
            // Parse the RDF data returned by the transformer after the geocoding 
            final Graph responseGraph = Parser.getInstance().parse(response.getData(), "text/turtle");
            //checks for the presence of a specific property added by the transformer to the input data
            final Iterator<Triple> propertyIter = responseGraph.filter(null, geosparql_geometry, null);
            Assert.assertTrue("No specific property found in response", propertyIter.hasNext());
            //verify that the xml has been loaded from the data server (one call)
            //verify(1,getRequestedFor(urlEqualTo("/xml/" + XML_DATA)));
            
        }
	}
	
	/**
	 * Set up a service in the data server to respond to a get request that must be sent by the transformer
	 * on behalf of its client to fetch the xml data. 
	 * Returns the data url.
	 */
	private String setUpDataServer() throws UnsupportedEncodingException{
		stubFor(get(urlEqualTo("/xml/" + XML_DATA))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.SC_OK)
                    .withHeader("Content-Type", "application/xml")
                    .withBody(xmlData)));
	   // prepare the client HTTP POST message with the RDF data and the url where to dereference the XML data 
       String xmlUrl = "http://localhost:" + dataServerPort + "/xml/" + XML_DATA ;
       // the client sends a request to the transformer with the url of the xml data to be fetched
       String queryString = "xml=" + URLEncoder.encode(xmlUrl, "UTF-8");
       return RestAssured.baseURI + "?" + queryString;
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
