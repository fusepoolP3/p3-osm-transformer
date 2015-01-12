Fusepool P3 Geocoding Service
=============================

The component is implemented as a transformer for Open Street Map XML data. It takes as input RDF data with schema:streetAddress property and 
optionally the URL of the OSM data set to search for the addresses. The OSM data is fetched, transformed into RDF and stored in a local 
triple store as a named graph. The triple store is indexed whenever a new graph is added to it so that it would be possible to make a keyword 
search. The addresses sent by the client are searched in the triple store in order to retrieve the geographic coordinates. The original RDF 
data and the coordinates are sent back to the client.
