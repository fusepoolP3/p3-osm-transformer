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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    Map<String,OsmWay> osmWayNodeMap = null;
    
    private static final Logger log = LoggerFactory.getLogger(OsmXmlParser.class);
    
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
    /*
     * Retrieves the OSM ways with its nodes and their geographic coordinates. It uses the node list to get the nodes from their reference id.
     */
    public void processXml(){ 
        // Map each way to its referenced nodes
        osmWayNodeMap = new HashMap<String, OsmWay>();         
        // OSM nodes
        NodeList nodeList = doc.getElementsByTagName("node");
        // OSM ways (streets, roads). These are 'way' elements with a 'tag' child element with 'k' attribute set to 'highway'
        // and another 'tag' child element with 'k' attribute set to 'name' that must be non empty (name of the street)
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Node wayNode = wayList.item(i);
            if (wayNode.getNodeType() == Node.ELEMENT_NODE) {
                Element wayElement = (Element) wayNode;
                String wayId = wayElement.getAttribute("id");                
                if( Long.parseLong(wayId) > 0 ) {
                    OsmWay wayObject = new OsmWay();
                    wayObject.setWayId(wayId);
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
                                if( "name".equals(tagK) && ! tagElement.getAttribute("v").isEmpty() ){
                                    isName = true;
                                    wayObject.setTagName(tagElement.getAttribute("v"));
                                }
                            }
                        }
                    }
                    if(isHighway & isName){                        
                        // Get the way's referenced nodes. The order of the referenced nodes is preserved. 
                        NodeList refList = wayElement.getElementsByTagName("nd");
                        if(refList != null){                            
                            for (int j = 0; j < refList.getLength(); j++) {
                                Node refNode = refList.item(j);                        
                                if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element refElement = (Element) refNode;
                                    String refId = refElement.getAttribute("ref");                                    
                                    // Look for the node by its reference
                                    for (int k = 0; k < nodeList.getLength(); k++) {
                                        Node nodeNode = nodeList.item(k);
                                        if (nodeNode.getNodeType() == Node.ELEMENT_NODE) {
                                            Element nodeElement = (Element) nodeNode;
                                            String nodeId = nodeElement.getAttribute("id");
                                            if( refId.equals(nodeId) ) {
                                                double nodeLat = Double.parseDouble(nodeElement.getAttribute("lat"));
                                                double nodeLon = Double.parseDouble(nodeElement.getAttribute("lon"));
                                                OsmNode osmNode = new OsmNode();
                                                osmNode.setId(nodeId);
                                                osmNode.setLat(nodeLat);
                                                osmNode.setLong(nodeLon);
                                                wayObject.getNodeReferenceList().add(osmNode);                                                
                                            }
                                        }
                                    }
                                }
                            }
                            osmWayNodeMap.put(wayId, wayObject);
                        }         
                    }
                }
            }
        }
        
        // Print the nodes that are referenced in a way (points of a line) and gets the geographic coordinates
        for(String way:  osmWayNodeMap.keySet()) {
            log.debug("Way id = " + way);
            OsmWay wayObj = osmWayNodeMap.get(way);
            for(Iterator<OsmNode> nodeIter = wayObj.getNodeReferenceList().iterator(); nodeIter.hasNext();){                
                OsmNode node = nodeIter.next();
                log.debug("Referenced node: " + node.getId() + " Lat. " + node.getLat() + " Lon. " + node.getLong());
            }
           
        }
        
        
    }
    
    /*
     * Retrieves the OSM ways with its nodes and their geographic coordinates. It does a binary search to get the nodes by their id.
     */
    public void processXmlBinary(){ 
        // Map each way to its referenced nodes
        osmWayNodeMap = new HashMap<String, OsmWay>();  
        // List of the Ids of all the nodes that are part of ways
        List<String> osmWayNodeList = new ArrayList<String>(); 
        // OSM ways (streets, roads). These are 'way' elements with a 'tag' child element with 'k' attribute set to 'highway'
        // and another 'tag' child element with 'k' attribute set to 'name' that must be non empty (name of the street)
        long time = System.currentTimeMillis();
        log.info("start search way and their nodes.." + time);
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Node wayNode = wayList.item(i);
            if (wayNode.getNodeType() == Node.ELEMENT_NODE) {
                Element wayElement = (Element) wayNode;
                String wayId = wayElement.getAttribute("id");                
                if( Long.parseLong(wayId) > 0 ) {
                    OsmWay wayObject = new OsmWay();
                    wayObject.setWayId(wayId);                
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
                                if( "name".equals(tagK) && ! tagElement.getAttribute("v").isEmpty() ){
                                    isName = true;
                                    wayObject.setTagName(tagElement.getAttribute("v"));
                                }
                            }
                        }
                    }
                    if(isHighway & isName){                        
                        // Get the way's referenced nodes. The order of the referenced nodes is preserved. 
                        NodeList refList = wayElement.getElementsByTagName("nd");
                        if(refList != null){                            
                            for (int j = 0; j < refList.getLength(); j++) {
                                Node refNode = refList.item(j);                        
                                if (refNode.getNodeType() == Node.ELEMENT_NODE) {
                                    Element refElement = (Element) refNode;
                                    String refId = refElement.getAttribute("ref");    
                                    // add the node to the list of the ways' nodes
                                    osmWayNodeList.add(refId);
                                    // add the node to a list to look for those that are referenced in a OSM way
                                    OsmNode osmNode = new OsmNode();
                                    osmNode.setId(refId);
                                    wayObject.getNodeReferenceList().add(osmNode);
                                }
                            }
                            osmWayNodeMap.put(wayId, wayObject);
                        }         
                    }
                }
            }
        }
        long time2 = System.currentTimeMillis();
        log.info("Search done.." + (time2 - time)/1000.0);
        
        // Sort the list of the ids of nodes that are part of a way
        Collections.sort(osmWayNodeList);
        // Number of all the nodes that are part of a way
        int osmWayNodesNumber = osmWayNodeList.size();
        log.info("Number of referenced nodes: " + osmWayNodesNumber);
        
        // List of all the OSM nodes 
        List<OsmNode> osmNodeList = new ArrayList<OsmNode>();
        
        // OSM nodes with geographic coordinates
        NodeList nodeList = doc.getElementsByTagName("node");
        int osmWayNodesCounter = osmWayNodesNumber;
        log.info("Collecting all the nodes and their geographic coordinates.."); // this is the bottleneck
        // Puts all OSM nodes in an Array List to be sorted
        for (int k = 0; k < nodeList.getLength() && osmWayNodesCounter > 0; k++) {
            Node nodeNode = nodeList.item(k);
            Element nodeElement = (Element) nodeNode;
            String nodeId = nodeElement.getAttribute("id");  
            int wayNodeIndex = Collections.binarySearch(osmWayNodeList, nodeId);
            if( wayNodeIndex >= 0){
                osmWayNodesCounter--;
                osmWayNodeList.remove(wayNodeIndex);               
                // adds the node to a list to look for those that are referenced in a OSM way
                double lat = Double.parseDouble(nodeElement.getAttribute("lat"));
                double lon = Double.parseDouble(nodeElement.getAttribute("lon"));
                OsmNode nodeObj = new OsmNode();
                nodeObj.setId(nodeId);
                nodeObj.setLat(lat);
                nodeObj.setLong(lon);
                osmNodeList.add(nodeObj);
            }
        }
        
        long time3 = System.currentTimeMillis();
        log.info("Collection done in " + (time3 - time2)/1000.0 + " sec.");
        log.info("Sorting the nodes..");
        // Sort the OSM nodes by their id
        Collections.sort(osmNodeList);
        
        long time4 = System.currentTimeMillis();
        log.info("Sort done in " + (time4 - time3)/1000.0 + " sec.");
        log.info("Search the nodes of each way..");
        
        // Search the nodes that are referenced in a way (points of a line) and gets the geographic coordinates
        for(String way:  osmWayNodeMap.keySet()) {
            if(log.isDebugEnabled())
                log.debug("Way id = " + way);
            OsmWay wayObj = osmWayNodeMap.get(way);
            for(Iterator<OsmNode> nodeIter = wayObj.getNodeReferenceList().iterator(); nodeIter.hasNext();){
                OsmNode wayNode = nodeIter.next();
                int nodeIndex = Collections.binarySearch(osmNodeList, wayNode);
                OsmNode node = osmNodeList.get(nodeIndex);
                if(log.isDebugEnabled())
                    log.debug("Referenced node: " + node.getId() + " Lat. " + node.getLat() + " Lon. " + node.getLong());
            }
            
        }
        
        long time5 = System.currentTimeMillis();
        
        log.info("Search done in " + (time5 - time4)/1000.0 + " sec.");
        
        
    }
    
    public TripleCollection transform(){
        TripleCollection resultGraph = new SimpleMGraph();
        processXml();
        //UriRef wayUri = new UriRef("http://fusepoolp3.eu/osm/way/" + wayId);
        //resultGraph.add(new TripleImpl(wayUri, RDFS.label, new PlainLiteralImpl(wayId)));
        //resultGraph.add(new TripleImpl(wayUri, RDF.type, new UriRef("http://linkedgeodata.org/ontology/HighWay")));
        return resultGraph;
    }
    
    
}
