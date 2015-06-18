<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!--
   Transforms OpenStreetMap XML data into RDF 
   version 20150325_1
  -->

  <xsl:output method="text" media-type="text/turtle" encoding="UTF-8"/>

  <!--xsl:strip-space elements="*"/-->

  <xsl:template match="/">
# XSLT transformation of OSM XML data for ways and nodes.

@prefix rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .
@prefix rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt; .
@prefix geo: &lt;http://www.w3.org/2003/01/geo/wgs84_pos#&gt; .
@prefix xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt; .
@prefix schema: &lt;http://schema.org/&gt; .
@prefix ogc: &lt;http://www.opengis.net/ont/geosparql#&gt; .
@prefix dcterms: &lt;http://purl.org/dc/terms/&gt; .


    <xsl:apply-templates select="osm"/>
  </xsl:template>

  <xsl:key name="nodes" match="node" use="@id"/>

  <xsl:template match="osm">
    <xsl:variable name="double_quote"><xsl:text>"</xsl:text></xsl:variable>
    <xsl:variable name="apos"><xsl:text>'</xsl:text></xsl:variable>
       <!-- Nodes -->
       <!-- Public Transport -->
       <xsl:for-each select="node">
         <xsl:variable name="id" select="@id"/>
         <xsl:variable name="tagKey" select="tag[@k='public_transport']"/>
         <xsl:variable name="tagKeyValue" select="$tagKey/@v"/>
         <xsl:variable name="tagName" select="tag[@k='name']"/>
         <xsl:if test="$id > 0 and $tagKeyValue = 'stop_position'">
           &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:BusStop ;
           rdfs:label "<xsl:value-of select="$tagName/@v"/>" ;
           rdfs:seeAlso &lt;http://www.openstreetmap.org/node/<xsl:value-of select="@id"/>&gt; ;
           geo:lat "<xsl:value-of select="@lat"/>"^^xsd:double ;
           geo:long "<xsl:value-of select="@lon"/>"^^xsd:double .
         </xsl:if>
       </xsl:for-each>
       
       <!-- Ways -->
       <xsl:for-each select="way">
         <xsl:variable name="id" select="@id"/>
         <xsl:variable name="tagKey" select="tag[@k='amenity']"/>
         <xsl:variable name="tagKeyValue" select="$tagKey/@v"/>
         <xsl:variable name="tagName" select="tag[@k='name']"/>

         <!-- Places of Worship -->
         <xsl:if test="$id > 0 and $tagKeyValue = 'place_of_worship' and $tagName/@v != ''">
              &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:PlaceOfWorship ;
              dcterms:identifier "<xsl:value-of select="@id"/>" ;
              <xsl:variable name="name" select="$tagName/@v"/>
              rdfs:label "<xsl:value-of select="translate($name,$double_quote,$apos)"/>" ;   
              rdfs:seeAlso &lt;http://www.openstreetmap.org/way/<xsl:value-of select="@id"/>&gt; ;                                         
              <xsl:for-each select="nd[1]">
                <xsl:variable name="node_ref" select="@ref"/>
                <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                geo:lat "<xsl:value-of select="$node_key/@lat"/>"^^xsd:double ;
                geo:long "<xsl:value-of select="$node_key/@lon"/>"^^xsd:double .                  
              </xsl:for-each>  
              <xsl:variable name="raw_polygon" >
                  <xsl:for-each select="nd">
                    <xsl:variable name="node_ref" select="@ref"/>
                    <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                    <xsl:variable name="lon" select="$node_key/@lon"/>
                    <xsl:variable name="lat" select="$node_key/@lat"/>
                    <xsl:value-of select="$lon"/><xsl:text>_</xsl:text><xsl:value-of select="$lat"/>,
                  </xsl:for-each>
                </xsl:variable>
              <xsl:variable name="nowhitespace_polygon" select="translate($raw_polygon,'&#10;&#32;','')"/>
              <xsl:variable name="nounderscore_polygon" select="translate($nowhitespace_polygon,'&#95;',' ')"/>
              <xsl:variable name="polygon" select="substring($nounderscore_polygon,1,string-length($nounderscore_polygon)-1)"/>
              &lt;urn:osm:way:geometry:uuid:<xsl:value-of select="$id"/>&gt; ogc:asWKT "Polygon ((<xsl:value-of select="$polygon"/>))" .                                       
         </xsl:if>

         <!-- Schools -->
         <xsl:if test="$id > 0 and $tagKeyValue = 'school' and $tagName/@v != ''">
              &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:School ;
              dcterms:identifier "<xsl:value-of select="@id"/>" ;
              <xsl:variable name="name" select="$tagName/@v"/>
              rdfs:label "<xsl:value-of select="translate($name,$double_quote,$apos)"/>" ;    
              rdfs:seeAlso &lt;http://www.openstreetmap.org/way/<xsl:value-of select="@id"/>&gt; ;                                        
              <xsl:for-each select="nd[1]">
                <xsl:variable name="node_ref" select="@ref"/>
                <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                geo:lat "<xsl:value-of select="$node_key/@lat"/>"^^xsd:double ;
                geo:long "<xsl:value-of select="$node_key/@lon"/>"^^xsd:double .                  
              </xsl:for-each>                                         
         </xsl:if>

         <!-- Restaurants -->
         <xsl:if test="$id > 0 and $tagKeyValue = 'restaurant' and $tagName/@v != ''">
              &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:Restaurant ;
              dcterms:identifier "<xsl:value-of select="@id"/>" ;
              <xsl:variable name="name" select="$tagName/@v"/>
              rdfs:label "<xsl:value-of select="translate($name,$double_quote,$apos)"/>" ; 
              rdfs:seeAlso &lt;http://www.openstreetmap.org/way/<xsl:value-of select="@id"/>&gt; ;                                           
              <xsl:for-each select="nd[1]">
                <xsl:variable name="node_ref" select="@ref"/>
                <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                geo:lat "<xsl:value-of select="$node_key/@lat"/>"^^xsd:double ;
                geo:long "<xsl:value-of select="$node_key/@lon"/>"^^xsd:double .                  
              </xsl:for-each>                                         
         </xsl:if>

         <!-- Museums -->
         <xsl:variable name="tagKeyTourism" select="tag[@k='tourism']"/>
         <xsl:if test="$id > 0 and $tagKeyTourism/@v = 'museum' and $tagName/@v != ''">
              &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:Museum ;
              dcterms:identifier "<xsl:value-of select="@id"/>" ;
              <xsl:variable name="name" select="$tagName/@v"/>
              rdfs:label "<xsl:value-of select="translate($name,$double_quote,$apos)"/>" ;    
              rdfs:seeAlso &lt;http://www.openstreetmap.org/way/<xsl:value-of select="@id"/>&gt; ;                                        
              <xsl:for-each select="nd[1]">
                <xsl:variable name="node_ref" select="@ref"/>
                <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                geo:lat "<xsl:value-of select="$node_key/@lat"/>"^^xsd:double ;
                geo:long "<xsl:value-of select="$node_key/@lon"/>"^^xsd:double .                  
              </xsl:for-each>                                         
         </xsl:if>


       </xsl:for-each>
      

  </xsl:template>


</xsl:stylesheet>
