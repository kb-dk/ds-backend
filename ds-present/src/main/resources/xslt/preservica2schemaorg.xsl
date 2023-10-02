<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:xip="http://www.tessella.com/XIP/v4"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:pbc="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
               version="3.0">


  <xsl:output method="text"/>

  <xsl:param name="streamingserver"/>
  <xsl:param name="origin"/>
  <xsl:param name="recordID"/>

  <xsl:template match="/">

    <xsl:variable name="json">
      <f:map>
        <!-- TODO: Add logic for selecting more specific type -->
        <!-- TODO: Change default to VideoObject and create About field with broadcast information -->
        <f:string key="@type">VideoObject</f:string>
        <f:string key="id">
            <xsl:value-of select="$recordID"/>
        </f:string>

        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument">
          <xsl:call-template name="pbc-metadata"/>
        </xsl:for-each>

      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

  <!-- TEMPLATE FOR ACCESSING PBC METADATA. CALLED ABOVE-->
  <xsl:template name="pbc-metadata">
    <!-- TODO: Investigate relation between titel and originaltitel. Some logic related to metadata delivery type exists. -->
    <!-- Create fields headline and alternativeHeadline if needed.
         Determine if title and original title are alike. Both fields should always be in metadata -->
    <xsl:variable name="title">
      <xsl:value-of select="pbcoreTitle[1]/title"/>
    </xsl:variable>
    <xsl:variable name="original-title">
      <xsl:value-of select="pbcoreTitle[2]/title"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$title = $original-title or ($title != '' and $original-title = '')">
        <f:array key="name">
          <f:map>
            <f:string key="value">
              <xsl:value-of select="$title"/>
            </f:string>
            <!-- TODO: Figure how to determine language for the string "@language"-->
          </f:map>
        </f:array>
      </xsl:when>
      <xsl:otherwise>
        <f:array key="name">
          <f:map>
            <f:string key="value">
              <xsl:value-of select="$title"/>
            </f:string>
            <!-- TODO: Figure how to determine language for the string "@language"-->
          </f:map>
        </f:array>
        <f:array key="alternateName">
          <f:map>
            <f:string key="value">
              <xsl:value-of select="$original-title"/>
            </f:string>
            <!-- TODO: Figure how to determine language for the string "@language"-->
          </f:map>
        </f:array>
      </xsl:otherwise>
    </xsl:choose>

    <!-- TODO: Ellaborate this as specified in https://schema.org/BroadcastEvent -->
    <!-- Publisher extraction. Some metadata has two pbcorePublisher/publisher/publisherRole.
         We use the one with the value "kanalnavn" as this should be present in all metadata files.-->
    <xsl:for-each select="pbcorePublisher">
      <xsl:if test="./publisherRole = 'kanalnavn'">
        <f:map key="publishedOn">
          <f:string key="@type">BroadcastService</f:string>
          <f:string key="broadcastDisplayName">
            <xsl:value-of select="./publisher"/>
          </f:string>
        </f:map>
      </xsl:if>
    </xsl:for-each>

    <!-- Create about field from langomtale1 -->
    <!-- TODO: Investigate relation between langomtale1, langomtale2 and kortomtale -->
    <xsl:for-each select="pbcoreDescription">
      <xsl:if test="./descriptionType = 'langomtale1'">
        <f:string key="description">
          <xsl:value-of select="normalize-space(./description)"/>
        </f:string>
      </xsl:if>
    </xsl:for-each>

    <!-- Extract start and end times for broadcast  and calculate duration -->
    <xsl:if test="pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart and pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd">
      <xsl:variable name="start-date">
        <xsl:value-of select="xs:dateTime(pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart)"/>
      </xsl:variable>
      <xsl:variable name="end-date">
        <xsl:value-of select="xs:dateTime(pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd)"/>
      </xsl:variable>
      <f:string key="startDate">
        <xsl:value-of select="$start-date"/>
      </f:string>
      <f:string key="endDate">
        <xsl:value-of select="$end-date"/>
      </f:string>

      <!-- Schema.org refers to the wiki page for ISO8601 and actually wants the duration in the format PT12M50S
           for a duration of 12 minutes and 50 seconds -->
      <f:string key="duration">
        <xsl:value-of select="xs:dateTime($end-date) - xs:dateTime($start-date)"/>
      </f:string>

      <!-- Construct keywords list from all genre fields. Seperates entries by comma and removes last comma.-->
      <xsl:if test="pbcoreGenre">
        <xsl:variable name="keywords">
          <xsl:for-each select="pbcoreGenre">
            <xsl:value-of select="remove(concat(normalize-space(f:substring-after(., ': ')), ', '), 2)"/>
          </xsl:for-each>
        </xsl:variable>
        <!-- Length used to delete last comma from keyword list.-->
        <xsl:variable name="keywords-length" select="string-length($keywords)"/>

        <f:string key="keywords">
          <xsl:value-of select="substring($keywords, 1, $keywords-length - 2)"/>
        </f:string>
      </xsl:if>

      <!-- Construct identifiers for accession_number, ritzau_id and tvmeter_id -->
      <f:array key="identifier">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">Origin</f:string>
          <f:string key="value"><xsl:value-of select="$origin"/></f:string>
        </f:map>
        <!-- TODO: Update template to require parameters containing identifers from the xip level of the metadata -->
        <xsl:if test="pbcoreIdentifier">
          <xsl:for-each select="pbcoreIdentifier">
            <xsl:choose>
              <xsl:when test="identifierSource = ''">
              </xsl:when>
              <xsl:otherwise>
                <f:map>
                  <f:string key="@type">PropertyValue</f:string>
                  <f:string key="PropertyID">
                    <xsl:value-of select="./identifierSource"/>
                  </f:string>
                  <f:string key="value">
                    <xsl:value-of select="./identifier"/>
                  </f:string>
                </f:map>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:if>
      </f:array>
    </xsl:if>

  </xsl:template>

</xsl:transform>