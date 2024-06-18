<?xml version="1.0" encoding="UTF-8" ?>
<!-- When changes are made to this XSLT, please update the general description of the mapping which is available
     in the file: src/main/webapp/mappings_radiotv.html  -->
<xsl:transform xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:xip="http://www.tessella.com/XIP/v4"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:pbc="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
               xmlns:padding="http://kuana.kb.dk/types/padding/0/1/#"
               xmlns:access="http://doms.statsbiblioteket.dk/types/access/0/1/#"
               xmlns:pidhandle="http://kuana.kb.dk/types/pidhandle/0/1/#"
               xmlns:program_structure="http://doms.statsbiblioteket.dk/types/program_structure/0/1/#"
               xmlns:err="http://www.w3.org/2005/xqt-errors"
               version="3.0">
  
  <xsl:output method="text"/>

  <!--INJECTIONS -->
  <!-- Streraming server where preservica manifestations can be streamed from.-->
  <xsl:param name="streamingserver"/>
  <!-- Origin for transformed record.-->
  <xsl:param name="origin"/>
  <!-- ID of the record. -->
  <xsl:param name="recordID"/>
  <!-- XML containing the presentation manifestation for the record in hand-->
  <xsl:param name="manifestation"/>
  <!-- Representation of when the record was last modified in the backing ds-storage. The value is a long representing time
       since epoch with microsecond precision (milliseconds with 3 extra digits). -->
  <xsl:param name="mTime"/>
  <!-- Access condition for DR material. Currently, this param contains a placeholder. -->
  <xsl:param name="conditionsOfAccess"/>
  <xsl:include href="xslt/utils.xsl"/>


  <!-- MAIN TEMPLATE. This template delegates, which fields are to be created for each schema.org object.
       Currently, the template handles transformations from Preservica records to SCHEMA.ORG VideoObjects and AudioObjects. -->
  <xsl:template match="/">

    <xsl:variable name="pbCore">
          <xsl:for-each select="/XIP/Metadata/Content/pbc:PBCoreDescriptionDocument">
            <xsl:apply-templates mode="strip-ns"/>
          </xsl:for-each>
          <xsl:value-of select="."/>
    </xsl:variable>

    <xsl:variable name="pidHandles">
      <xsl:value-of select="distinct-values(//pidhandle:pidhandle/handle)"/>
    </xsl:variable>

    <!-- Determine the type of schema.org object in hand.-->
    <xsl:variable name="type">
      <xsl:choose>
        <!-- XIP/Metadata/Content/pbc:PBCoreDescriptionDocument/pbc:pbcoreInstantiation/pbc:formatMediaType = 'Moving Image'"-->
        <xsl:when test="$pbCore/pbcoreInstantiation/formatMediaType = 'Moving Image'">VideoObject</xsl:when>
        <xsl:when test="$pbCore/pbcoreInstantiation/formatMediaType = 'Sound'">AudioObject</xsl:when>
        <xsl:otherwise>MediaObject</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>


    <!-- Saves all extensions in a variable used to check if one or more conditions are met in any of them.
         This is done to create one nested object in the JSON with values from multiple PBC extensions. -->
    <xsl:variable name="pbcExtensions" select="$pbCore/pbcoreExtension/extension"/>

    <xsl:variable name="json">
      <!-- TODO: Generel todo: Figure how to determine language for the strings "@language" that can be used throughout the schema.-->
      <!-- Choose which type of transformation to do, based on the input data.
           This choose-statement decides, which of the following templates are to be used for the given record. -->
      <xsl:choose>
        <xsl:when test="$type = 'VideoObject'">
          <xsl:call-template name="video-transformation">
            <xsl:with-param name="pbCore" select="$pbCore"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
            <xsl:with-param name="pidHandles" select="$pidHandles"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$type = 'AudioObject'">
          <xsl:call-template name="audio-transformation">
            <xsl:with-param name="pbCore" select="$pbCore"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
            <xsl:with-param name="pidHandles" select="$pidHandles"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="generic-transformation">
            <xsl:with-param name="pbCore" select="$pbCore"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
            <xsl:with-param name="pidHandles" select="$pidHandles"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

  <!-- TEMPLATE FOR TRANSFORMING VIDEOOBJECTS. The template requires the following two parameters:
        type: The type of schema-org object in hand.
        pbcExtensions: A parameter containing all PBCore Extensions for better retrieval of specific extensions during
                       the transformation. -->
  <xsl:template name="video-transformation">
    <xsl:param name="pbCore"/>
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="pidHandles"/>

    <f:map>
      <!-- Creates the first three fields for docs. -->
      <xsl:call-template name="schema-context-and-type">
        <xsl:with-param name="type" select="$type"/>
      </xsl:call-template>

      <xsl:try>
        <!-- Extract PBCore metadata-->
        <xsl:for-each select="$pbCore">
          <xsl:call-template name="pbc-metadata">
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
            <xsl:with-param name="pidHandles" select="$pidHandles"/>
          </xsl:call-template>

          <!-- This is the only field directly present in pbc:PBCoreDescriptionDocument, which is only used for video
               objects. Therefore, it is extracted here. If more fields from this part of the metadata were only
               relevant for video objects, then a new template would be introduced. -->
          <!-- Is the resource hd? or do we know anything about the video quality=? -->
          <xsl:if test="//pbcoreInstantiation/formatStandard != ''">
            <f:string key="videoQuality"><xsl:value-of select="//pbcoreInstantiation/formatStandard"/></f:string>
          </xsl:if>
        </xsl:for-each>

        <!-- Extract manifestation -->
        <xsl:call-template name="extract-manifestation-preservica">
          <xsl:with-param name="pbCore" select="$pbCore"/>
        </xsl:call-template>

        <!-- Create the kb:internal map. This map contains all metadata, that are not represented in schema.org, but were
             available from the preservica records.-->
        <f:map key="kb:internal">
        <!-- Transforms values that does not fit directly into Schema.org into an internal map. -->
        <xsl:call-template name="kb-internal">
          <xsl:with-param name="pbCore" select="$pbCore"/>
          <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
          <xsl:with-param name="type" select="$type"/>
        </xsl:call-template>

        <!-- This template extracts internal fields, that are only relevant for video objects. Therefore, they have been
             removed from the overall kb-internal template called above. The fields are: aspect_ratio and color.-->
        <xsl:call-template name="internal-video-fields">
          <xsl:with-param name="pbCore" select="$pbCore"/>
        </xsl:call-template>

        <!-- This template also extracts internal fields only relevant for video. The rest of the extension extraction
             is handled in the kb-internal template called above. -->
        <xsl:for-each select="$pbCore//pbcoreExtension/extension">
          <xsl:call-template name="video-extension-extractor"/>
        </xsl:for-each>
        </f:map>

        <!-- Catches all errors in sequence constructors (places where data can be used as input). If an error occurs
             the field origin will be created and internal fields about the error will be created as well. -->
        <xsl:catch errors="*">
          <f:array key="identifier">
            <f:map>
              <f:string key="@type">PropertyValue</f:string>
              <f:string key="PropertyID">Origin</f:string>
              <f:string key="value"><xsl:value-of select="$origin"/></f:string>
            </f:map>
          </f:array>
          <f:map key="kb:internal">
            <!-- Internal value for backing ds-storage mTime-->
            <f:string key="kb:storage_mTime">
              <xsl:value-of select="format-number($mTime, '0')"/>
            </f:string>
            <f:string key="kb:transformation_error"><xsl:value-of select="true()"/></f:string>
            <f:string key="kb:transformation_error_description"><xsl:value-of select="concat($err:code, ': ', $err:description)"/></f:string>
          </f:map>
        </xsl:catch>
      </xsl:try>

    </f:map>
  </xsl:template>

  <!-- TEMPLATE FOR TRANSFORMING AUDIOOBJECTS. The template requires the following five parameters:
        type: The type of schema-org object in hand.
        pbcExtensions: A parameter containing all PBCore Extensions for better retrieval of specific extensions during
                       the transformation. -->
  <xsl:template name="audio-transformation">
    <xsl:param name="pbCore"/>
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="pidHandles"/>

    <!-- As the generic template currently is the same as the AudioObject, then this template is called here-->
    <xsl:call-template name="generic-transformation">
      <xsl:with-param name="pbCore" select="$pbCore"/>
      <xsl:with-param name="type" select="$type"/>
      <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
      <xsl:with-param name="pidHandles" select="$pidHandles"/>
    </xsl:call-template>
  </xsl:template>

  <!-- TEMPLATE FOR TRANSFORMING OBJECTS, WHICH ARE WRONGLY DEFINED. The template requires the following five parameters:
        type: The type of schema-org object in hand.
        pbcExtensions: A parameter containing all PBCore Extensions for better retrieval of specific extensions during
                       the transformation.-->
  <xsl:template name="generic-transformation">
    <xsl:param name="pbCore"/>
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="pidHandles"/>

    <f:map>
      <!-- Creates the first three fields for docs. -->
      <xsl:call-template name="schema-context-and-type">
        <xsl:with-param name="type" select="$type"/>
      </xsl:call-template>

      <xsl:try>
        <!-- Extract PBCore metadata -->
        <!-- We cant assume that all records contain PBCore metadata as some OAI harvests might fail and not extract it.
             We do need to set origin anyway, therefore this choose-statement. -->
        <xsl:choose>
          <xsl:when test="$pbCore != ''">
            <xsl:for-each select="$pbCore">
              <xsl:call-template name="pbc-metadata">
                <xsl:with-param name="type" select="$type"/>
                <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
                <xsl:with-param name="pidHandles" select="$pidHandles"/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <f:array key="identifier">
              <f:map>
                <f:string key="@type">PropertyValue</f:string>
                <f:string key="PropertyID">Origin</f:string>
                <f:string key="value"><xsl:value-of select="$origin"/></f:string>
              </f:map>
            </f:array>
          </xsl:otherwise>
        </xsl:choose>

        <!-- Extract manifestation -->
        <xsl:call-template name="extract-manifestation-preservica">
          <xsl:with-param name="pbCore" select="$pbCore"/>
        </xsl:call-template>

        <!-- If type is MediaObject we don't create the internal map. -->
        <xsl:if test="$type != 'MediaObject'">
          <f:map key="kb:internal">
          <!-- Transforms values that does not fit directly into Schema.org into an internal map. -->
            <xsl:call-template name="kb-internal">
              <xsl:with-param name="pbCore" select="$pbCore"/>
              <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
              <xsl:with-param name="type" select="$type"/>
            </xsl:call-template>
          </f:map>
        </xsl:if>
        <!-- Catches all errors in sequence constructors (places where data can be used as input). If an error occurs
           the field origin will be created and internal fields about the error will be created as well. -->
        <xsl:catch errors="*">
          <f:array key="identifier">
            <f:map>
              <f:string key="@type">PropertyValue</f:string>
              <f:string key="PropertyID">Origin</f:string>
              <f:string key="value"><xsl:value-of select="$origin"/></f:string>
            </f:map>
          </f:array>
          <f:map key="kb:internal">
            <!-- Internal value for backing ds-storage mTime-->
            <f:string key="kb:storage_mTime">
              <xsl:value-of select="format-number($mTime, '0')"/>
            </f:string>
            <f:string key="kb:transformation_error"><xsl:value-of select="true()"/></f:string>
            <f:string key="kb:transformation_error_description"><xsl:value-of select="$err:description"/></f:string>
          </f:map>
        </xsl:catch>
      </xsl:try>
    </f:map>
  </xsl:template>

  <!-- CREATE THREE FIRST FIELDS FOR SCHEMAORG JSON: CONTEXT, TYPE AND ID. These fields are present in every document.-->
  <xsl:template name="schema-context-and-type">
    <xsl:param name="type"/>
    <!-- First three fields for schema.org are these no matter which object the transformer transforms to. -->
    <f:string key="@context">http://schema.org/</f:string>
    <f:string key="@type"><xsl:value-of select="$type"/></f:string>
    <f:string key="id">
      <xsl:value-of select="$recordID"/>
    </f:string>
    <f:string key="conditionsOfAccess">
      <xsl:value-of select="$conditionsOfAccess"/>
    </f:string>
  </xsl:template>


  <!-- TEMPLATE FOR ACCESSING PBCORE METADATA. This template transforms all fields, that are relevant for all objects.
       Fields such as 'videoQuality' is not part of the template extraction and are extracted in the video-transformation
       template.-->
  <xsl:template name="pbc-metadata">
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="pidHandles"/>
    <!-- TODO: Investigate relation between titel and originaltitel. Some logic related to metadata delivery type exists. -->
    <!-- Create fields headline and alternativeHeadline if needed.
         Determine if title and original title are alike. Both fields should always be in metadata -->
    <!-- TODO: Do some validation of titles - check with metadata schema when they are set.    -->
    <xsl:variable name="title">
      <xsl:for-each select="./pbcoreTitle">
        <xsl:if test="./titleType = 'titel'">
          <xsl:value-of select="./title"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="original-title">
      <xsl:for-each select="/pbcoreTitle">
        <xsl:if test="./titleType = 'originaltitel'">
          <xsl:value-of select="./title"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$title = $original-title and $title != '' or ($title != '' and $original-title = '')">
        <f:string key="name">
          <xsl:value-of select="$title"/>
        </f:string>
      </xsl:when>
      <xsl:when test="$title = '' and $original-title != ''">
        <f:string key="name">
          <xsl:value-of select="$original-title"/>
        </f:string>
      </xsl:when>
      <xsl:otherwise>
        <f:string key="name">
          <xsl:value-of select="$title"/>
        </f:string>
        <f:string key="alternateName">
          <xsl:value-of select="$original-title"/>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Publisher extraction. Some metadata has two pbcorePublisher/publisher/publisherRole.
         We use the one with the value "kanalnavn" as this should be present in all metadata files.-->
    <xsl:variable name="publisherSpecific">
      <xsl:for-each select="./pbcorePublisher">
        <xsl:if test="./publisherRole ='kanalnavn'">
          <xsl:value-of select="./publisher"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="publisherGeneral">
      <xsl:for-each select="./pbcorePublisher">
        <xsl:choose>
          <xsl:when test="./publisherRole ='channel_name'">
            <xsl:value-of select="./publisher"/>
          </xsl:when>
          <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </xsl:variable>

    <xsl:if test="./pbcorePublisher">
      <f:map key="publication">
        <f:string key="@type">BroadcastEvent</f:string>
        <!-- Define isLiveBroadcast from live extension field.  -->
        <!-- TODO: Figure out what to do when live field isn't present in metadata. -->
        <xsl:for-each select="./pbcoreExtension/extension">
          <xsl:if test="f:contains(., 'live:live') or f:contains(., 'live:ikke live')">
            <f:boolean key="isLiveBroadcast">
              <!-- Chooses between 'live' or 'ikke live' as these are boolean values.-->
              <xsl:choose>
                <xsl:when test="contains(., 'live:live')"><xsl:value-of select="f:true()"/></xsl:when>
                <xsl:when test="contains(., 'live:ikke live')"><xsl:value-of select="false()"/></xsl:when>
              </xsl:choose>
            </f:boolean>
          </xsl:if>
        </xsl:for-each>
        <!-- Preservica contains two different fields for broadcaster-->
        <xsl:if test="$publisherSpecific != ''">
          <f:map key="publishedOn">
            <f:string key="@type">BroadcastService</f:string>
            <f:string key="broadcastDisplayName">
              <xsl:value-of select="$publisherSpecific"/>
            </f:string>
            <xsl:if test="f:exists($publisherGeneral) and not(f:empty($publisherGeneral)) and $publisherGeneral != ''">
            <f:string key="alternateName">
              <xsl:value-of select="$publisherGeneral"/>
            </f:string>
            <xsl:choose>
              <xsl:when test="f:starts-with($publisherGeneral, 'dr')">
                <f:map key="broadcaster">
                  <f:string key="@type">Organization</f:string>
                  <f:string key="legalName">DR</f:string>
                </f:map>
              </xsl:when>
              <xsl:when test="f:starts-with($publisherGeneral, 'tv2')">
                <f:map key="broadcaster">
                  <f:string key="@type">Organization</f:string>
                  <f:string key="legalName">TV 2 Danmark</f:string>
                </f:map>
              </xsl:when>
              <xsl:when test="f:starts-with($publisherGeneral, 'tv3')">
                <f:map key="broadcaster">
                  <f:string key="@type">Organization</f:string>
                  <f:string key="legalName">TV3</f:string>
                </f:map>
              </xsl:when>
              <xsl:otherwise></xsl:otherwise>
            </xsl:choose>
            </xsl:if>
          </f:map>
        </xsl:if>
        <!-- TODO: Figure if it is possible to extract broadcaster in any meaningful way for the field 'broadcaster',
              maybe from hovedgenre_id or kanalid. Otherwise it could be defined as 'Danmarks Radio'
              for the first part of the project. -->
      </f:map>
    </xsl:if>

    
    <!-- Creates datePublished, when pbcore extension tells that the program is a premiere.  -->
    <xsl:if test="$pbcExtensions[f:contains(., 'premiere:premiere')] and ./pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart">
      <f:string key="datePublished">
        <xsl:value-of select="f:substring-before(pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart, 'T')"/>
      </f:string>
    </xsl:if>

    <!-- Checks if PBC extensions contain metadata about episodes and season lengths
         and creates the field encodesCreativeWork if true.
         This if-statements checks that the PBC extensions 'episodenr' and 'antalepisoder' have actual values.-->
    <xsl:if test="$pbcExtensions[f:contains(.,'episodenr:') and
                  f:string-length(substring-after(., 'episodenr:')) > 0 or
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
        <xsl:for-each select="./pbcoreTitle">
          <xsl:if test="titleType = 'episodetitel' and title != ''">
            <f:string key="name"><xsl:value-of select="title"/></f:string>
          </xsl:if>
        </xsl:for-each>


        <xsl:for-each select="./pbcoreExtension/extension">
          <xsl:choose>
            <xsl:when test="f:contains(substring-after(., 'episodenr:'), ':')">
                <xsl:variable name="episodeInfo">
                  <xsl:value-of select="substring-after(., 'episodenr:')"/>
                </xsl:variable>

                <f:number key="episodeNumber">
                  <xsl:value-of select="substring-before($episodeInfo, ':')"/>
                </f:number>
                <f:map key="partOfSeason">
                  <f:string key="@type">
                    <xsl:choose>
                      <xsl:when test="$type = 'VideoObject'">TVSeason</xsl:when>
                      <xsl:when test="$type = 'AudioObject'">RadioSeason</xsl:when>
                      <xsl:otherwise>CreativeWorkSeason</xsl:otherwise>
                    </xsl:choose>
                  </f:string>
                  <f:number key="numberOfEpisodes">
                    <xsl:value-of select="substring-after($episodeInfo, ':')"/>
                  </f:number>
                </f:map>
            </xsl:when>
            <xsl:otherwise>
              <!-- Extract metadata from PBC extensions related to episodes -->
              <xsl:for-each select=".">
                <!-- Extract episode number if present.
                     Checks for 'episodenr' in PBC extension and checks that there is a substring after the key.-->
                <xsl:if test="f:contains(., 'episodenr:') and f:string-length(substring-after(., 'episodenr:')) > 0">
                  <f:number key="episodeNumber">
                    <xsl:value-of select="substring-after(., 'episodenr:')"/>
                  </f:number>
                </xsl:if>
              </xsl:for-each>

              <!-- Extract metadata from PBC extensions related to season length. -->
              <xsl:for-each select=".">
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
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </f:map>
    </xsl:if>

    <!-- Create description field from 'langomtale1' and abstract field from 'kortomtale' -->
    <!-- From the metadata it is clear, that 'kortomtale' and 'langomtale' can contain completely different values.
         'kortomtale' is therefore not just a shorter form of 'langomtale'.
         'kortomtale' maps to the schema.org value abstract, while 'langomtale' maps to description-->
    <xsl:for-each select="./pbcoreDescription">
      <xsl:choose>
        <!-- Extract 'kortomtale' as abstract. -->
        <xsl:when test="./descriptionType = 'kortomtale' and description != ''">
          <f:string key="abstract">
            <xsl:value-of select="normalize-space(./description)"/>
          </f:string>
        </xsl:when>
        <xsl:when test="./descriptionType = 'langomtale1' and description != ''">
          <f:string key="description">
            <xsl:value-of select="normalize-space(./description)"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>

    <!-- Extract start and end times for broadcast  and calculate duration -->
    <xsl:if test="./pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart and
                  ./pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd">
      <xsl:variable name="start-time">
        <xsl:variable name="startTimeString">
          <xsl:value-of select="./pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart"/>
        </xsl:variable>
        <xsl:value-of select="my:convertDatetimeToZulu($startTimeString)"/>
      </xsl:variable>
      <xsl:variable name="end-time">
        <xsl:variable name="endTimeString">
          <xsl:value-of select="./pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd"/>
        </xsl:variable>
        <xsl:value-of select="my:convertDatetimeToZulu($endTimeString)"/>
      </xsl:variable>
      <f:string key="startTime">
        <xsl:value-of select="$start-time"/>
      </f:string>
      <f:string key="endTime">
        <xsl:value-of select="$end-time"/>
      </f:string>

      <!-- Schema.org refers to the wiki page for ISO8601 and actually wants the duration in the format PT12M50S
           for a duration of 12 minutes and 50 seconds -->
      <f:string key="duration">
        <xsl:value-of select="xs:dateTime($end-time) - xs:dateTime($start-time)"/>
      </f:string>
    </xsl:if>

    <!-- Construct keywords list from all genre fields. Seperates entries by comma and removes last comma.
         Also extracts maingenre to the schema.org field 'genre'-->
    <xsl:if test="//pbcoreGenre">
      <xsl:variable name="keywords">
        <xsl:for-each select="./pbcoreGenre/genre">
          <xsl:if test="substring-after(., ':') != '' and not(f:contains(., 'null'))">
            <xsl:value-of select="concat(normalize-space(f:substring-after(., ':')), ', ')"/>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <!-- Length used to delete last comma from keyword list.-->
      <xsl:variable name="keywords-length" select="string-length($keywords)"/>

      <xsl:if test="$keywords != ''">
        <f:string key="keywords">
          <xsl:value-of select="substring($keywords, 1, $keywords-length - 2)"/>
        </f:string>
      </xsl:if>

      <xsl:for-each select="./pbcoreGenre/genre">
        <xsl:if test="f:contains(., 'hovedgenre:') and substring-after(., 'hovedgenre:') != ''">
          <f:string key="genre">
            <xsl:value-of select="normalize-space(substring-after(., 'hovedgenre:'))"/>
          </f:string>
        </xsl:if>
      </xsl:for-each>
    </xsl:if>

    <!-- Construct identifiers for accession_number, ritzau_id and tvmeter_id -->
    <f:array key="identifier">
      <f:map>
        <f:string key="@type">PropertyValue</f:string>
        <f:string key="PropertyID">Origin</f:string>
        <f:string key="value"><xsl:value-of select="$origin"/></f:string>
      </f:map>
      <!-- TODO: Update template to require parameters containing identifiers from the xip level of the metadata -->
      <f:map>
        <f:string key="@type">PropertyValue</f:string>
        <f:string key="PropertyID">RecordID</f:string>
        <f:string key="value">
          <xsl:value-of select="$recordID"/>
        </f:string>
      </f:map>
      <xsl:if test="//pbcoreIdentifier">
        <xsl:for-each select="./pbcoreIdentifier">
          <xsl:choose>
            <!-- Do nothing when identifierSource or identifier is empty. -->
            <xsl:when test="identifierSource = ''">
            </xsl:when>
            <xsl:when test="identifier = ''">
            </xsl:when>
            <xsl:when test="identifierSource = 'Det Kongelige Bibliotek; Radio/TV-samlingen; De hvide programmer'">
              <f:map>
                <f:string key="@type">PropertyValue</f:string>
                <f:string key="PropertyID">WhiteProgramID</f:string>
                <f:string key="value">
                  <xsl:value-of select="normalize-space(substring-after(identifier, 'ID:'))"/>
                </f:string>
              </f:map>
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
      <!-- Extracts PID as identifier if present.-->
      <xsl:if test="$pidHandles != ''">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">PID</f:string>
          <f:string key="value">
            <xsl:value-of select="normalize-space(substring-after($pidHandles, 'hdl:'))"/>
          </f:string>
        </f:map>
      </xsl:if>
      <!-- Extract accession ref as schema.org identifier --> <!-- TODO: This could properly be done with loads of the identifiers in the kb:internal map.-->
      <xsl:if test="/XIP/Metadata/Content/LegacyXIP/AccessionRef">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">InternalAccessionRef</f:string>
          <f:string key="value"><xsl:value-of select="/XIP/Metadata/Content/LegacyXIP/AccessionRef"/></f:string>
        </f:map>
      </xsl:if>
    </f:array>

    <!-- Extracts collection -->
    <xsl:if test="./pbcoreInstantiation/formatLocation != ''">
      <f:array key="isPartOf">
        <f:map>
          <f:string key="@type">Collection</f:string>
          <f:string key="name"><xsl:value-of select="//pbcoreInstantiation/formatLocation"/></f:string>
        </f:map>
      </f:array>
    </xsl:if>
  </xsl:template>

  <!-- EXTRACT MANIFESTATION FROM METADATA.-->
  <xsl:template name="extract-manifestation-preservica">
    <xsl:param name="pbCore"/>
    <xsl:if test="$manifestation != ''">
      <xsl:variable name="manifestationRef">
        <xsl:value-of select="$manifestation"/>
      </xsl:variable>
      <xsl:variable name="urlPrefix">
        <xsl:choose>
          <xsl:when test="$pbCore/pbcoreInstantiation/formatMediaType = 'Moving Image'">mp4:bart-access-copies-tv/</xsl:when>
          <xsl:when test="$pbCore/pbcoreInstantiation/formatMediaType = 'Sound'">mp3:bart-access-copies-radio/</xsl:when>
          <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="path">
        <xsl:variable name="path_level1">
          <xsl:value-of select="concat(substring($manifestationRef,1,2), '/')"/>
        </xsl:variable>
        <xsl:variable name="path_level2">
          <xsl:value-of select="concat(substring($manifestationRef,3,2), '/')"/>
        </xsl:variable>
        <xsl:variable name="path_level3">
          <xsl:value-of select="concat(substring($manifestationRef,5,2), '/')"/>
        </xsl:variable>
        <xsl:value-of select="concat($path_level1, $path_level2, $path_level3)"/>
      </xsl:variable>
      <xsl:variable name="streamingUrl">
        <xsl:value-of select="concat($streamingserver, $urlPrefix,
                                         $path, $manifestationRef,
                                         '/playlist.m3u8')"/>
      </xsl:variable>
      <f:string key="contentUrl">
        <!-- TODO: Add full url to content when possible-->
        <xsl:value-of select="$streamingUrl"/>
      </f:string>
    </xsl:if>
  </xsl:template>


  <!-- TEMPLATE FOR EXTRACTING INTERNAL VALUES WHICH DON'T HAVE A SCHEMA.ORG DATA REPRESENTATION.
       These values can be almost anything ranging from identifiers to acces conditions.
       This kb:internal map was how we've handled internal values in the past, see line 109 in this file:
       https://github.com/kb-dk/ds-present/blob/spolorm-now-works/src/main/resources/xslt/mods2schemaorg.xsl -->
  <xsl:template name="kb-internal">
    <xsl:param name="pbCore"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="type"/>

    <!-- Internal value for backing ds-storage mTime-->
    <f:string key="kb:storage_mTime">
      <xsl:value-of select="format-number($mTime, '0')"/>
    </f:string>

    <xsl:if test="$manifestation != ''">
      <f:string key="kb:file_id">
        <xsl:value-of select="$manifestation"/>
      </f:string>
    </xsl:if>

    <!-- Extract subgenre if present -->
    <xsl:for-each select="$pbCore/pbcoreGenre/genre">
      <xsl:if test="contains(., 'undergenre:') and substring-after(., 'undergenre:') != ''">
        <f:string key="kb:genre_sub">
          <xsl:value-of select="normalize-space(substring-after(., 'undergenre:'))"/>
        </f:string>
      </xsl:if>
    </xsl:for-each>
    <!-- Create boolean for surround-->
    <xsl:choose>
      <xsl:when test="$pbCore/pbcoreInstantiation/formatChannelConfiguration = 'surround'">
        <f:boolean key="kb:surround_sound"><xsl:value-of select="true()"/></f:boolean>
      </xsl:when>
      <xsl:when test="$pbCore/pbcoreInstantiation/formatChannelConfiguration = 'ikke surround'">
        <f:boolean key="kb:surround_sound"><xsl:value-of select="false()"/></f:boolean>
      </xsl:when>
    </xsl:choose>
    <!-- Create boolean for premiere-->
    <xsl:choose>
      <xsl:when test="$pbcExtensions[f:contains(., 'premiere:ikke premiere')]">
        <f:boolean key="kb:premiere">
          <xsl:value-of select="false()"/>
        </f:boolean>
      </xsl:when>
      <xsl:when test="$pbcExtensions[f:contains(., 'premiere:premiere')]">
        <f:boolean key="kb:premiere">
          <xsl:value-of select="true()"/>
        </f:boolean>
      </xsl:when>
    </xsl:choose>
    <!-- Extract format identifiers -->
    <xsl:for-each select="$pbCore/pbcoreInstantiation/pbcoreFormatID">
      <xsl:choose>
        <xsl:when test="formatIdentifierSource = 'ritzau'">
          <f:string key="kb:format_identifier_ritzau">
            <xsl:value-of select="formatIdentifier"/>
          </f:string>
        </xsl:when>
        <xsl:when test="formatIdentifierSource = 'nielsen'">
          <f:string key="kb:format_identifier_nielsen">
            <xsl:value-of select="formatIdentifier"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
    <!--TODO: Figure if retransmission can fit into real schema.org -->
    <!-- Create boolean for retransmission-->
    <xsl:choose>
      <xsl:when test="$pbcExtensions[f:contains(., 'genudsendelse:ikke genudsendelse')]">
        <f:boolean key="kb:retransmission">
          <xsl:value-of select="false()"/>
        </f:boolean>
      </xsl:when>
      <xsl:when test="$pbcExtensions[f:contains(., 'genudsendelse:genudsendelse')]">
        <f:boolean key="kb:retransmission">
          <xsl:value-of select="true()"/>
        </f:boolean>
      </xsl:when>
    </xsl:choose>
    <!-- Extracts multiple extensions to the internal KB map. These extensions can contain many different values.
         Some have external value, while others primarily are for internal usage.-->
    <xsl:for-each select="$pbCore/pbcoreExtension/extension">
      <xsl:call-template name="extension-extractor">
      <xsl:with-param name="type" select="$type"/>
      </xsl:call-template>
    </xsl:for-each>

    <!-- Extracts information on video padding. -->
    <xsl:if test="/XIP/Metadata/Content/padding:padding/paddingSeconds">
      <f:number key="kb:padding_seconds">
        <xsl:value-of select="/XIP/Metadata/Content/padding:padding/paddingSeconds"/>
      </f:number>
    </xsl:if>

    <!-- Extracts access metadata to the internal kb map -->
    <xsl:for-each select="/XIP/Metadata/Content/access:access">
      <xsl:call-template name="access-template"/>
    </xsl:for-each>

    <!-- Extracts information on the structure of the video component. -->
    <xsl:for-each select="/XIP/Metadata/Content/program_structure:program_structure">
      <xsl:call-template name="program-structure"/>
    </xsl:for-each>
  </xsl:template>

  <!-- Transforms internal fields, that are only present for tv/video metadata. These fields are:
       aspect_ratio and color.-->
  <xsl:template name="internal-video-fields">
    <xsl:param name="pbCore"/>
    <!-- Extract aspect ratio-->
    <xsl:if test="$pbCore/pbcoreInstantiation/formatAspectRatio">
      <f:string key="kb:aspect_ratio">
        <xsl:value-of select="$pbCore/pbcoreInstantiation/formatAspectRatio"/>
      </f:string>
    </xsl:if>

    <!-- Create boolean for color for tv resources-->
    <xsl:choose>
      <xsl:when test="$pbCore/pbcoreInstantiation/formatColors = 'farve'">
        <f:boolean key="kb:color"><xsl:value-of select="true()"/></f:boolean>
      </xsl:when>
      <xsl:otherwise>
        <f:boolean key="kb:color"><xsl:value-of select="false()"/></f:boolean>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- EXTRACT VALUES FROM PBCORE EXTENSIONS TO KB:INTERNAL MAP. These extensions can contain many different values.
       Some might be relevant in relation to schema.org and can be elevated to the correct structure.-->
  <xsl:template name="extension-extractor">
    <xsl:param name="type"/>
    <!-- Extracts multiple internal ids. -->
    <xsl:choose>
      <xsl:when test="f:starts-with(. , 'hovedgenre_id:')">
        <f:string key="kb:maingenre_id">
          <xsl:value-of select="substring-after(. , 'hovedgenre_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'kanalid:')">
        <f:number key="kb:channel_id">
          <xsl:value-of select="substring-after(. , 'kanalid:')"/>
        </f:number>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'produktionsland_id:')">
        <f:string key="kb:country_of_origin_id">
          <xsl:value-of select="f:substring-after(. , 'produktionsland_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'program_id:')">
        <f:string key="kb:ritzau_program_id">
          <xsl:value-of select="f:substring-after(. , 'program_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'undergenre_id:')">
        <f:string key="kb:subgenre_id">
          <xsl:value-of select="f:substring-after(. , 'undergenre_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'afsnit_id:')">
        <f:string key="kb:episode_id">
          <xsl:value-of select="f:substring-after(. , 'afsnit_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'saeson_id:')">
        <f:string key="kb:season_id">
          <xsl:value-of select="f:substring-after(. , 'saeson_id:')"/>
        </f:string>
      </xsl:when>
      <xsl:when test="f:starts-with(. , 'serie_id:')">
        <f:string key="kb:series_id">
          <xsl:value-of select="f:substring-after(. , 'serie_id:')"/>
        </f:string>
      </xsl:when>
      <!-- Check if there has been a stop in the transmission-->
      <xsl:when test="f:starts-with(. , 'program_ophold:')">
        <!-- inner XSLT Choose which determines if program_ophold is false or true -->
        <xsl:choose>
          <xsl:when test=". = 'program_ophold:ikke program ophold'">
            <f:boolean key="kb:program_ophold">
              <xsl:value-of select="false()"/>
            </f:boolean>
          </xsl:when>
          <xsl:when test=". = 'program_ophold:program ophold'">
            <f:boolean key="kb:program_ophold">
              <xsl:value-of select="true()"/>
            </f:boolean>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Extracts extensions that are only applicable for video objects. -->
  <xsl:template name="video-extension-extractor">
    <xsl:choose>
      <!--Extract internal showviewcode -->
      <xsl:when test="f:starts-with(. , 'showviewcode:')">
        <f:string key="kb:showviewcode">
          <xsl:value-of select="f:substring-after(. , 'showviewcode:')"/>
        </f:string>
      </xsl:when>
      <!-- TODO: Check if has_subtitles fits in schema.org-->
      <!-- Boolean for if the program contains subtitles.-->
      <xsl:when test="f:starts-with(. , 'tekstet:')">
        <!-- Inner XSLT  choose to determine value of boolean -->
        <xsl:choose>
          <xsl:when test=". = 'tekstet:ikke tekstet'">
            <f:boolean key="kb:has_subtitles">
              <xsl:value-of select="false()"/>
            </f:boolean>
          </xsl:when>
          <xsl:when test=". = 'tekstet:tekstet'">
            <f:boolean key="kb:has_subtitles">
              <xsl:value-of select="true()"/>
            </f:boolean>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <!-- Is the resource teletext?-->
      <xsl:when test="f:starts-with(. , 'ttv:')">
        <!-- Inner XSLT  choose to determine value of boolean -->
        <xsl:choose>
          <xsl:when test=". = 'ttv:ikke tekst-tv'">
            <f:boolean key="kb:is_teletext">
              <xsl:value-of select="false()"/>
            </f:boolean>
          </xsl:when>
          <xsl:when test=". = 'ttv:tekst-tv'">
            <f:boolean key="kb:is_teletext">
              <xsl:value-of select="true()"/>
            </f:boolean>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <!--TODO: Check if subtitles for hearing impaired can be described in schema.org'-->
      <!-- Boolean value - does the resource contain subtitles for hearing impaired?-->
      <xsl:when test="f:starts-with(. , 'th:')">
        <!-- Inner XSLT  choose to determine value of boolean -->
        <xsl:choose>
          <xsl:when test=". = 'th:ikke tekstet for hørehæmmede'">
            <f:boolean key="kb:has_subtitles_for_hearing_impaired">
              <xsl:value-of select="false()"/>
            </f:boolean>
          </xsl:when>
          <xsl:when test=". = 'th:tekstet for hørehæmmede'">
            <f:boolean key="kb:has_subtitles_for_hearing_impaired">
              <xsl:value-of select="true()"/>
            </f:boolean>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- TEMPLATE FOR ACCESSING ACCESS METADATA.-->
  <xsl:template name="access-template">
    <xsl:if test="individuelt_forbud">
      <f:string key="kb:access_individual_prohibition">
        <xsl:value-of select="individuelt_forbud"/>
      </f:string>
    </xsl:if>
    <xsl:if test="klausuleret">
      <f:string key="kb:access_claused">
        <xsl:value-of select="klausuleret"/>
      </f:string>
    </xsl:if>
    <xsl:if test="defekt">
      <f:string key="kb:access_malfunction">
        <xsl:value-of select="defekt"/>
      </f:string>
    </xsl:if>
    <xsl:if test="kommentarer and kommentarer != ''">
      <f:string key="kb:access_comments">
        <xsl:value-of select="kommentarer"/>
      </f:string>
    </xsl:if>
  </xsl:template>

  <!--TEMPLATE ON PROGRAM STRUCTURE. This template extracts metadata on the structure of the program. e.g.
      Is anything missing, if yes, how manyb seconds are missing in the beginning or the end of the resource etc.-->
  <xsl:template name="program-structure">
    <xsl:if test="missingStart/missingSeconds != ''">
      <f:string key="kb:program_structure_missing_seconds_start">
        <xsl:value-of select="missingStart/missingSeconds"/>
      </f:string>
    </xsl:if>
    <xsl:if test="missingEnd/missingSeconds != ''">
      <f:string key="kb:program_structure_missing_seconds_end">
        <xsl:value-of select="missingEnd/missingSeconds"/>
      </f:string>
    </xsl:if>
    <xsl:if test="holes != ''">
      <f:string key="kb:program_structure_holes">
        <xsl:value-of select="holes"/>
      </f:string>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="overlaps != ''">
        <f:string key="kb:program_structure_overlaps">
          <xsl:value-of select="f:true()"/>
        </f:string>
        <f:array key="kb:program_structure_overlap">
          <xsl:for-each select="overlaps/overlap">
            <f:map>
              <f:string key="file1UUID">
                <xsl:value-of select="file1UUID"/>
              </f:string>
              <f:string key="file2UUID">
                <xsl:value-of select="file2UUID"/>
              </f:string>
              <f:number key="overlap_length">
                <xsl:value-of select="overlapLength"/>
              </f:number>
              <f:string key="overlap_type">
                <xsl:value-of select="overlapType"/>
              </f:string>
            </f:map>
          </xsl:for-each>
        </f:array>
      </xsl:when>
      <xsl:otherwise>
        <f:string key="kb:program_structure_overlaps">
          <xsl:value-of select="f:false()"/>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

        <!-- Template to strip namespace from elements -->
  <xsl:template match="*" mode="strip-ns">
  <xsl:element name="{local-name()}">
    <xsl:apply-templates select="@*|node()" mode="strip-ns"/>
  </xsl:element>
  </xsl:template>

          <!-- Template to strip namespace from attributes -->
  <xsl:template match="@*" mode="strip-ns">
  <xsl:attribute name="{local-name()}">
    <xsl:value-of select="."/>
  </xsl:attribute>
  </xsl:template>

</xsl:transform>