<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               version="3.0">
  <xsl:output method="text" />

  <!-- Declare all externally provided parameters here -->
  <xsl:param name="record_identifier"/>      <!-- Provided by the Java code that calls the Transformer -->
  <xsl:param name="missing" select="'N/A'"/> <!-- Not provided, but there is a default value 'N/A' -->

  <xsl:template match="/">

    <!-- Create a variable that holds the full output content in the form of a map where keys
         are Solr field-names and values are Solr field-values. Multi-value fields are represented
          using array (see "some_array" below) -->
    <xsl:variable name="json"><f:map>

      <!-- Use the externally provided parameter -->
      <f:string key="id"><xsl:value-of select="$record_identifier"/></f:string>

      <!-- The caller does not define 'missing' so the default 'N/A' will be used instead -->
      <f:string key="optional"><xsl:value-of select="$missing"/></f:string>

      <!-- Plain XSLT work -->
      <xsl:for-each select="//title">
        <f:string key="main_title"><xsl:value-of select="."/></f:string>
      </xsl:for-each>

      <!-- Demonstration of multi-value field -->
      <f:array key="some_array">
        <f:string>content 1</f:string>
        <f:string>content 2</f:string>
      </f:array>

    </f:map></xsl:variable>

    <!-- Output the variable with all the content using the build-in xml-to-json transformation -->
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

</xsl:transform>
