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
  <xsl:param name="childID"/>

  <xsl:template match="/">
    <xsl:variable name="json">
      <!-- TODO: Generel todo: Figure how to determine language for the strings "@language" that can be used throughout the schema.-->

      <xsl:variable name="type">
        <xsl:choose>
        <xsl:when test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatMediaType = 'Moving Image'">VideoObject</xsl:when>
        <xsl:when test="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreInstantiation/formatMediaType = 'Sound'">AudioObject</xsl:when>
        <xsl:otherwise>MediaObject</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <f:map>
        <f:string key="@type"><xsl:value-of select="$type"/></f:string>
        <f:string key="id">
            <xsl:value-of select="$recordID"/>
        </f:string>

        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument">
          <xsl:call-template name="pbc-metadata">
            <xsl:with-param name="type" select="$type"/>
          </xsl:call-template>
        </xsl:for-each>

        <!-- Manifestations are extracted here. I would like to create a template for this.
            However, this is quite tricky when using the document() function -->
        <xsl:if test="$childID != '' and doc-available($childID)">
          <f:string key="contentUrl">
            <!-- TODO: Add full url to content when possible-->
            <xsl:value-of select="f:concat($streamingserver,document($childID)/xip:Manifestation/ComponentManifestation/FileRef)"/>
          </f:string>
        </xsl:if>

      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

  <!-- TEMPLATE FOR ACCESSING PBC METADATA.-->
  <xsl:template name="pbc-metadata">
    <xsl:param name="type"/>
    <!-- TODO: Investigate relation between titel and originaltitel. Some logic related to metadata delivery type exists. -->
    <!-- Create fields headline and alternativeHeadline if needed.
         Determine if title and original title are alike. Both fields should always be in metadata -->
    <!-- TODO: Do some validation of titles - check with metadata schema when they are set.    -->
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
          </f:map>
        </f:array>
      </xsl:when>
      <xsl:otherwise>
        <f:array key="name">
          <f:map>
            <f:string key="value">
              <xsl:value-of select="$title"/>
            </f:string>
          </f:map>
        </f:array>
        <f:array key="alternateName">
          <f:map>
            <f:string key="value">
              <xsl:value-of select="$original-title"/>
            </f:string>
          </f:map>
        </f:array>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Publisher extraction. Some metadata has two pbcorePublisher/publisher/publisherRole.
         We use the one with the value "kanalnavn" as this should be present in all metadata files.-->
    <xsl:for-each select="pbcorePublisher">
      <xsl:if test="./publisherRole = 'kanalnavn'">
        <f:map key="publication">
          <f:string key="@type">BroadcastEvent</f:string>
          <!-- Define isLiveBroadcast from live extension field.  -->
          <!-- TODO: Figure out what to do when live field isn't present in metadata. -->
          <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument/pbcoreExtension/extension">
            <xsl:if test="f:contains(., 'live:live') or f:contains(., 'live:ikke live')">
              <f:string key="isLiveBroadcast">
                <!-- Chooses between 'live' or 'ikke live' as these are boolean values.-->
                <xsl:choose>
                  <xsl:when test="contains(., 'live:live')"><xsl:value-of select="f:true()"/></xsl:when>
                  <xsl:when test="contains(., 'live:ikke live')"><xsl:value-of select="false()"/></xsl:when>
                </xsl:choose>
              </f:string>
            </xsl:if>
          </xsl:for-each>
          <f:map key="publishedOn">
            <f:string key="@type">BroadcastService</f:string>
            <f:string key="broadcastDisplayName">
              <xsl:value-of select="./publisher"/>
            </f:string>
          </f:map>
          <!-- TODO: Figure if it is possible to extract broadcaster in any meaningful way for the field 'broadcaster',
                maybe from hovedgenre_id or kanalid. Otherwise it could be defined as 'Danmarks Radio'
                for the first part of the project. -->
        </f:map>
      </xsl:if>
    </xsl:for-each>

    <!-- Saves all extensions in a variable used to check if one or more conditions are met in any of them.
         This is done to create one nested object in the JSON with values from multiple PBC extensions. -->
    <xsl:variable name="pbcExtensions" select="./pbcoreExtension/extension"/>

    <!-- Checks if PBC extensions contain metadata about episodes and season lengths
         and creates the field encodesCreativeWork if true.
         This if-statements checks that the PBC extensions 'episodenr' and 'antalepisoder' have actual values.-->
    <xsl:if test="$pbcExtensions[f:contains(.,'episodenr:') and
                  f:string-length(substring-after(., 'episodenr:')) or
                  (f:contains(., 'antalepisoder:') and
                  not(f:contains(., 'antalepisoder:0')) and
                  f:string-length(substring-after(., 'antalepisoder:')) > 0)]">
      <f:map key="encodesCreativeWork">
        <!-- Determine the type of episode based on the general type of the metadata record.-->
        <f:string key="@type">
          <xsl:choose>
            <xsl:when test="$type = 'VideoObject'">TVEpisode</xsl:when>
            <xsl:when test="$type = 'AudioObject'">RadioEpisode</xsl:when>
            <xsl:otherwise>Episode</xsl:otherwise>
          </xsl:choose>
        </f:string>

        <!-- If episode titel is defined it is extracted here. -->
        <xsl:for-each select="pbcoreTitle">
          <xsl:if test="titleType = 'episodetitel'">
            <f:string key="name"><xsl:value-of select="title"/></f:string>
          </xsl:if>
        </xsl:for-each>

        <!-- Extract metadata from PBC extensions related to episodes -->
        <xsl:for-each select="./pbcoreExtension/extension">
          <!-- Extract episode number if present.
               Checks for 'episodenr' in PBC extension and checks that there is a substring after the key.-->
          <xsl:if test="f:contains(., 'episodenr:') and f:string-length(substring-after(., 'episodenr:')) > 0">
            <f:number key="episodeNumber">
              <xsl:value-of select="substring-after(., 'episodenr:')"/>
            </f:number>
          </xsl:if>
        </xsl:for-each>

        <!-- Extract metadata from PBC extensions related to season length. -->
        <xsl:for-each select="./pbcoreExtension/extension">
          <!-- Extract number of episodes in a season, if present.
               Checks for 'antalepisoder' in PBC extension and checks that the value is not an empty string or 0.
               Create partOfSeason field, if any metadata is present. -->
          <xsl:if test="f:contains(., 'antalepisoder:') and
                        not(f:contains(., 'antalepisoder:0')) and
                        f:string-length(substring-after(., 'antalepisoder:')) > 0">
            <!-- TODO: Figure if  there is a difference between no value and 0.
                 Could one mean that a series is related but no data on it and
                 the other means individual program with no series? -->
            <f:map key="partOfSeason">
              <f:string key="@type">
                <xsl:choose>
                  <xsl:when test="$type = 'VideoObject'">TVSeason</xsl:when>
                  <xsl:when test="$type = 'AudioObject'">RadioSeason</xsl:when>
                  <xsl:otherwise>CreativeWorkSeason</xsl:otherwise>
                </xsl:choose>
              </f:string>
                <f:number key="numberOfEpisodes">
                  <xsl:value-of select="substring-after(., 'antalepisoder:')"/>
                </f:number>
            </f:map>
          </xsl:if>
        </xsl:for-each>
      </f:map>
    </xsl:if>

    <!-- Create description field from 'langomtale1' and abstract field from 'kortomtale' -->
    <!-- From the metadata it is clear, that 'kortomtale' and 'langomtale' can contain completely different values.
         'kortomtale' is therefore not just a shorter form of 'langomtale'.
         'kortomtale' maps to the schema.org value abstract, while 'langomtale' maps to description-->
    <xsl:for-each select="pbcoreDescription">
      <xsl:choose>
        <!-- Extract 'kortomtale' as abstract. -->
        <xsl:when test="./descriptionType = 'kortomtale'">
          <f:string key="abstract">
            <xsl:value-of select="normalize-space(./description)"/>
          </f:string>
        </xsl:when>
        <xsl:when test="./descriptionType = 'langomtale1'">
          <f:string key="description">
            <xsl:value-of select="normalize-space(./description)"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
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
    </xsl:if>

    <!-- Construct keywords list from all genre fields. Seperates entries by comma and removes last comma.-->
    <xsl:if test="pbcoreGenre">
      <xsl:variable name="keywords">
        <xsl:for-each select="pbcoreGenre/genre">
          <xsl:value-of select="concat(normalize-space(f:substring-after(., ': ')), ', ')"/>
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
      <f:map>
        <f:string key="@type">PropertyValue</f:string>
        <f:string key="PropertyID">RecordID</f:string>
        <f:string key="value"><xsl:value-of select="$recordID"/></f:string>
      </f:map>
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
      <f:map>
        <f:string key="@type">PropertyValue</f:string>
        <f:string key="PropertyID">InternalAccessionRef</f:string>
        <f:string key="value"><xsl:value-of select="/xip:DeliverableUnit/AccessionRef"/></f:string>
      </f:map>
    </f:array>


  </xsl:template>

</xsl:transform>