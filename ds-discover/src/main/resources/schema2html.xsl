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


  <xsl:output method="html"/>
  <xsl:template match="/">

    <html>
      <body>
        <h1>Schema documentation</h1>
        <h2>Summary</h2>
        <p><xsl:value-of select="normalize-space(./processing-instruction('summary'))"/></p>
        <h2>Fields</h2>
        <!-- Extract documentation for each field-->
        <xsl:for-each select="/schema/field">
          <h3>Field:</h3>
          <p>Name: <xsl:value-of select="@name"/> <br/>
            <xsl:if test="./processing-instruction('description')">
              Description: <xsl:value-of select="normalize-space(./processing-instruction('description'))"/> <br/>
            </xsl:if>
            <xsl:if test="./processing-instruction('example')">
              <xsl:for-each select="./processing-instruction('example')">
                Example: <xsl:value-of select="normalize-space(.)"/> <br/>
              </xsl:for-each>
            </xsl:if>
          </p>
        </xsl:for-each>
      </body>
    </html>
  </xsl:template>

</xsl:transform>