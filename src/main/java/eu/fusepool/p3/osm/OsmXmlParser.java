package eu.fusepool.p3.osm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

/**
 * Transforms a OpenStreetMap XML file into RDF following the schema specified in http://wiki.openstreetmap.org/wiki/OSM_XML
 * @author luigi
 *
 */
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
        
        // Map each way to its referenced nodes
        Map<String,List<String>> osmWayNodeMap = new HashMap<String, List<String>>(); 
        
        // OSM ways (streets, roads). These are 'way' elements with a 'tag' child element with 'k' attribute set to 'highway'
        // and another 'tag' child element with 'k' attribute set to 'name' that must be non empty (name of the street)
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Node wayNode = wayList.item(i);
            if (wayNode.getNodeType() == Node.ELEMENT_NODE) {
                Element wayElement = (Element) wayNode;
                String wayId = wayElement.getAttribute("id");                
                if( Long.parseLong(wayId) > 0 ) {
                    boolean isHighway = false;
                    boolean isName = false;
                    // Check the tags sub elements to see whether it is a highway with a name
                    NodeList tagList = wayElement.getElementsByTagName("tag");
                    if(tagList != null){
                        for (int j = 0; j < tagList.getLength(); j++) {
                            Node tagNode = tagList.item(j);                        
                            if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
                                Element tagElement = (Element) tagNode;
                                String tagK = tagElement.getAttribute("k");
                                if("highway".equals(tagK))
                                    isHighway = true;
                                if( "name".equals(tagK) && ! tagElement.getAttribute("v").isEmpty() )
                                    isName = true;                   
                            }
                        }
                    }
                    if(isHighway & isName){
                        UriRef wayUri = new UriRef("http://fusepoolp3.eu/osm/way/" + wayId);
                        resultGraph.add(new TripleImpl(wayUri, RDFS.label, new PlainLiteralImpl(wayId)));
                        resultGraph.add(new TripleImpl(wayUri, RDF.type, new UriRef("http://linkedgeodata.org/ontology/HighwayThing")));
                        
                        // Get the way's referenced nodes. The order of the referenced nodes is preserved. 
                        NodeList refList = wayElement.getElementsByTagName("nd");
                        if(refList != null){
                            List<String> wayRefList = new ArrayList<String>();
                            for (int j = 0; j < refList.getLength(); j++) {
                                Node refNode = refList.item(j);                        
                                if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element refElement = (Element) refNode;
                                    String refId = refElement.getAttribute("ref");
                                    wayRefList.add(refId);
                                }
                            }
                            osmWayNodeMap.put(wayId, wayRefList);
                        }         
                    }
                }
            }
        }
        
        // List of all the OSM nodes
        List<String> osmNodeList = new ArrayList<>();
       
        // Gets all OSM nodes then search (binary search) for those that are referenced in a way (points of a line)
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int k = 0; k < nodeList.getLength(); k++) {
            Node nodeNode = nodeList.item(k);
            if (nodeNode.getNodeType() == Node.ELEMENT_NODE) {
                Element nodeElement = (Element) nodeNode;
                String nodeId = nodeElement.getAttribute("id");
                if( Long.parseLong(nodeId) > 0 ) {
                    // add the node to a list to look for those that are referenced in a OSM way
                    osmNodeList.add(nodeId);
                }
            }
        }
        
        // Sort the OSM nodes by their id
        Collections.sort(osmNodeList);
       
        // Search the nodes that are referenced in a way (points of a line)
        for(String way:  osmWayNodeMap.keySet()) {
            System.out.println("Way id = " + way);
            List<String> wayRefList = osmWayNodeMap.get(way);
            for(Iterator<String> nodeIter = wayRefList.iterator(); nodeIter.hasNext();){
                int nodeId = Collections.binarySearch(osmNodeList, nodeIter.next());
                System.out.println("referenced node: " + osmNodeList.get(nodeId) );
            }
            System.out.println();
        }
        
        return resultGraph;
    }
    
    
}
