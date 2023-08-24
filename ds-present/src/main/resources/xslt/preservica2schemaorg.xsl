<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:xip="http://example.com/"
               xmlns:pbc="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
               version="3.0">


  <xsl:output method="text"/>

  <xsl:param name="recordID"/>

  <xsl:template match="/">

    <xsl:variable name="json">
      <f:map>
        <!-- TODO: Add logic for selecting more specific type -->
        <f:string key="@type">VideoObject</f:string>
        <f:string key="id">
          <xsl:value-of select="/xip:DeliverableUnit/DeliverableUnitRef"/>
        </f:string>

        <!-- TODO: Investigate relation between titel and originaltitel. Some logic related to metadata delivery type exists. -->
        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreTitle">
          <xsl:if test="titleType = 'titel'">
            <f:array key="headline">
              <f:map>
                <f:string key="value">
                  <xsl:value-of select="./title"/>
                </f:string>
                <!-- TODO: Figure how to determine language for the string "@language"-->
              </f:map>
            </f:array>
          </xsl:if>
          <xsl:if test="titleType = 'originaltitel'">
            <f:array key="alternativeHeadline">
              <f:map>
                <f:string key="value">
                  <xsl:value-of select="./title"/>
                </f:string>
                <!-- TODO: Figure how to determine language for the string "@language"-->
              </f:map>
            </f:array>
          </xsl:if>
        </xsl:for-each>



      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

</xsl:transform>