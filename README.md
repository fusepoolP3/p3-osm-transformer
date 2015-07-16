OpenStreetMap Geocoding Service
=============================

The component is implemented as a transformer for Open Street Map XML data. It takes as input RDF data with the address to be geocoded specified with

1. name of the street and civic number
2. locality
3. country code

The name of the street must be provided following the format used by the postal service of the country. The country code must be provided in two digit ISO format (e.g. "IT" for Italy). Optionally the URL of the OSM data set to search for the addresses can be provided as a URL paramenter (xml=<data set url>). The OSM data is fetched, transformed into RDF and stored in a local triple store as a named graph. The transformer extracts from the OSM/XML data nodes and ways that contain address information in the tags. Nodes and Ways elements that have the following attributes are extracted and mapped into RDF using the schema.org vocabulary. 

- k='addr:street'  
- k='addr:housenumber' 
- k='addr:city'
- k='addr:country'

The addr:street attribute value with  addr:housenumber are joined together following the standard used by the postal service of the country and mapped as value of the schema:streetAddress. The other two attributes addr:city and addr:country are mapped into schema:addressLocality and schema:addressCountry. The address to be geocoded must be provided in RDF using the same schema.org terms as in the example

    @prefix schema: <http://schema.org/> .

    <> schema:streetAddress "Via Thaon de Revel 20" ;
       schema:addressLocality "Giglio Porto" ;
       schema:addressCountry "IT" .

The triple store is indexed whenever a new graph is added to it so that it is possible to make a keyword search. The addresses sent by the client are searched in the triple store in order to retrieve the geographic coordinates. The original RDF data and the coordinates are sent back to the client. 

## Try it out
Compile and start the transformer as described in the next section. The following command invokes the transformer on some test-data:

    curl -i -X POST -H "Content-Type: text/turtle" -d @input_data.ttl http://localhost:7100/?xml=https://raw.githubusercontent.com/fusepoolP3/p3-osm-transformer/master/src/test/resources/eu/fusepool/p3/osm/giglio_island.osm  

The needed input_data.ttl can be found in the sources or directly downloaded from [here](https://github.com/fusepoolP3/p3-osm-transformer/blob/master/src/test/resources/eu/fusepool/p3/osm/input_data.ttl).

## Compiling and Running
After obtaining the sourcecode, compile the project using maven:

    mvn install

Then, start the transformer:

    mvn exec:java

 
## Usage
The transformer takes as input an address described as in the example above. The value of the scheme:streetAddress property is used as a keyword to search for in the OSM data set whose URL as been provided as a parameter in the request. The application caches the RDF data. If no URL for the data set is provided the application looks in the cache. As an example, the address in the example can be used as input data to send a request to the transformer, after it has been started, with the url of the data set to search for. A data set, giglio_island.osm extracted from OpenStreetMap using JOSM, is provided. A request can be sent using curl to the transformer, waiting for a request at port 7100

    curl -i -X POST -H "Content-Type: text/turtle" -d @input_data.ttl http://localhost:7100/?xml=https://raw.githubusercontent.com/fusepoolP3/p3-osm-transformer/master/src/test/resources/eu/fusepool/p3/osm/giglio_island.osm  

The transformer will parse the XML document, transform the data into RDF and store the result in a local indexed triple store. The address is searched in the local graph and if an address is found it will be returned to the client with its geocoordinates.

    <urn:osm:uuid:848724518>
        <http://schema.org/addressCountry>
                "IT" ;
        <http://schema.org/addressLocality>
                "Giglio Porto" ;
        <http://schema.org/streetAddress>
                "Via Thaon de Revel 20" ;
        <http://www.w3.org/2003/01/geo/wgs84_pos#lat>
                "42.3600902" ;
        <http://www.w3.org/2003/01/geo/wgs84_pos#lon>
                "10.9195731" .
.    
