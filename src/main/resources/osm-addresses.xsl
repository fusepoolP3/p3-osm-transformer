<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <!--
   Extracts addresses from  OpenStreetMap XML data (nodes and ways) and maps them into RDF.
   Address information might be available in nodes and polygons (closed ways).  
   version 20150713_1
   Author: Luigi Selmi
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
    <xsl:variable name="nochar"><xsl:text></xsl:text></xsl:variable>
       <!-- Addresses from nodes-->
       <xsl:for-each select="node">
         <xsl:variable name="id" select="@id"/>
         <xsl:variable name="tagAddress" select="tag[@k='addr:street']"/>
         <xsl:variable name="tagHouseNumber" select="tag[@k='addr:housenumber']"/>
         <xsl:variable name="tagCity" select="tag[@k='addr:city']"/>
         <xsl:variable name="tagCountry" select="tag[@k='addr:country']"/> 
        <xsl:if test="$id > 0 and $tagAddress/@v != '' and $tagHouseNumber/@v != ''">
           &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:PostalAddress ;
           schema:streetAddress "<xsl:value-of select="translate($tagAddress/@v,$double_quote,$nochar)"/><xsl:text> </xsl:text><xsl:value-of select="$tagHouseNumber/@v"/>" ;
           rdfs:seeAlso &lt;http://www.openstreetmap.org/node/<xsl:value-of select="@id"/>&gt; ;
           <xsl:if test="$tagCity/@v != ''">
           schema:addressLocality "<xsl:value-of select="$tagCity/@v"/>" ;
           </xsl:if>
           <xsl:if test="$tagCountry/@v != ''">
           schema:addressCountry "<xsl:value-of select="$tagCountry/@v"/>" ;
           </xsl:if>
           geo:lat "<xsl:value-of select="@lat"/>"^^xsd:double ;
           geo:long "<xsl:value-of select="@lon"/>"^^xsd:double .
         </xsl:if>
       </xsl:for-each>

       <!-- Addresses from polygons (closed ways with address information) -->
       <xsl:for-each select="way">
         <xsl:variable name="id" select="@id"/>
         <xsl:variable name="tagAddress" select="tag[@k='addr:street']"/>
         <xsl:variable name="tagHouseNumber" select="tag[@k='addr:housenumber']"/>
         <xsl:variable name="tagCity" select="tag[@k='addr:city']"/>
         <xsl:variable name="tagCountry" select="tag[@k='addr:country']"/>
         <xsl:if test="$id > 0 and $tagAddress/@v != '' and $tagHouseNumber/@v != ''">
              &lt;urn:osm:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:PostalAddress ;
              schema:streetAddress "<xsl:value-of select="translate($tagAddress/@v,$double_quote,$nochar)"/><xsl:text> </xsl:text><xsl:value-of select="$tagHouseNumber/@v"/>" ;
              rdfs:seeAlso &lt;http://www.openstreetmap.org/way/<xsl:value-of select="@id"/>&gt; ;                                        
              <xsl:if test="$tagCity/@v != ''">
                schema:addressLocality "<xsl:value-of select="$tagCity/@v"/>" ;
              </xsl:if>
              <xsl:if test="$tagCountry/@v != ''">
                schema:addressCountry "<xsl:value-of select="$tagCountry/@v"/>" ;
              </xsl:if>
              <!-- Takes only the geocoordinates of the first node -->
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
