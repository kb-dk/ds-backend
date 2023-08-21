<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:t="http://www.test.com/"
               xmlns:m="http://www.loc.gov/mods/v3"
               xmlns:mets="http://www.loc.gov/METS/"
               xmlns:tei="http://www.tei-c.org/ns/1.0"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:premis="http://www.loc.gov/premis/v3"
               xmlns:mix="http://www.loc.gov/mix/v20"
               xmlns:pbc="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
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

  <xsl:template match="/" >
    <xsl:variable name="solrjson">
      <f:map>

        <f:string key="id">
          <xsl:value-of select="xip:DeliverableUnit/DeliverableUnitRef"/>
        </f:string>
        <!-- Not sure if this accession ref is correctly understood-->
        <f:string key="accession_number">
          <xsl:value-of select="/xip:DeliverableUnit/AccessionRef"/>
        </f:string>
        <f:string key="collection">
          <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatLocation"/>
        </f:string>

        <!-- TODO: Check how genre is specified in mods2solr transformation  -->
        <xsl:if test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreGenre">
          <f:array key="genre">
            <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreGenre">
              <f:string>
                <xsl:value-of select="normalize-space(.)"/>
              </f:string>
            </xsl:for-each>
          </f:array>
        </xsl:if>

        <f:string key="resource_description">
          <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatMediaType"/>
        </f:string>


        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreTitle">
          <xsl:if test="titleType = 'titel'">
            <f:string key="title">
              <xsl:value-of select="title"/>
            </f:string>
          </xsl:if>
          <xsl:if test="titleType = 'originaltitel'">
            <f:string key="original_title">
              <xsl:value-of select="title"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>

        <!--TODO: Figure out what the difference is between kanalnavn and channel_name in the metadata. -->
        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcorePublisher">
          <xsl:if test="publisherRole = 'kanalnavn'">
            <f:string key="creator_affiliation">
              <xsl:value-of select="publisher"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>

        <f:string key="notes">
          <xsl:value-of select="normalize-space(/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreDescription/description)"/>
        </f:string>

        <!-- Video specific transformations -->
        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreIdentifier">
          <xsl:if test="identifierSource = 'ritzauId'">
            <f:string key="ritzau_id">
              <xsl:value-of select="identifier"/>
            </f:string>
          </xsl:if>
          
          <xsl:if test="identifierSource = 'tvmeterId'">
            <f:string key="tvmeter_id">
              <xsl:value-of select="identifier"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>

        <!--Some docs have this duration field. Others doesn't. Then we need to extract the duration from pbcoreDateAvailable-->
        <xsl:choose>
          <xsl:when test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatDuration">
            <f:string key="duration">
              <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatDuration"/>
            </f:string>
          </xsl:when>
          <xsl:otherwise>

            <xsl:variable name="startTime">
              <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart"/>
            </xsl:variable>
            <xsl:variable name="endTime">
              <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd"/>
            </xsl:variable>
            <xsl:variable name="durationInMilliseconds">
              <xsl:value-of select="my:toMilliseconds($startTime, $endTime)"/>
            </xsl:variable>

            <f:string key="duration">
              <xsl:value-of select="$durationInMilliseconds"/>
            </f:string>
          </xsl:otherwise>
        </xsl:choose>



        <xsl:if test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatColors = 'farve'">
          <f:string key="color">true</f:string>
        </xsl:if>
        <xsl:if test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatColors != 'farve'">
          <f:string key="color">false</f:string>
        </xsl:if>

        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreExtension/extension">
          <xsl:if test="f:starts-with(., 'premiere:') and f:contains(., 'ikke premiere')">
            <f:string key="premiere">
              <xsl:value-of select="false()"/>
            </f:string>
          </xsl:if>
          <xsl:if test="f:starts-with(., 'premiere:') and f:contains(., ':premiere')">
            <f:string key="premiere">
              <xsl:value-of select="true()"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>

        <xsl:if test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatAspectRatio">
          <f:string key="aspect_ratio">
            <xsl:value-of select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatAspectRatio"/>
          </f:string>
        </xsl:if>






      </f:map>
    </xsl:variable>


    <xsl:value-of select="f:xml-to-json($solrjson)"/>
  </xsl:template>

  <!-- FUNCTIONS -->
  <!-- Get milliseconds between two dates. -->
  <xsl:function name="my:toMilliseconds" as="xs:integer">
    <xsl:param name="startDate" as="xs:dateTime"/>
    <xsl:param name="endDate" as="xs:dateTime"/>
    <xsl:value-of select="($endDate - $startDate) div xs:dayTimeDuration('PT0.001S')"/>
  </xsl:function>
</xsl:transform>