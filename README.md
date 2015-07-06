OpenStreetMap Geocoding Service
=============================

The component is implemented as a transformer for Open Street Map XML data. It takes as input RDF data with schema:streetAddress property and 
optionally the URL of the OSM data set to search for the addresses. The OSM data is fetched, transformed into RDF and stored in a local 
triple store as a named graph. Only the way elements in the OSM file are used and mapped into RDF. The triple store is indexed whenever a new graph is added to it so that it would be possible to make a keyword 
search. The addresses sent by the client are searched in the triple store in order to retrieve the geographic coordinates. The original RDF 
data and the coordinates are sent back to the client. 

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
The transformer takes as input triples with the schema:streetAddress property 

    <http://example.org/res1> schema:streetAddress "Via Trento 1" .

The value of the scheme:streetAddress property is used as a keyword to search for in the OSM data set whose URL as been provided as a parameter in the request. The data can be provided in two formats

1. OSM XML file, using the 'xml' parameter
2. RDF data, using the 'rdf' parameter

The application caches the RDF data. If no URL for the data set is provided the application looks in the cache.

As an example, the triple above can be used as input data to send in a request to the transformer, after it has been started, with the url of the data set to search for. A data set, giglio_island.osm extracted from OpenStreetMap using JOSM, is provided. A request can be sent using curl to the transformer, waiting for a request at port 7100

    curl -i -X POST -H "Content-Type: text/turtle" -d @input_data.ttl http://localhost:7100/?xml=https://raw.githubusercontent.com/fusepoolP3/p3-osm-transformer/master/src/test/resources/eu/fusepool/p3/osm/giglio_island.osm  

The transformer will parse the XML document, transform the data into RDF and store the result in a local indexed triple store. The address is searched in the local graph and if a street is found (a way in OSM terminology) with a similar name its geometry with the coordinates are added to the street and returned to the client.

    <http://fusepoolp3.eu/osm/way/61132582>
        <http://schema.org/streetAddress>
                "Via Trento" ;
        <http://www.opengis.net/ont/geosparql#geometry>
                <http://fusepoolp3.eu/osm/geometry/61132582> .

    <http://fusepoolp3.eu/osm/geometry/61132582>
        <http://www.opengis.net/ont/geosparql#asWKT>
                "LineString((10.9190917 42.3598746, 10.9189831 42.3597034, 10.9188791 42.3596448, 10.9187486 42.3595527, 10.9186877 42.3594989))" .    
