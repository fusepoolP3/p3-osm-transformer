package eu.fusepool.p3.osm;


import java.util.ArrayList;
import java.util.List;

public class OsmWay {
    
    String wayId;
    List<OsmNode> refList = new ArrayList<OsmNode>();
    String tagName;
    
    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagname) {
        this.tagName = tagname;
    }
    
    public List<OsmNode> getNodeReferenceList(){
        return refList;
    }
    
    public void setWayId(String id){
        wayId = id;
    }
    
    public String getWayId(){
        return wayId;
    }
    
    

}
