package eu.fusepool.p3.osm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.Iterator;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdfxml.xmlinput.JenaReader;

import eu.fusepool.p3.osm.XsltProcessor;
import eu.fusepool.p3.osm.XsltProcessorImpl;

public class XsltProcessorImplTest {
	
	XsltProcessor processor;
	
	private static final Logger log = LoggerFactory.getLogger(XsltProcessorImplTest.class);
	private String locationHeader = "http://localhost:7002"; // location header sent by the client to be used as base uri in triples

	@Before
	public void setUp() throws Exception {
		processor = new XsltProcessorImpl();
	}

	@Test
	public void testOsmData() throws TransformerConfigurationException, FileNotFoundException, 
	                                  TransformerException, IOException {
		InputStream xmlIn = getClass().getResourceAsStream("giglio_island.osm");
		InputStream xslIn = getClass().getResourceAsStream("/osm-way-node-keys.xsl");
		InputStream rdfIn = processor.processXml(xslIn, xmlIn, locationHeader);
		Dataset jenads = DatasetFactory.createMem() ;
		Model graph = jenads.getDefaultModel();		
		RDFDataMgr.read(graph, rdfIn, null, Lang.TURTLE);
		Property streetAddress = graph.createProperty("http://schema.org/streetAddress");
		StmtIterator istmt = graph.listStatements(null, streetAddress, (RDFNode) null);
		Assert.assertTrue(istmt.hasNext());
		
	}
	
	/**
     * Prints an input stream..
     * @param in
     */
    private void printInputStream(InputStream in) {
    	StringBuilder text = new StringBuilder();
    	try {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	        String line;
	        while((line = reader.readLine()) != null){
	            text.append(line + "\n");
	        }	
    	}
    	catch(IOException ioe){
    		ioe.printStackTrace();
    	}
    	
      	System.out.println(text);
    	
    }

}
