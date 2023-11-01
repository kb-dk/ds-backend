<?xml version="1.0" encoding="UTF-8" ?>
<!-- When changes are made to this XSLT, please update the general description of the mapping which is available
     in the file: src/main/webapp/mappings_radiotv.html  -->
<xsl:transform xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:map="http://www.w3.org/2005/xpath-functions/map"
               xmlns:array="http://www.w3.org/2005/xpath-functions/array"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               version="3.0">
  
  <xsl:output method="text" />
  <xsl:param name="schemaorgjson"/>

  <!--Saves the input JSON as an XDM object. -->
  <xsl:variable name="schemaorg-xml" as="item()*">
    <xsl:copy-of select="f:parse-json($schemaorgjson)"/>
  </xsl:variable>

  <xsl:template match="/">
    <xsl:variable name="solrjson">
      <f:map>
        <f:string key="resource_description">
          <xsl:value-of select="$schemaorg-xml('@type')"/>
        </f:string>
        <!--Simple ID extraction -->
        <f:string key="id">
          <xsl:value-of select="$schemaorg-xml('id')"/>
        </f:string>
        <f:string key="name">
          <xsl:value-of select="$schemaorg-xml('name')"/>
        </f:string>

        <xsl:if test="$schemaorg-xml('keywords') != ''">
          <!--Save categories to a variable as a sequence. -->
          <xsl:variable name="categories" as="item()*" select="tokenize($schemaorg-xml('keywords'), ',')"/>
          <!--Create array of categories, which fits with the multivalued solr field -->
          <f:array key="categories">
            <xsl:for-each select="$categories">
              <f:string><xsl:value-of select="."/></f:string>
            </xsl:for-each>
          </f:array>
        </xsl:if>

        <f:string key="collection">
          <xsl:value-of select="$schemaorg-xml('isPartOf')('name')"/>
        </f:string>

        <f:string key="genre">
          <xsl:value-of select="$schemaorg-xml('genre')"/>
        </f:string>


        <!--Extract the array of identifiers to a variable, where the individual maps can be accessed. -->
        <xsl:variable name="identifers" as="item()*">
          <xsl:copy-of select="array:flatten($schemaorg-xml('identifier'))"/>
        </xsl:variable>
        <!--For each identifier in the variable $identifiers check for specific properties and map them to their
            respective solr counterpart.-->
        <xsl:for-each select="$identifers">
          <!-- Finds origin -->
          <xsl:if test="map:get(., 'PropertyID') = 'Origin'">
            <f:string key="origin">
              <xsl:value-of select="map:get(., 'value')"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>
      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($solrjson)"/>
  </xsl:template>

</xsl:transform>
