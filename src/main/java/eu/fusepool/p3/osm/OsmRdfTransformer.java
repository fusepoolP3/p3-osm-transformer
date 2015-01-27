package eu.fusepool.p3.osm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
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
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;

/**
 * A OpenStreeetMap XML data transformer
 * @author luigi
 *
 */
public class OsmRdfTransformer extends RdfGeneratingTransformer{
    
    final static String DATA_MIME_TYPE = "application/xml"; //MIME type of the data fetched from the url
    public static final String DATA_QUERY_PARAM = "data";
    
    private static final Logger log = LoggerFactory.getLogger(OsmRdfTransformer.class);
    
    JenaTextConfig jena = null;
    Dataset osmDataset = null;
    
    OsmRdfTransformer() throws IOException{
        jena = new JenaTextConfig();
        osmDataset = jena.getDataset();
        String file = getClass().getResource("trento-osm-keys.ttl").getFile();
        jena.loadData(osmDataset, file);
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
     * Takes from the client some RDF data and optionally a URL to fetch OSM XML data to be used to geocode it.
     * Returns the original RDF data with the enrichments.    
     */
    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        TripleCollection resultGraph = new SimpleMGraph(); // graph to be sent back to the client
        Model dataGraph = ModelFactory.createDefaultModel(); // graph to store the data after the transformation
        String mediaType = entity.getType().toString();   
        //Parser parser = Parser.getInstance();
        //TripleCollection clientGraph = parser.parse( entity.getData(), mediaType);
        String toponym = IOUtils.toString(entity.getData());
        
        // add the client data to the result graph
        //resultGraph.addAll(clientGraph);
        
        String dataUrl = entity.getRequest().getParameter(DATA_QUERY_PARAM);
        
        // Fetch the XML data from the url and transforms it in RDF.
        // The data url must be specified as a query parameter
        log.info("Data Url : " + dataUrl);
        if(dataUrl != null){
            OsmXmlParser osmParser = new OsmXmlParser(dataUrl);
            if( (dataGraph = osmParser.jenaTransform()) != null ){
                store(dataGraph);    
            }
            else {
                throw new RuntimeException("Failed to transform the source data.");
            }
            
        }
        
        // Geocoding: search for the street with the name sent by the client and return the geographic coordinates
        resultGraph = geocode(toponym);
        
        return resultGraph;
        
    }
    /**
     * Store and index the new RDF data in a Jena triple store.
     * @param graph
     */
    private void store(Model graph){
        //jena.updateDataset(graph);
    }
    
    /**
     * 
     * @param graph The input graph contain a schema:streetAddress with the name of the street.
     * @return Returns the geometry of the street that has been found with the coordinates serialized as WKT. 
     */
    private TripleCollection geocode(String toponym){
        TripleCollection geoCodeRdf = new SimpleMGraph();
        
        String pre = StrUtils.strjoinNL( 
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" ,
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" ,
            "PREFIX schema: <http://schema.org/>" ,
            "PREFIX text: <http://jena.apache.org/text#>" ,
            "PREFIX ogc: <http://www.opengis.net/ont/geosparql#>") ;
        String qs = StrUtils.strjoinNL( "SELECT ?s ?street ?geo ?wkt " ,
                                    " { ?s text:query (schema:streetAddress '" + toponym + "') ;" ,
                                    "      schema:streetAddress ?street ;" ,
                                    "      ogc:geometry ?geo ." ,
                                    "   ?geo ogc:asWKT ?wkt ." ,
                                    " }") ;
        
        osmDataset.begin(ReadWrite.READ) ;
        try {
            Query q = QueryFactory.create(pre + "\n" + qs) ;
            QueryExecution qexec = QueryExecutionFactory.create(q , osmDataset) ;
            //QueryExecUtils.executeQuery(q, qexec) ;
            ResultSet results = qexec.execSelect();            
            for( ; results.hasNext(); ){
                QuerySolution sol = results.nextSolution();
                String streetUriName = sol.getResource("s").getURI();
                String streetName = sol.getLiteral("?street").getString();
                Resource geo = sol.getResource("?geo");
                String geoUri = geo.getURI();
                String wkt = sol.getLiteral("?wkt").getString();
                UriRef streetRef = new UriRef(streetUriName);
                UriRef geometryRef = new UriRef(geoUri);
                geoCodeRdf.add(new TripleImpl(streetRef, new UriRef("http://schema.org/streetAddress"), new PlainLiteralImpl(streetName)));
                geoCodeRdf.add(new TripleImpl(streetRef, new UriRef("http://www.opengis.net/ont/geosparql#geometry"), geometryRef));
                geoCodeRdf.add(new TripleImpl(geometryRef, new UriRef("http://www.opengis.net/ont/geosparql#asWKT"), new PlainLiteralImpl(wkt)));
            }
        } 
        finally { 
            osmDataset.end() ; 
        }
        
        return geoCodeRdf;
    }
  
    @Override
    public boolean isLongRunning() {
        // downloading the dataset can be time consuming
        return false;
    }

}
