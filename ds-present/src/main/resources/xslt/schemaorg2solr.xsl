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

  <xsl:variable name="schemaorg-xml" as="item()*">
    <xsl:copy-of select="f:parse-json($schemaorgjson)"/>
  </xsl:variable>

  <xsl:template match="/">
    <xsl:variable name="solrjson">
      <f:map>
        <f:string key="id">
          <xsl:value-of select="$schemaorg-xml('id')"/>
        </f:string>




        <f:string key="keys">
          <xsl:value-of select="map:keys($schemaorg-xml('identifier'))"/>
        </f:string>

        <f:string key="test-origin">
          <xsl:value-of select="map:find($schemaorg-xml, 'PropertyID')"/>
        </f:string>

        <f:string key="origin">
          <xsl:value-of select="$schemaorg-xml('identifier')(1)('value')"/>
        </f:string>
      </f:map>
    </xsl:variable>



    <xsl:value-of select="f:xml-to-json($solrjson)"/>
  </xsl:template>
</xsl:transform>
