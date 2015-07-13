package eu.fusepool.p3.osm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.commons.Entity;

/**
 * A OpenStreeetMap XML data transformer
 * @author Luigi Selmi
 *
 */
public class OsmRdfTransformer extends RdfGeneratingTransformer {
    
    final static String XML_DATA_MIME_TYPE = "application/xml"; //MIME type of the data fetched from the url
    final static String RDF_DATA_MIME_TYPE = "text/turtle"; //MIME type of the data fetched from the url
    private static final String XML_DATA_URL_PARAM = "xml";
    //private static final String XSLT_PATH = "/osm-way-node-keys.xsl";
    private static final String XSLT_PATH = "/osm-addresses.xsl";
    private static final UriRef schema_streetAddress = new UriRef("http://schema.org/streetAddress");
    private static final UriRef schema_addressLocality = new UriRef("http://schema.org/addressLocality");
    private static final UriRef schema_addressCountry = new UriRef("http://schema.org/addressCountry");
    private static final UriRef geo_lat = new UriRef("http://www.w3.org/2003/01/geo/wgs84_pos#lat");
    private static final UriRef geo_lon = new UriRef("http://www.w3.org/2003/01/geo/wgs84_pos#lon");
    
    private static final Logger log = LoggerFactory.getLogger(OsmRdfTransformer.class);
    
    JenaTextConfig jena = null;
    Dataset osmDataset = null;
    //Parser parser = null;
    XsltProcessor processor = null;
    String xmlUri = null; // url of the OSM/XML data set
    
    OsmRdfTransformer(XsltProcessor processor, String xmlUri) {
        jena = new JenaTextConfig();
        osmDataset = jena.createMemDatasetFromCode();
        this.xmlUri = xmlUri;
        this.processor = processor;
        //String file = getClass().getResource("osm-giglio-ways.ttl").getFile();
        //jena.loadData(osmDataset, file);
    }
    
