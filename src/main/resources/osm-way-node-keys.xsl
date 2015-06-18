<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

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

    <xsl:apply-templates select="osm"/>
  </xsl:template>

  <xsl:key name="nodes" match="node" use="@id"/>

  <xsl:template match="osm">
    <xsl:variable name="double_quote"><xsl:text>"</xsl:text></xsl:variable>
    <xsl:variable name="apos"><xsl:text>'</xsl:text></xsl:variable>
       <xsl:for-each select="way">
         <xsl:variable name="id" select="@id"/>
         <xsl:variable name="tagHighway" select="tag[@k='highway']"/>
         <xsl:variable name="tagName" select="tag[@k='name']"/>
        <xsl:if test="$id > 0 and $tagHighway and $tagName/@v != ''">
              &lt;urn:osm:way:uuid:<xsl:value-of select="@id"/>&gt; rdf:type schema:PostalAddress ;
              <xsl:variable name="street" select="$tagName/@v"/>
                schema:streetAddress "<xsl:value-of select="translate($street,$double_quote,$apos)"/>" ;            
                ogc:geometry &lt;urn:osm:way:geometry:uuid:<xsl:value-of select="$id"/>&gt; .
                <xsl:variable name="raw_linestring" >
                  <xsl:for-each select="nd">
                    <xsl:variable name="node_ref" select="@ref"/>
                    <xsl:variable name="node_key" select="key('nodes',$node_ref)"/>
                    <xsl:variable name="lon" select="$node_key/@lon"/>
                    <xsl:variable name="lat" select="$node_key/@lat"/>
                    <xsl:value-of select="$lon"/><xsl:text>_</xsl:text><xsl:value-of select="$lat"/>,
                  </xsl:for-each>
                </xsl:variable>
              <xsl:variable name="nowhitespace_linestring" select="translate($raw_linestring,'&#10;&#32;','')"/>
              <xsl:variable name="nounderscore_linestring" select="translate($nowhitespace_linestring,'&#95;',' ')"/>
              <xsl:variable name="linestring" select="substring($nounderscore_linestring,1,string-length($nounderscore_linestring)-1)"/>
              &lt;urn:osm:way:geometry:uuid:<xsl:value-of select="$id"/>&gt; ogc:asWKT "LineString((<xsl:value-of select="$linestring"/>))" .
        </xsl:if>


       </xsl:for-each>

  </xsl:template>





</xsl:stylesheet>
