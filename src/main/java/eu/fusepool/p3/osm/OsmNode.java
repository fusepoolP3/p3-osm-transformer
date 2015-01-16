package eu.fusepool.p3.osm;

public class OsmNode implements Comparable<OsmNode> {

    private double latitude;
    private double longitude;
    private String uriName;
    private String id;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUriName(){
        return uriName;
    }
    
    public void setUri(String uriName){
        this.uriName = uriName;
    }
    
    
    public double getLat() {
        return latitude;
    }
    
    public void setLat(double lat) {
        this.latitude = lat;
    }
    
    public double getLong() {
        return longitude;
    }
    
    public void setLong(double longitude) {
        this.longitude = longitude;
    }
    
    public int compareTo(OsmNode node){
        return id.compareTo(node.getId());
    }
    // Two nodes are equal if they have the same geographic coordinates 
    public boolean equals(Object o){
        boolean isSame = false;
        if( ! (o instanceof OsmNode) )
            return false;
        OsmNode node = (OsmNode) o;
        return ( latitude == node.getLat() && longitude == node.getLong() );
    }
    
        
}