     /**
     * Set the supported input format for the data sent directly by the client
     */
    @Override
    public Set<MimeType> getSupportedInputFormats() {
        Parser parser = Parser.getInstance();
        try {
            Set<MimeType> mimeSet = new HashSet<MimeType>();
            for (String mediaFormat : parser.getSupportedFormats()) {           
              mimeSet.add(new MimeType(mediaFormat));
            }
            return Collections.unmodifiableSet(mimeSet);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Set of transformer output data formats supported.
     */
    @Override
    public Set<MimeType> getSupportedOutputFormats() {
        try {
          Set<MimeType> mimeSet = new HashSet<MimeType>();             
          mimeSet.add(new MimeType(RDF_DATA_MIME_TYPE));
          return Collections.unmodifiableSet(mimeSet);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Takes from the client an address in RDF of which it wants the 
     * geographical coordinates in a format like
     *  
     * <> <http://schema.org/streetAddress> "Via Roma 1" ;
     *                           <http://schema.org/addressLocality> "Trento" ;
     *                           <http://schema.org/addressCountry> "IT" .
     * 
     * The format of the address should follow that used by the national post service of the country.
     * The country must be provided by its 2 digit ISO code (i.e. "IT" for Italy)
     * The url of the OSM/XML data set to look into must be provided as a parameter 'xml' (optional)     
     * The application caches the RDF data. If no URL are provided for the data the application
     * looks in the cache. 
     * Returns the original RDF data with geographical coordinates. 
     *    
     * <http://example.org/res1> <http://schema.org/streetAddress> "Via Roma 1" ;
     *                           <http://schema.org/addressLocality> "Trento" ;
     *                           <http://schema.org/addressCountry> "IT" ;
     *                           <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "46.3634673" ;
     *                           <http://www.w3.org/2003/01/geo/wgs84_pos#long> "11.0357087" .
     */
    @Override
    public TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {    	
        TripleCollection resultGraph = new SimpleMGraph(); // graph to be sent back to the client
        Model dataGraph = ModelFactory.createDefaultModel(); // graph to store the data after the transformation
        String mediaType = entity.getType().toString();   
        String contentLocation = null;
        if ( entity.getContentLocation() != null ) {
            contentLocation = entity.getContentLocation().toString();
        }
                
        TripleCollection inputGraph = Parser.getInstance().parse( entity.getData(), mediaType);        
        
        Address address = getAddress( inputGraph );
        
        String mimeType = entity.getType().toString();        
        
        // Fetch the OSM data from the url and transforms it into RDF via XSL.
        Dataset dataset = null;
        log.info("Data Url : " + xmlUri);
        if( xmlUri != null){
        	try {
        	  InputStream xslt = getClass().getResourceAsStream( XSLT_PATH );
  			  InputStream osmRdfIn = processor.processXml(xslt, getOsmData(xmlUri), contentLocation);
  			  RDFDataMgr.read(dataGraph, osmRdfIn, null, Lang.TURTLE);
  			  dataset = store(dataGraph);
  			}
  			catch(TransformerConfigurationException tce){
  				throw new RuntimeException(tce.getMessage());
  			} 
  			catch (TransformerException te) {				
					throw new RuntimeException(te.getMessage());
			}
            
        }
        else {
            dataset = osmDataset;
        }
        
        // Geocoding: search for the street with the name sent by the client 
        // and return the geographic coordinates
        if(address != null && ! "".equals(address.getStreetAddress()))
            resultGraph = geocodeAddress(dataset, address);
        
        return resultGraph;
        
    }
    /*
     * Extracts the object of the property schema:streetAddress that will be geocoded.
     */
    private Address getAddress(TripleCollection inputGraph) {
    	Address addr = new Address();    	
    	Iterator<Triple> itriple = inputGraph.filter(null,schema_streetAddress,null);
    	while ( itriple.hasNext() ) {
    		Triple triple = itriple.next();
    		UriRef addressUri = (UriRef) triple.getSubject();
    		addr.setStreetAddress( ((PlainLiteralImpl) triple.getObject()).getLexicalForm() );
    		// get locality
    		Iterator<Triple> addresslocalityIter = inputGraph.filter(addressUri, schema_addressLocality, null) ;
    		if ( addresslocalityIter != null ) {
	    		while ( addresslocalityIter.hasNext() ) {
	    			String locality = ((PlainLiteralImpl) addresslocalityIter.next().getObject()).getLexicalForm();	    
	    			if ( ! "".equals(locality) ) {
	    				addr.setLocality( locality );
	    			}
	    		}
    		}    		   
	        // get country code
	    	Iterator<Triple> addressCountryIter = inputGraph.filter(addressUri, schema_addressCountry, null) ;
	    	if ( addressCountryIter != null ) {
	    		while ( addressCountryIter.hasNext() ) {
	    			String countryCode = ((PlainLiteralImpl) addressCountryIter.next().getObject()).getLexicalForm();	    
	    			if ( ! "".equals( countryCode ) ) {
	    				addr.setCountryCode( countryCode );
	    			}
	    		}
    		}
    		
    	}
	    	
    	return addr;
    }
    /**
     * Store and index the new RDF data in a Jena triple store.
     * Returns the updated dataset.
     * @param graph
     * @throws IOException 
     */
    private Dataset store(Model graph) throws IOException{
        Dataset dataset = jena.createMemDatasetFromCode();
        File rdfFile = File.createTempFile("jenatdb-", ".ttl");
        OutputStream os = new FileOutputStream(rdfFile);
        RDFDataMgr.write(os, graph, Lang.TURTLE);
        //String defaultFile = getClass().getResource("trento-osm-keys.ttl").getFile();
        //jena.loadData(dataset, defaultFile);
        jena.loadData(dataset, rdfFile.getAbsolutePath());
        return dataset;
    }
    
    /**
     * Search for an address (a node in OSM).
     * @param graph The input graph contains a schema:streetAddress with the name of the street, the locality and the country code .
     * @return Returns the geocoordinates of the street that has been found. 
     */
    private TripleCollection geocodeAddress(Dataset ds, Address address){
        TripleCollection geoCodeRdf = new SimpleMGraph();
        
        String pre = StrUtils.strjoinNL( 
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" ,
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" ,
            "PREFIX schema: <http://schema.org/>" ,
            "PREFIX text: <http://jena.apache.org/text#>" ,
            "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>" ,
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>") ;
        
        String qs = StrUtils.strjoinNL( "SELECT ?s ?street ?lat ?lon" ,
                                    " { ?s text:query (schema:streetAddress '" + address.getStreetAddress() + "') ;" ,
                                    "      schema:streetAddress ?street ;" ,
                                    "      schema:addressLocality \"" + address.getLocality() + "\" ;" ,
                                    "      schema:addressCountry \"" + address.getCountryCode() + "\" ;" ,
                                    "      geo:lat ?lat ;" ,
                                    "      geo:long ?lon ." ,                                                                       
                                    " }") ;
        
        log.info(pre + "\n" + qs);
        
        ds.begin(ReadWrite.READ) ;
        try {
            Query q = QueryFactory.create(pre + "\n" + qs) ;
            QueryExecution qexec = QueryExecutionFactory.create(q , ds) ;
            //QueryExecUtils.executeQuery(q, qexec) ;
            ResultSet results = qexec.execSelect();   
            int numberOfAddresses = 0;
            for( ; results.hasNext(); ){
                QuerySolution sol = results.nextSolution();
                String streetUriName = sol.getResource("s").getURI();
                String streetName = sol.getLiteral("?street").getString();  
                String latitude = sol.getLiteral("?lat").getLexicalForm();
                String longitude = sol.getLiteral("?lon").getLexicalForm();
                UriRef addressRef = new UriRef(streetUriName);                
                geoCodeRdf.add(new TripleImpl(addressRef, schema_streetAddress, new PlainLiteralImpl(streetName)));
                geoCodeRdf.add(new TripleImpl(addressRef, schema_addressLocality, new PlainLiteralImpl( address.getLocality())) );
                geoCodeRdf.add(new TripleImpl(addressRef, schema_addressCountry, new PlainLiteralImpl( address.getCountryCode())) );
                geoCodeRdf.add(new TripleImpl(addressRef, geo_lat, new PlainLiteralImpl( latitude )) );
                geoCodeRdf.add(new TripleImpl(addressRef, geo_lon, new PlainLiteralImpl( longitude )) );
                numberOfAddresses++;
            }
            log.info("Number of addresses like " + address.getStreetAddress() + " found: " + numberOfAddresses);
        } 
        finally { 
            ds.end() ; 
        }
        
        return geoCodeRdf;
    }
    
    /**
     * Search for a street (way in OSM) 
     * @param graph The input graph contain a schema:streetAddress with the name of the street.
     * @return Returns the geometry of the street that has been found with the coordinates serialized as WKT. 
     */
    private TripleCollection geocodeStreet(Dataset ds, Address address){
        TripleCollection geoCodeRdf = new SimpleMGraph();
        
        String pre = StrUtils.strjoinNL( 
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" ,
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" ,
            "PREFIX schema: <http://schema.org/>" ,
            "PREFIX text: <http://jena.apache.org/text#>" ,
            "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>" ,
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>") ;
        
        String qs = StrUtils.strjoinNL( "SELECT ?s ?street ?geometry ?wkt " ,
                                    " { ?s text:query (schema:streetAddress '" + address.getStreetAddress() + "') ;" ,
                                    "      schema:streetAddress ?street ;" ,
                                    "      schema:addressLocality " + address.getLocality() + " ;" ,
                                    "      schema:addressCountry " + address.getCountryCode() + " ;" ,
                                    "      ogc:geometry ?geometry ." ,
                                    "   ?geo ogc:asWKT ?wkt ." ,
                                    " }") ;
        
        System.out.println(pre + "\n" + qs);
        
        ds.begin(ReadWrite.READ) ;
        try {
            Query q = QueryFactory.create(pre + "\n" + qs) ;
            QueryExecution qexec = QueryExecutionFactory.create(q , ds) ;
            //QueryExecUtils.executeQuery(q, qexec) ;
            ResultSet results = qexec.execSelect();   
            int numberOfToponyms = 0;
            for( ; results.hasNext(); ){
                QuerySolution sol = results.nextSolution();
                String streetUriName = sol.getResource("s").getURI();
                String streetName = sol.getLiteral("?street").getString();
                Resource geo = sol.getResource("?geo");
                String geoUri = geo.getURI();
                String wkt = sol.getLiteral("?wkt").getString();
                UriRef streetRef = new UriRef(streetUriName);
                UriRef geometryRef = new UriRef(geoUri);
                geoCodeRdf.add(new TripleImpl(streetRef, schema_streetAddress, new PlainLiteralImpl(streetName) ));
                geoCodeRdf.add(new TripleImpl(streetRef, schema_addressLocality, new PlainLiteralImpl( address.getLocality())) );
                geoCodeRdf.add(new TripleImpl(streetRef, schema_addressCountry, new PlainLiteralImpl( address.getCountryCode())) );
                geoCodeRdf.add(new TripleImpl(streetRef, new UriRef("http://www.opengis.net/ont/geosparql#geometry"), geometryRef));
                geoCodeRdf.add(new TripleImpl(geometryRef, new UriRef("http://www.opengis.net/ont/geosparql#asWKT"), new PlainLiteralImpl(wkt)));
                numberOfToponyms++;
            }
            log.info("Number of toponymis like " + address.getStreetAddress() + " found: " + numberOfToponyms);
        } 
        finally { 
            ds.end() ; 
        }
        
        return geoCodeRdf;
    }
    
    private InputStream getOsmData(String xmlUri) throws IOException {
    	URL osmDataUrl = new URL(xmlUri);
    	URLConnection connection = osmDataUrl.openConnection();
    	return connection.getInputStream();
    }
  
    @Override
    public boolean isLongRunning() {
        // downloading the dataset can be time consuming
        return false;
    }

}
