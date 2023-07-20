<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:m="http://www.loc.gov/mods/v3"
               xmlns:mets="http://www.loc.gov/METS/"
               xmlns:t="http://www.tei-c.org/ns/1.0"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:premis="http://www.loc.gov/premis/v3"
               xmlns:mix="http://www.loc.gov/mix/v20"
               version="3.0">


  <xsl:output method="text" indent="no" />
  <xsl:template match="/">

  <xsl:variable name="markdown">
    <xsl:text># Schema documentation</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <xsl:text>## Summary</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <xsl:value-of select="normalize-space(./processing-instruction('summary'))"/>
    <xsl:text>&#10;</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <xsl:text>## Fields</xsl:text>
    <xsl:text>&#10;</xsl:text>
    <!-- Extract documentation for each field-->
    <xsl:for-each select="/schema/field">
      <xsl:text>Field name: </xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>&#10;</xsl:text>
      <xsl:if test="./processing-instruction('description')">
        <xsl:text>Description: </xsl:text>
        <xsl:value-of select="normalize-space(./processing-instruction('description'))"/>
        <xsl:text>&#10;</xsl:text>
      </xsl:if>
      <xsl:if test="./processing-instruction('example')">
        <xsl:for-each select="./processing-instruction('example')">
          <xsl:text>Example: </xsl:text>
          <xsl:value-of select="normalize-space(.)"/>
          <xsl:text>&#10;</xsl:text>
        </xsl:for-each>
      </xsl:if>
      <xsl:text>&#10;</xsl:text>

    </xsl:for-each>

  </xsl:variable>

    <!-- Define output -->
    <xsl:value-of select="$markdown"/>
  </xsl:template>

</xsl:transform>