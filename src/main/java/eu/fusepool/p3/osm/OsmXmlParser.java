package eu.fusepool.p3.osm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class OsmXmlParser {
    
    Document doc = null;
    
    public OsmXmlParser(String osmUrl) throws IOException{
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(getOsmStream(osmUrl));
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        } catch (SAXException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private InputStream getOsmStream(String osmFileUrl) throws IOException{
        URL configUrl = new URL(osmFileUrl);
        URLConnection connection = configUrl.openConnection();
        return connection.getInputStream();
    }
    
    public TripleCollection transform(){
        TripleCollection resultGraph = new SimpleMGraph();
        // OSM ways (streets, roads)
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Node wayNode = wayList.item(i);
            if (wayNode.getNodeType() == Node.ELEMENT_NODE) {
                Element wayElement = (Element) wayNode;
                String wayId = wayElement.getAttribute("id");
                if( Integer.parseInt(wayId) > 0 ) {
                    UriRef wayUri = new UriRef("http://fusepoolp3.eu/osm/way/" + wayId);
                    resultGraph.add(new TripleImpl(wayUri, RDFS.label, new PlainLiteralImpl(wayId)));
                    resultGraph.add(new TripleImpl(wayUri, RDF.type, new UriRef("http://linkedgeodata.org/ontology/HighwayThing")));
                }
            }
        }
        
        // Gets all OSM nodes then search (binary search) for those that are referenced in a way (points of a line)
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < wayList.getLength(); i++) {
            Node nodeNode = wayList.item(i);
            if (nodeNode.getNodeType() == Node.ELEMENT_NODE) {
                Element nodeElement = (Element) nodeNode;
                String nodeId = nodeElement.getAttribute("id");
                if( Integer.parseInt(nodeId) > 0 ) {
                    // add the node to a list to look for those that are referenced in a OSM way
                }
            }
        }
        
        
        return resultGraph;
    }
    
    
}
