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
               xmlns:xip="http://example.com/"
               version="3.0">

  <xsl:output method="text" />
  <!-- Declare all externally provided parameters here -->
  <!-- Provided by the Java code that calls the Transformer. (Key,Value) pairs are given in a map -->
  <xsl:param name="access_blokeret"/>
  <xsl:param name="access_pligtafleveret"/>
  <xsl:param name="access_ejermaerke"/>
  <xsl:param name="access_note"/>
  <xsl:param name="access_skabelsesaar"/>
  <xsl:param name="access_ophavsperson_doedsaar"/>
  <xsl:param name="access_searlige_visningsvilkaar"/>
  <xsl:param name="access_materiale_type"/>
  <xsl:param name="access_foto_aftale"/>
  <xsl:param name="access_billede_aftale"/>
  <xsl:param name="access_ophavsret_tekst"/>
  <xsl:param name="imageserver"/>

  <xsl:template match="/">
    <xsl:variable name="json">
      <f:map>
        <!--This is where transformations happen -->



        <xsl:if test="DeliverableUnit/Metadata/pbcoreTitle/pbcoreTitleType = 'originaltitel'">
          <f:string key="titel">
            <xsl:value-of select="xip:DeliverableUnit/Metadata/pbcoreTitle/title"/>
          </f:string>
        </xsl:if>

        <f:string key="test">
          <xsl:value-of select="DeliverableUnit/CatalogueReference"/>
        </f:string>



      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

</xsl:transform>