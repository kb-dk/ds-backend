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
               xmlns:access="http://id.kb.dk/schemas/radiotv_access/access"
               xmlns:pidhandle="http://kuana.kb.dk/types/pidhandle/0/1/#"
               xmlns:program_structure="http://doms.statsbiblioteket.dk/types/program_structure/0/1/#"
               xmlns:err="http://www.w3.org/2005/xqt-errors"
               xmlns:transcoding="http://id.kb.dk/schemas/radiotv_access/transcoding_status"
               version="3.0">

  <xsl:output method="text"/>

  <!--INJECTIONS -->
  <!-- Origin for transformed record.-->
  <xsl:param name="origin"/>
  <!-- ID of the record. -->
  <xsl:param name="recordID"/>
  <!-- Start and enddate from this record. Correctly parsed and in UTC time.-->
  <xsl:param name="startTime"/>
  <xsl:param name="endTime"/>
  <xsl:param name="referenceId"/>
  <!-- ID created by kaltura. This ID is the ID of the stream containing the newest presentation copy for this resource. Used for video and audio objects.-->
  <xsl:param name="kalturaID"/>
  <!-- Representation of when the record was last modified in the backing ds-storage. The value is a long representing time
       since epoch with microsecond precision (milliseconds with 3 extra digits). -->
  <xsl:param name="mTime"/>
  <!-- Holdback and Own Production params are values needed for DR material. They are used in the transformations to determine if users are allowed to access the material. -->
  <xsl:param name="holdbackDate"/>
  <xsl:param name="holdbackPurposeName"/>
  <xsl:param name="holdbackFormValue"/>
  <xsl:param name="holdbackContentValue"/>
  <xsl:param name="productionCodeAllowed"/>
  <xsl:param name="productionCodeValue"/>
  <!-- ProductionId has been extracted from either tvmeter or nielsen metadata, and is then injected as a single value. -->
  <xsl:param name="productionId"/>
  <xsl:param name="productionIdRestrictedDr"/>
  <xsl:param name="dsIdRestricted"/>
  <xsl:param name="titleRestricted"/>
  <xsl:param name="platform"/>
  <xsl:param name="transcription"/>
  <xsl:param name="has_transcription"/>
  <xsl:include href="xslt/utils.xsl"/>

  <xsl:variable name="InternalAccessionRef">
    <xsl:value-of select="/XIP/Metadata/Content/LegacyXIP/AccessionRef"/>
  </xsl:variable>

  <xsl:variable name="pidHandles">
    <xsl:value-of select="distinct-values(//pidhandle:pidhandle/handle)"/>
  </xsl:variable>

  <!-- MAIN TEMPLATE. This template delegates, which fields are to be created for each schema.org object.
       Currently, the template handles transformations from Preservica records to SCHEMA.ORG VideoObjects and AudioObjects. -->
  <xsl:template match="/">

    <!-- We cannot rely on namespaces being present in the records. Therefore everything at content level has namespaces removed. This makes it possible to work with PBCore
    metadata defined as PBCoreDescriptionDocument and PBCoreDescriptionDocument:PBCoreDescriptionDocument.-->
    <xsl:variable name="contentObjects">
      <!--/XIP/Metadata-->
      <xsl:for-each select="/XIP/Metadata/Content">
        <xsl:apply-templates mode="strip-ns"/>
      </xsl:for-each>
      <xsl:value-of select="."/>
    </xsl:variable>

    <!-- As above, we are removing the namespaces for everything inside the PBCoreDescriptionDocument as some records have ns1, ns2, ns3 and so on for the same field. -->
    <xsl:variable name="pbCore">
          <!-- TODO: RECORDS SHOULD ONLY HAVE ONE OF THESE. HOWEVER RECORD WITH ID: 382c7e23-06b9-42c0-8857-c0b898235bb2 HAS TWO. Choosing the first one until Karen is back from
           holiday.-->
          <xsl:for-each select="$contentObjects/PBCoreDescriptionDocument[1]">
            <xsl:apply-templates mode="strip-ns"/>
          </xsl:for-each>
          <xsl:value-of select="."/>
    </xsl:variable>

    <!-- Determine the type of schema.org object in hand.-->
    <xsl:variable name="type">
      <xsl:choose>
        <!-- /XIP/Metadata[1]/Content/ns2:PBCoreDescriptionDocument/ns2:pbcoreInstantiation/ns2:formatMediaType-->
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
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$type = 'AudioObject'">
          <xsl:call-template name="audio-transformation">
            <xsl:with-param name="pbCore" select="$pbCore"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="generic-transformation">
            <xsl:with-param name="pbCore" select="$pbCore"/>
            <xsl:with-param name="type" select="$type"/>
            <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

    </xsl:variable>

    <!-- Wrapping the xml-to-json function in a  -->
    <xsl:try>
      <xsl:value-of select="f:xml-to-json($json)"/>
      <xsl:catch errors="*">
        <xsl:variable name="errorDoc">
          <f:map>
            <!-- First three fields for schema.org are these no matter which object the transformer transforms to. -->
            <xsl:call-template name="schema-context-and-type">
              <xsl:with-param name="type" select="$type"/>
            </xsl:call-template>
            <xsl:call-template name="origin-identifier"/>
            <xsl:call-template name="error-internal-map">
              <xsl:with-param name="errorDescription" select="concat($err:code, ': ', $err:description)"/>
            </xsl:call-template>
          </f:map>
        </xsl:variable>
        <xsl:value-of select="f:xml-to-json($errorDoc)"/>
      </xsl:catch>
    </xsl:try>
  </xsl:template>

  <!-- TEMPLATE FOR TRANSFORMING VIDEOOBJECTS. The template requires the following two parameters:
        type: The type of schema-org object in hand.
        pbcExtensions: A parameter containing all PBCore Extensions for better retrieval of specific extensions during
                       the transformation. -->
  <xsl:template name="video-transformation">
    <xsl:param name="pbCore"/>
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>

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
          </xsl:call-template>

          <!-- Extract actors if any present in metadata. see https://schema.org/actor and the JSON.LD example -->
          <xsl:if test="$pbCore/pbcoreContributor/contributorRole = 'medvirkende' and ./pbcoreContributor/contributor != ''">
            <f:array key="actor">
              <xsl:for-each select="./pbcoreContributor">
                <xsl:if test="./contributorRole = 'medvirkende' and ./contributor != ''">
                  <f:map>
                    <f:string key="@type">PerformanceRole</f:string>
                    <xsl:choose>
                      <!-- When contributor contains a ':' it means that the character on the left is played by the actor on the right of the ':'. In this case we are creating
                            a Person object and a characterName string. -->
                      <xsl:when test="contains(./contributor, ':')">
                        <f:map key="actor">
                          <f:string key="@type">Person</f:string>
                          <f:string key="name">
                            <xsl:value-of select="normalize-space(substring-after(./contributor, ':'))"/>
                          </f:string>
                        </f:map>
                        <f:string key="characterName">
                          <xsl:value-of select="normalize-space(substring-before(./contributor, ':'))"/>
                        </f:string>
                      </xsl:when>
                      <!-- When contributor doesn't contain a ':' we dont know anything about the character and therefore we aren't creating a characterName string but using the full
                      content as name for the Person. -->
                      <xsl:when test="not(contains(./contributor, ':') and ./contributor != '')">
                        <f:map key="actor">
                          <f:string key="@type">Person</f:string>
                          <f:string key="name">
                            <xsl:value-of select="normalize-space(./contributor)"/>
                          </f:string>
                        </f:map>
                      </xsl:when>
                    </xsl:choose>
                  </f:map>
                </xsl:if>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <!-- Is the resource hd? or do we know anything about the video quality=? -->
          <xsl:if test="//pbcoreInstantiation/formatStandard != ''">
            <f:string key="videoQuality"><xsl:value-of select="//pbcoreInstantiation/formatStandard"/></f:string>
          </xsl:if>

          <!-- Extract aspect ratio -->
          <!-- Aspect ratio contains many dirty values. such as ',', ', ', '16:9,' and '16:9, '. -->
          <xsl:choose>
            <xsl:when test="normalize-space($pbCore/pbcoreInstantiation/formatAspectRatio) = ','"/>
            <xsl:when test="normalize-space($pbCore/pbcoreInstantiation/formatAspectRatio) = '16:9,'">
              <f:string key="videoFrameSize">16:9</f:string>
            </xsl:when>
            <xsl:when test="$pbCore/pbcoreInstantiation/formatAspectRatio != ''">
              <f:string key="videoFrameSize">
                <xsl:value-of select="$pbCore/pbcoreInstantiation/formatAspectRatio"/>
              </f:string>
            </xsl:when>
            <!-- If the field doesn't exist, don't do anything -->
            <xsl:otherwise></xsl:otherwise>
          </xsl:choose>

        </xsl:for-each>

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

        <!-- Video-only extension fields, each found in the whole extension set: showviewcode carries a
             value; the subtitle / teletext / hearing-impaired flags are booleans. Handled elsewhere for
             non-video records via kb-internal above.
             TODO: check whether has_subtitles / subtitles-for-hearing-impaired can be described in schema.org. -->
        <xsl:sequence select="my:extensionStringField($pbcExtensions, 'showviewcode', 'kb:showviewcode')"/>
        <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'tekstet', 'tekstet', 'ikke tekstet', 'kb:has_subtitles')"/>
        <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'th', 'tekstet for hørehæmmede', 'ikke tekstet for hørehæmmede', 'kb:has_subtitles_for_hearing_impaired')"/>
        <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'ttv', 'tekst-tv', 'ikke tekst-tv', 'kb:is_teletext')"/>
        </f:map>

        <!-- Catches all errors in sequence constructors (places where data can be used as input). If an error occurs
             the field origin will be created and internal fields about the error will be created as well. -->
        <xsl:catch errors="*">
          <xsl:call-template name="origin-identifier"/>
          <xsl:call-template name="error-internal-map">
            <xsl:with-param name="errorDescription" select="concat($err:code, ': ', $err:description)"/>
          </xsl:call-template>
        </xsl:catch>
      </xsl:try>

    </f:map>
  </xsl:template>

  <!-- TEMPLATE FOR TRANSFORMING AUDIOOBJECTS. Parameters:
        pbCore:        The PBCore metadata document node.
        type:          The type of schema-org object in hand (here always 'AudioObject').
        pbcExtensions: All PBCore Extensions, for retrieval of specific extensions during the transformation. -->
  <xsl:template name="audio-transformation">
    <xsl:param name="pbCore"/>
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>

    <xsl:call-template name="generic-transformation">
      <xsl:with-param name="pbCore" select="$pbCore"/>
      <xsl:with-param name="type" select="$type"/>
      <xsl:with-param name="pbcExtensions" select="$pbcExtensions"/>
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
              </xsl:call-template>


              <!-- Contributors with role 'medvirkende' as a schema.org person array.
                   see https://schema.org/contributor and the JSON.LD example -->
              <xsl:sequence select="my:personArray($pbCore/pbcoreContributor[contributorRole = 'medvirkende' and contributor != '']/contributor, 'contributor')"/>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="origin-identifier"/>
          </xsl:otherwise>
        </xsl:choose>

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
        <xsl:catch>
          <xsl:call-template name="origin-identifier"/>
          <xsl:call-template name="error-internal-map">
            <xsl:with-param name="errorDescription" select="$err:description"/>
          </xsl:call-template>
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
  </xsl:template>

  <!-- The 'identifier' array carrying only the Origin PropertyValue. Emitted on its own in the
       error/fallback paths, where the full identifier array (with RecordID etc.) is not available. -->
  <xsl:template name="origin-identifier">
    <f:array key="identifier">
      <f:map>
        <f:string key="@type">PropertyValue</f:string>
        <f:string key="PropertyID">Origin</f:string>
        <f:string key="value"><xsl:value-of select="$origin"/></f:string>
      </f:map>
    </f:array>
  </xsl:template>

  <!-- The 'kb:internal' map produced when a transformation error is caught: the storage mTime plus
       the error flag and description. The description differs per call site, so it is passed in
       (it must be computed where the err: error variables are in scope, i.e. inside xsl:catch). -->
  <xsl:template name="error-internal-map">
    <xsl:param name="errorDescription"/>
    <f:map key="kb:internal">
      <!-- Internal value for backing ds-storage mTime-->
      <f:string key="kb:storage_mTime">
        <xsl:value-of select="format-number($mTime, '0')"/>
      </f:string>
      <f:string key="kb:transformation_error"><xsl:value-of select="true()"/></f:string>
      <f:string key="kb:transformation_error_description"><xsl:value-of select="$errorDescription"/></f:string>
    </f:map>
  </xsl:template>


  <!-- TEMPLATE FOR ACCESSING PBCORE METADATA. This template transforms all fields, that are relevant for all objects.
       Fields such as 'videoQuality' is not part of the template extraction and are extracted in the video-transformation
       template.-->
  <xsl:template name="pbc-metadata">
    <xsl:param name="type"/>
    <xsl:param name="pbcExtensions"/>
    <!-- TODO: Investigate relation between titel and originaltitel. Some logic related to metadata delivery type exists. -->
    <!-- Create fields headline and alternativeHeadline if needed.
         Determine if title and original title are alike. Both fields should always be in metadata -->
    <!-- TODO: Do some validation of titles - check with metadata schema when they are set.    -->
    <xsl:variable name="title" select="string-join(pbcoreTitle[titleType = 'titel']/title, ' ')"/>
    <xsl:variable name="original-title" select="string-join(pbcoreTitle[titleType = 'originaltitel']/title, ' ')"/>

    <xsl:choose>
      <xsl:when test="$title = $original-title and $title != '' or ($title != '' and $original-title = '')">
        <f:string key="name">
          <xsl:value-of select="normalize-space($title)"/>
        </f:string>
      </xsl:when>
      <xsl:when test="$title = '' and $original-title != ''">
        <f:string key="name">
          <xsl:value-of select="normalize-space($original-title)"/>
        </f:string>
      </xsl:when>
      <xsl:otherwise>
        <f:string key="name">
          <xsl:value-of select="normalize-space($title)"/>
        </f:string>
        <f:string key="alternateName">
          <xsl:value-of select="normalize-space($original-title)"/>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Publisher extraction. Some metadata has two pbcorePublisher/publisher/publisherRole.
      We use the one with the value "kanalnavn" as this should be present in all metadata files.-->
    <xsl:variable name="publisherSpecificIfExists" select="string-join(pbcorePublisher[publisherRole = 'kanalnavn']/publisher, ' ')"/>
    <xsl:variable name="publisherGeneralIfExists" select="string-join(pbcorePublisher[publisherRole = 'channel_name']/publisher, ' ')"/>

    <xsl:variable name="publisherSpecific">
      <xsl:choose>
        <xsl:when test="string($publisherSpecificIfExists)">
          <xsl:value-of select="$publisherSpecificIfExists"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$publisherGeneralIfExists"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="publisherGeneral">
      <xsl:choose>
        <xsl:when test="string($publisherGeneralIfExists)">
          <xsl:value-of select="$publisherGeneralIfExists"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$publisherSpecific"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:if test="./pbcorePublisher">
      <f:map key="publication">
        <f:string key="@type">BroadcastEvent</f:string>
        <!-- Define isLiveBroadcast from the 'live' extension: live:live -> true, live:ikke live -> false,
             nothing when absent. Same helper as the kb:* booleans (exact match over the whole extension set).
             TODO: Figure out what to do when the live field isn't present in metadata. -->
        <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'live', 'live', 'ikke live', 'isLiveBroadcast')"/>
        <!-- Preservica contains two different fields for broadcaster-->
        <xsl:if test="$publisherSpecific != ''">
          <f:map key="publishedOn">
            <f:string key="@type">BroadcastService</f:string>
            <f:string key="broadcastDisplayName">
              <xsl:choose>
                <xsl:when test="f:starts-with($publisherSpecific, '_')">
                  <xsl:value-of select="substring($publisherSpecific, 2)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$publisherSpecific"/>
                </xsl:otherwise>
              </xsl:choose>
            </f:string>
            <!-- $publisherGeneral is a result-tree variable so it always exists/is non-empty; only its
                 string value matters. -->
            <xsl:if test="$publisherGeneral != '' or $publisherSpecific = 'DR1' or $publisherSpecific = 'DR2'">
            <f:string key="alternateName">
              <xsl:choose>
                <!-- Do clean up of DR channel names -->
                <xsl:when test="f:starts-with($publisherGeneral, 'dr')">
                  <xsl:value-of select="my:cleanDrChannel($publisherGeneral)"/>
                </xsl:when>
                <!-- In some cases records from DR1 and DR2 doesn't contain a value in the field $publisherGeneral, when this happens and the value in $publisherSpecific is
                either DR1 or DR2, then these values should be used for generation of alternateName. -->
                <xsl:when test="$publisherSpecific = 'DR1' or $publisherSpecific = 'DR2'">
                  <xsl:value-of select="my:cleanDrChannel($publisherSpecific)"/>
                </xsl:when>
                <!-- Plain usage of alterneName which isn't going to be used before other collections than DR are in the system -->
                <xsl:otherwise>
                  <xsl:value-of select="$publisherGeneral"/>
                </xsl:otherwise>
              </xsl:choose>
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

    <!-- True if either 'produktionsland' or 'produktionsland_id' is present in metadata.
         Kept on contains() (not my:valueFromPBCoreExtensionKey) so an empty-valued extension still yields a bare
         Country map, exactly as before. -->
    <xsl:variable name="produktionslandBoolean"
                  select="exists($pbcExtensions[f:contains(., 'produktionsland:') or f:contains(., 'produktionsland_id:')])"/>

    <!-- Create country of origin and add the identifier for the production country as text. -->
    <xsl:if test="$produktionslandBoolean">
      <f:map key="countryOfOrigin">
        <f:string key="@type">Country</f:string>
        <xsl:for-each select="./pbcoreExtension/extension">
          <!-- A single extension is either produktionsland:X or produktionsland_id:Y, so the
               two branches are mutually exclusive and can be independent xsl:if. -->
          <xsl:if test="my:valueFromPBCoreExtensionString(., 'produktionsland')">
            <f:string key="name">
              <xsl:value-of select="my:valueFromPBCoreExtensionString(., 'produktionsland')"/>
            </f:string>
          </xsl:if>
          <xsl:if test="my:valueFromPBCoreExtensionString(., 'produktionsland_id')">
            <f:string key="identifier">
              <xsl:value-of select="my:valueFromPBCoreExtensionString(., 'produktionsland_id')"/>
            </f:string>
          </xsl:if>
        </xsl:for-each>
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
        <f:string key="@type"><xsl:value-of select="my:episodeType($type)"/></f:string>

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
                  <xsl:value-of select="normalize-space(substring-after(., 'episodenr:'))"/>
                </xsl:variable>

              <xsl:variable name="episodeNumber">
                  <xsl:value-of select="number(normalize-space(substring-before($episodeInfo, ':')))"/>
                </xsl:variable>

                <xsl:if test="string($episodeNumber) != 'NaN'">

                  <f:number key="episodeNumber">
                    <xsl:value-of select="$episodeNumber"/>
                  </f:number>
                </xsl:if>

                <xsl:sequence select="my:partOfSeason(substring-after($episodeInfo, ':'), $type)"/>
            </xsl:when>
            <xsl:otherwise>
              <!-- Separate 'episodenr:N' / 'antalepisoder:M' extensions (not the combined episodenr:N:M form). -->
              <!-- Episode number, only when it is a plain integer - not e.g. '2+3' meaning episodes 2 and 3. -->
              <xsl:if test="f:contains(., 'episodenr:') and f:string-length(normalize-space(substring-after(., 'episodenr:'))) > 0">
                <xsl:variable name="episodeNumber">
                    <xsl:value-of select="normalize-space(substring-after(., 'episodenr:'))"/>
                </xsl:variable>
                <xsl:if test="string($episodeNumber) != 'NaN' and matches($episodeNumber, '^\d+$')">
                  <f:number key="episodeNumber">
                    <xsl:value-of select="substring-after(., 'episodenr:')"/>
                  </f:number>
                </xsl:if>
              </xsl:if>

              <!-- Number of episodes in the season, when 'antalepisoder' has a non-empty, non-zero value.
                   TODO: Figure if there is a difference between no value and 0. Could one mean a series is
                   related but no data on it, and the other an individual program with no series? -->
              <xsl:if test="f:contains(., 'antalepisoder:') and
                        not(f:contains(., 'antalepisoder:0')) and
                        f:string-length(substring-after(., 'antalepisoder:')) > 0">
                <xsl:sequence select="my:partOfSeason(substring-after(., 'antalepisoder:'), $type)"/>
              </xsl:if>
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

    <!-- only extract non empty annotations -->
    <xsl:for-each select="./pbcoreInstantiation/pbcoreAnnotation[normalize-space(annotation) != '']">
      <f:string key="annotation">
        <xsl:value-of select="normalize-space(./annotation)"/>
      </f:string>
    </xsl:for-each>

    <!-- Extract start and end times for broadcast  and calculate duration -->
    <xsl:if test="$startTime != '' and $endTime != ''">
      <f:string key="startTime">
        <xsl:value-of select="$startTime"/>
      </f:string>
      <f:string key="endTime">
        <xsl:value-of select="$endTime"/>
      </f:string>

      <!-- Schema.org refers to the wiki page for ISO8601 and actually wants the duration in the format PT12M50S
           for a duration of 12 minutes and 50 seconds -->
      <f:string key="duration">
        <xsl:value-of select="xs:dateTime($endTime) - xs:dateTime($startTime)"/>
      </f:string>
    </xsl:if>

    <!-- Construct keywords list from all genre fields. Seperates entries by comma and removes last comma.
         Also extracts maingenre to the schema.org field 'genre'. Values here are checked against variables of "mapping values" mapping broader categories to simpler UX
         categories. These simpler categories are then used as genre. IMPORTANT all values added to these categories are to be lower cased to match in the
         sequenceAContainsValueFromSequenceB-method.-->
    <xsl:choose>
      <xsl:when test="//pbcoreGenre">
        <!-- Variables containing values that are to be mapped to a simpler combined value.-->
        <!-- These values should map to: Nyheder, politik og samfund-->
        <xsl:variable name="NewsPoliticsSociety" as="item()*"
                      select="('nyheder &amp; aktualitet', 'vejrudsigt', 'regional', 'forbruger', 'økonomi og erhvervsforhold', 'samfundsforhold (fakta)',
                              'samfundsforhold i et land', 'aktualitet og debat', 'nyheder', 'politiske forhold', 'de politiske partier', 'internationale forhold', 'skoleforhold')"/>
        <!-- These values should map to: Musik-->
        <xsl:variable name="Music" as="item()*" select="('musik', 'kor- og orkestervirksomhed', 'opera', 'rytmisk musik', 'populær musik', 'popmusik', 'populær musik popmusik')"/>
        <!-- These values should map to: Kultur og oplysning-->
        <xsl:variable name="Culture" as="item()*"
                      select="('kultur', 'religion', 'undervisning', 'historie og kulturhistorie', 'oplysning og kultur', 'dramatik og fiktion', 'udsendelsesvirksomhed', 'livsberetninger og skæbner',
                              'natur og dyr', 'mad og drikke', 'undervisning og kultur', 'medier', 'forbrugerstof', 'litteratur', 'forkyndende sangprogrammer', 'kulturforhold', 'sjælelivet')"/>
        <!-- These values should map to: Sport-->
        <xsl:variable name="Sport" as="item()*"
                      select="('sport', 'blandet sport - nyhedspræget uden', 'fodbold', 'hestesport (gallop trav ridebane)', 'atletik', 'badminton.', 'boksning.', 'blandet sport - nyhedspræget med',
                              'bordtennis.', 'cykling.', 'dansk klub-fodbold', 'dansk klub-håndbold - herrer',
                              'dansk klub-håndbold - kvinder', 'danske fodboldlandskampe', 'danske håndboldlandskampe - kvinder', 'engelsk klub-fodbold.',
                              'europa cup-kampe - herrer', 'europa cup-kampe - kvinder', 'europacup-fodbold', 'folkelig idræt (herunder f.eks.-', 'golf.', 'gymnastik.', 'håndbold.', 'ishockey.',
                              'kano/kajak/roning.', 'motorsport.', 'ol- em og vm-fodboldlandskampe', 'ol- em- og vm-håndboldkampe – herrer', 'ol- em- og vm-håndboldkampe – kvinder', 'sejlsport.','skisport.',
                              'speedway.', 'sportsdans.', 'sportstema - blandet sport uden', 'svømning.', 'taekwon-do o.lign.', 'tennis.', 'vintersport.', 'volleyball.'"/>
        <!-- These values should map to: Underholdning-->
        <xsl:variable name="Entertainment" as="item()*"
                      select="('underholdning', 'tips &amp; lotto', 'anden underholdning', 'individet', 'comedy/situation comedy', 'journalistisk underholdning', 'humor', 'quiz',
                              'shows', 'farce/spoof', 'quiz game (hvis ingen emnekategorisering)')"/>
        <!-- These values should map to: Børn og unge-->
        <xsl:variable name="ChildrenYouth" as="item()*" select="('børn &amp; ungdom', 'dyr med central rolle', 'eventyr')"/>
        <!-- These values should map to: Dokumentar-->
        <xsl:variable name="Documentary" as="item()*" select="('dokumentar', 'miljø')"/>
        <!-- These values should map to: Fiktion-->
        <xsl:variable name="Fiction" as="item()*"
                      select="('film', 'serie', 'serier', 'spænding', 'psykologisk', 'socialt og historisk drama', 'Trillers: Krimi', 'detektiv', 'spion', 'fiktion', 'socialt drama', 'psykologisk drama', 'soap hverdagsliv')"/>
        <!-- These values should map to: Livsstil-->
        <xsl:variable name="Lifestyle" as="item()*" select="('fritid &amp; livsstil', 'sundhed &amp; mad')"/>
        <!-- These values should map to: Videnskab og natur-->
        <xsl:variable name="ScienceNature" as="item()*"
                      select="('videnskab &amp; forskning', 'videnskab &amp; teknologi', 'natur &amp; miljø', 'natur', 'natur og kultur (fakta)', 'sundhed', 'naturvidenskab')"/>
        <!-- The former 'Misc'/'Diverse' category list ('alle', 'andet', 'blandet', ...) was removed: it
             mapped to the same rodekasse bucket as the no-match fall-through, so it never affected output. -->

        <!-- Save keywords as a sequence -->
        <xsl:variable name="keywordsSequence" as="item()*">
          <xsl:for-each select="./pbcoreGenre/genre">
            <xsl:choose>
              <xsl:when test="f:contains(., ':') and substring-after(., ':') != '' and not(f:contains(., 'null'))">
                <xsl:value-of select="tokenize(f:substring-after(., ':'), ',')"/>
              </xsl:when>
              <xsl:when test="not(f:contains(., ':')) and not(f:contains(., 'null'))">
                <xsl:value-of select="."/>
              </xsl:when>
            </xsl:choose>
          </xsl:for-each>
        </xsl:variable>

        <xsl:variable name="keywordsString">
          <xsl:value-of select="normalize-space(f:string-join($keywordsSequence, ', '))"/>
        </xsl:variable>

        <xsl:choose>
          <xsl:when test="$keywordsString != ''">
            <f:string key="keywords">
              <xsl:value-of select="$keywordsString"/>
            </f:string>
            <!-- Genre categories, in priority order: the first whose value list intersects the record's
                 keywords wins. An unmatched genre (formerly incl. the removed 'Misc' list) falls back to
                 the rodekasse bucket. Add/adjust a category by editing one row here. -->
            <xsl:variable name="genreCategories" as="map(*)*" select="(
              map { 'label': 'Nyheder, politik og samfund', 'values': $NewsPoliticsSociety },
              map { 'label': 'Musik',                        'values': $Music },
              map { 'label': 'Kultur og oplysning',          'values': $Culture },
              map { 'label': 'Sport',                        'values': $Sport },
              map { 'label': 'Humor, quiz og underholdning', 'values': $Entertainment },
              map { 'label': 'Børn og unge',                 'values': $ChildrenYouth },
              map { 'label': 'Dokumentar',                   'values': $Documentary },
              map { 'label': 'Film og serier',               'values': $Fiction },
              map { 'label': 'Livsstil',                     'values': $Lifestyle },
              map { 'label': 'Natur og videnskab',           'values': $ScienceNature })"/>
            <xsl:variable name="matchedGenre" as="xs:string?"
                          select="(for $cat in $genreCategories
                                   return if (my:sequenceAContainsValueFromSequenceB($keywordsSequence, $cat?values))
                                          then $cat?label else ())[1]"/>
            <xsl:variable name="genreValue" select="($matchedGenre, my:rodekasse($type))[1]"/>
            <f:string key="genre">
              <xsl:value-of select="$genreValue"/>
            </f:string>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <!-- Adding a fallback to 'Rodekassen' as we have 160K records without genre at all. -->
      <xsl:otherwise>
        <f:string key="genre"><xsl:value-of select="my:rodekasse($type)"/></f:string>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Extract directors if any present in metadata. see https://schema.org/director -->
    <!-- Directors: contributors with role 'instruktion' as a schema.org person array. -->
    <xsl:sequence select="my:personArray(pbcoreContributor[contributorRole = 'instruktion' and contributor != '']/contributor, 'director')"/>

    <!-- Creators/authors: pbcoreCreator with role 'forfatter' as a schema.org person array (creator is
         also used for images, hence the shared field). -->
    <xsl:sequence select="my:personArray(pbcoreCreator[creatorRole = 'forfatter' and creator != '']/creator, 'creator')"/>

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
      <xsl:if test="normalize-space($productionId) != ''">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">ProductionID</f:string>
          <f:string key="value"><xsl:value-of select="normalize-space($productionId)"/></f:string>
          <f:string key="description">DRs internal production ID of the record.</f:string>
        </f:map>
      </xsl:if>
      <xsl:if test="$kalturaID != ''">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">KalturaID</f:string>
          <f:string key="value"><xsl:value-of select="$kalturaID"/></f:string>
          <f:string key="description">Kaltura ID of the access copy. Created internally by Kaltura.</f:string>
        </f:map>
      </xsl:if>
      <!-- Skip identifiers with an empty source or value (the [not(...)] predicate); the rest map to a
           PropertyValue, with a special PropertyID for the 'De hvide programmer' source. -->
      <xsl:for-each select="./pbcoreIdentifier[not(identifierSource = '') and not(identifier = '')]">
        <xsl:choose>
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
      <xsl:if test="$InternalAccessionRef != ''">
        <f:map>
          <f:string key="@type">PropertyValue</f:string>
          <f:string key="PropertyID">InternalAccessionRef</f:string>
          <f:string key="value"><xsl:value-of select="$InternalAccessionRef"/></f:string>
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

  <!-- Build a schema.org person array (creator / director / contributor): one {@type:Person, name} map
       per name in $names, or nothing when the sequence is empty. Callers pass the already-filtered name
       nodes, e.g. pbcoreCreator[creatorRole = 'forfatter' and creator != '']/creator. -->
  <xsl:function name="my:personArray" visibility="public">
    <xsl:param name="names" as="xs:string*"/>
    <xsl:param name="outputKey" as="xs:string"/>
    <xsl:if test="exists($names)">
      <f:array key="{$outputKey}">
        <xsl:for-each select="$names">
          <f:map>
            <f:string key="@type">Person</f:string>
            <f:string key="name"><xsl:value-of select="normalize-space(.)"/></f:string>
          </f:map>
        </xsl:for-each>
      </f:array>
    </xsl:if>
  </xsl:function>

  <!-- schema.org type labels derived from the record's object type ($type: VideoObject / AudioObject / other). -->
  <xsl:function name="my:episodeType" as="xs:string">
    <xsl:param name="type" as="xs:string?"/>
    <xsl:sequence select="if ($type = 'VideoObject') then 'TVEpisode'
                          else if ($type = 'AudioObject') then 'RadioEpisode'
                          else 'Episode'"/>
  </xsl:function>
  <xsl:function name="my:seasonType" as="xs:string">
    <xsl:param name="type" as="xs:string?"/>
    <xsl:sequence select="if ($type = 'VideoObject') then 'TVSeason'
                          else if ($type = 'AudioObject') then 'RadioSeason'
                          else 'CreativeWorkSeason'"/>
  </xsl:function>
  <!-- The 'rodekasse' catch-all genre bucket; empty for non tv/radio types, matching the original choose. -->
  <xsl:function name="my:rodekasse" as="xs:string">
    <xsl:param name="type" as="xs:string?"/>
    <xsl:sequence select="if ($type = 'VideoObject') then 'TV-rodekasse'
                          else if ($type = 'AudioObject') then 'Radio-rodekasse'
                          else ''"/>
  </xsl:function>

  <!-- The encodesCreativeWork/partOfSeason map. $numberOfEpisodes is emitted verbatim as numberOfEpisodes,
       but only when it parses to a number (matching the original guard). Used for both the combined
       'episodenr:N:M' form and the separate 'antalepisoder:M' form. -->
  <xsl:function name="my:partOfSeason">
    <xsl:param name="numberOfEpisodes" as="xs:string?"/>
    <xsl:param name="type" as="xs:string?"/>
    <f:map key="partOfSeason">
      <f:string key="@type"><xsl:value-of select="my:seasonType($type)"/></f:string>
      <xsl:if test="string(number(normalize-space($numberOfEpisodes))) != 'NaN'">
        <f:number key="numberOfEpisodes"><xsl:value-of select="$numberOfEpisodes"/></f:number>
      </xsl:if>
    </f:map>
  </xsl:function>


  <!-- TEMPLATE FOR EXTRACTING INTERNAL VALUES WHICH DON'T HAVE A SCHEMA.ORG DATA REPRESENTATION.
       These values can be almost anything ranging from identifiers to acces conditions.
       This kb:internal map was how we've handled internal values in the past, see line 109 in this file:
       https://github.com/kb-dk/ds-present/blob/spolorm-now-works/src/main/resources/xslt/mods2schemaorg.xsl -->
  <xsl:template name="kb-internal">
    <xsl:param name="pbCore"/>
    <xsl:param name="pbcExtensions"/>
    <xsl:param name="type"/>

    <xsl:if test="$platform != ''">
      <f:string key="kb:platform">
        <xsl:value-of select="$platform"/>
      </f:string>
    </xsl:if>

   <xsl:if test="$transcription != ''">
      <f:string key="kb:transcription">
        <xsl:value-of select="$transcription"/>
      </f:string>
    </xsl:if>
   
   <xsl:if test="$has_transcription != ''">
      <f:string key="kb:has_transcription">
        <xsl:value-of select="$has_transcription"/>
      </f:string>
    </xsl:if>
    
    <!-- Boolean value which determins if the record has a stream available at Kaltura.-->
    <f:boolean key="kb:has_kaltura_id">
      <xsl:value-of select="$kalturaID != ''"/>
    </f:boolean>

    <!-- Extration of migration details if present. Implemented as a choose statement. -->
    <xsl:variable name="migrationSource">
      <xsl:value-of select="/XIP/Metadata/Content/migration_details/migrated_from"/>
    </xsl:variable>
    <f:string key="kb:originates_from">
      <xsl:choose>
        <xsl:when test="normalize-space($migrationSource) = 'Radio/tv DOMS - prod'">
          <xsl:value-of select="'DOMS'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'Preservica'"/>
        </xsl:otherwise>
      </xsl:choose>
    </f:string>

    <!-- If record originates from DOMS, we have to check if an access copy has been created by DOMS. That's whats happening inside this if-statement.
          This is done in the same way as mediestream did it.-->
    <xsl:if test="normalize-space($migrationSource) = 'Radio/tv DOMS - prod'">
      <xsl:variable name="maxMissingSeconds" select="90"/>

      <f:string key="kb:has_doms_access_copy">
        <xsl:choose>
          <!-- When access/defekt is Ja, then there is no access copy for the DOMS record -->
          <xsl:when test="/XIP/Metadata/Content/access/defekt = 'Ja'">
            <xsl:value-of select="false()"/>
          </xsl:when>
          <!-- When there is a progam structure object present, there is a presentation copy present, however it might be so bad, that it cannot be shown.
                The default configuration for missing seconds from the old transcoder was 120 for not generating the access copy. Mediestream set the value to 90, which
                is the one that we are reusing here to make sure all programs delivered are watchable.-->
          <xsl:when test="/XIP/Metadata/Content/program_structure">
            <!-- Each element in the program structure has to be analysed. This is done as in mediestream, where 90 seconds are allowed to be missing in each of the fields.
                  If the value is greater than that, then we dont want the program to be shown.-->
            <xsl:for-each select="/XIP/Metadata/Content/program_structure">
              <xsl:choose>
                <xsl:when test="holes/hole/holeLength[text() &gt; $maxMissingSeconds]"><xsl:value-of select="false()"/></xsl:when>
                <xsl:when test="missingStart/missingSeconds[text() &gt; $maxMissingSeconds]"><xsl:value-of select="false()"/></xsl:when>
                <xsl:when test="missingEnd/missingSeconds[text() &gt; $maxMissingSeconds]"><xsl:value-of select="false()"/></xsl:when>
                <!-- A program exists and there is not more than 90 seconds missing from each element above. -->
                <xsl:otherwise><xsl:value-of select="true()"/></xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
          </xsl:when>
          <!-- If the program structure doesn't exist and the record originates from DOMS, then an access copy haven't been created. -->
          <xsl:otherwise>
            <xsl:value-of select="false()"/>
          </xsl:otherwise>
        </xsl:choose>
      </f:string>
    </xsl:if>

    <!-- Internal value for backing ds-storage mTime-->
    <f:string key="kb:storage_mTime">
      <xsl:value-of select="format-number($mTime, '0')"/>
    </f:string>

    <xsl:if test="$referenceId">
      <xsl:for-each select="/XIP/Metadata[@schemaUri = 'http://id.kb.dk/schemas/radiotv_access/transcoding_status']/Content/radiotvTranscodingStatus/
            specificRadioTvTranscodingStatus[contains(accessFilePath, $referenceId)][1]">
        <f:string key="kb:file_id">
          <xsl:value-of select="$referenceId"/>
        </f:string>
        <f:string key="kb:file_path">
          <xsl:value-of select="accessFilePath"/>
        </f:string>
        <f:string key="kb:file_extension">
          <xsl:value-of select="fileExtension"/>
        </f:string>
      </xsl:for-each>
    </xsl:if>

    <xsl:if test="$productionIdRestrictedDr != ''">
      <f:boolean key="kb:dr_id_restricted">
        <xsl:value-of select="$productionIdRestrictedDr"/>
      </f:boolean>
    </xsl:if>

    <xsl:if test="$dsIdRestricted != ''">
      <f:boolean key="kb:ds_id_restricted">
        <xsl:value-of select="$dsIdRestricted"/>
      </f:boolean>
    </xsl:if>

      <xsl:if test="$titleRestricted != ''">
          <f:boolean key="kb:title_restricted">
              <xsl:value-of select="$titleRestricted"/>
          </f:boolean>
      </xsl:if>

    <!-- Extract subgenre if present
        <pbcoreGenre>
          <genre>undergenre: Alle</genre>
        </pbcoreGenre> -->
    <xsl:for-each select="$pbCore/pbcoreGenre/genre">
      <xsl:if test="my:valueFromPBCoreExtensionString(., 'undergenre')">
        <f:string key="kb:genre_sub">
          <xsl:value-of select="normalize-space(my:valueFromPBCoreExtensionString(., 'undergenre'))"/>
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
    <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'premiere', 'premiere', 'ikke premiere', 'kb:premiere')"/>
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
        <xsl:when test="formatIdentifierSource = 'tvmeter'">
          <f:string key="kb:format_identifier_tvmeter">
            <xsl:value-of select="formatIdentifier"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
    <!--TODO: Figure if retransmission can fit into real schema.org -->
    <!-- Create boolean for retransmission-->
    <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'genudsendelse', 'genudsendelse', 'ikke genudsendelse', 'kb:retransmission')"/>
    <!-- Boolean for whether there was a stop in the transmission -->
    <xsl:sequence select="my:extensionBooleanField($pbcExtensions, 'program_ophold', 'program ophold', 'ikke program ophold', 'kb:program_ophold')"/>
    <!-- Extension-derived KB id fields. Each is the value of its "prefix:value" extension, found in the
         whole extension set. kanalid is the exception: it emits a number resolved against the sibling
         extensionAuthorityUsed, so it needs the pbcoreExtension node, not just the extension text. It is
         placed between maingenre and program to keep the order the (order-sensitive) Java assertions pin. -->
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'hovedgenre_id', 'kb:maingenre_id')"/>
    <xsl:for-each select="$pbCore/pbcoreExtension[f:starts-with(extension , 'kanalid:') and f:string-length(normalize-space(substring-after(extension , 'kanalid:'))) > 0]">
      <xsl:variable name="channelId">
        <xsl:value-of select="number(normalize-space(substring-after(extension , 'kanalid:')))"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="extensionAuthorityUsed = 'ritzau' and string($channelId) != 'NaN'">
          <f:number key="kb:ritzau_channel_id">
            <xsl:value-of select="$channelId"/>
          </f:number>
        </xsl:when>
        <xsl:when test="extensionAuthorityUsed = 'nielsen' and string($channelId) != 'NaN'">
          <f:number key="kb:nielsen_channel_id">
            <xsl:value-of select="$channelId"/>
          </f:number>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'program_id', 'kb:ritzau_program_id')"/>
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'undergenre_id', 'kb:subgenre_id')"/>
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'afsnit_id', 'kb:episode_id')"/>
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'saeson_id', 'kb:season_id')"/>
    <xsl:sequence select="my:extensionStringField($pbcExtensions, 'serie_id', 'kb:series_id')"/>

    <!-- Extracts information on video padding. -->
    <xsl:for-each select="/XIP/Metadata/Content/padding:padding/paddingSeconds">
      <xsl:if test="position() = 1">
        <xsl:variable name="paddingSeconds">
          <xsl:value-of select="number(normalize-space(.))"/>
        </xsl:variable>
        <xsl:if test="string($paddingSeconds) != 'NaN'">
          <f:number key="kb:padding_seconds">
            <xsl:value-of select="$paddingSeconds"/>
          </f:number>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>


    <!-- Extracts access metadata to the internal kb map -->
    <xsl:for-each select="/XIP/Metadata/Content/access">
      <xsl:if test="position() = 1">
        <xsl:call-template name="access-template"/>
      </xsl:if>
    </xsl:for-each>

    <!-- Extracts information on the structure of the video component. -->
    <xsl:for-each select="/XIP/Metadata/Content/program_structure:program_structure">
      <xsl:call-template name="program-structure"/>
    </xsl:for-each>


    <xsl:if test="$productionCodeAllowed != ''">
      <f:boolean key="kb:production_code_allowed">
        <xsl:value-of select="$productionCodeAllowed"/>
      </f:boolean>
    </xsl:if>
    <xsl:if test="$productionCodeValue != '' and not(f:empty($productionCodeValue))">
      <xsl:if test="string(number(normalize-space($productionCodeValue))) != 'NaN'">
        <f:number key="kb:production_code_value">
          <xsl:value-of select="number(normalize-space($productionCodeValue))"/>
        </f:number>
      </xsl:if>
    </xsl:if>

    <!-- Create a field with a boolean value representing if the record has the extra dr_archive_supplementary_rights_metadata fragment -->
    <f:boolean key="kb:contains_dr_archive_supplementary_rights_metadata">
      <xsl:value-of select="exists(/XIP/Metadata[@schemaUri = 'http://id.kb.dk/schemas/dr_archive_supplementary_rights_metadata'])"/>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra tvmeter fragment -->
    <f:boolean key="kb:contains_tvmeter">
      <xsl:value-of select="exists(//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_tvmeter_metadata'])"/>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra nielsen fragment -->
    <f:boolean key="kb:contains_nielsen">
      <xsl:value-of select="exists(//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_nielsen_metadata'])"/>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra ritzau fragment -->
    <f:boolean key="kb:contains_ritzau">
      <xsl:value-of select="exists(//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_ritzau_metadata'])"/>
    </f:boolean>

    <!-- Holdback date included here. Holdback purpose is only included for video objects, therefor it is done in the
          internal-video-fields template. -->
    <xsl:if test="$holdbackDate != null or $holdbackDate != ''">
      <f:string key="kb:holdback_date">
        <xsl:value-of select="$holdbackDate"/>
      </f:string>
    </xsl:if>

  </xsl:template>

  <!-- Transforms internal fields, that are only present for tv/video metadata. These fields are:
       aspect_ratio and color.-->
  <xsl:template name="internal-video-fields">
    <xsl:param name="pbCore"/>

    <xsl:if test="$holdbackPurposeName != null or $holdbackPurposeName != ''">
      <f:string key="kb:holdback_name">
        <xsl:value-of select="$holdbackPurposeName"/>
      </f:string>
    </xsl:if>

    <xsl:if test="$holdbackFormValue != null or $holdbackFormValue != ''">
      <f:string key="kb:holdback_form_value">
        <xsl:value-of select="$holdbackFormValue"/>
      </f:string>
    </xsl:if>

    <xsl:if test="$holdbackContentValue != null or $holdbackContentValue != ''">
      <f:string key="kb:holdback_content_value">
        <xsl:value-of select="$holdbackContentValue"/>
      </f:string>
    </xsl:if>



    <!-- Create boolean for color for tv resources-->
    <f:boolean key="kb:color">
      <xsl:value-of select="$pbCore/pbcoreInstantiation/formatColors = 'farve'"/>
    </f:boolean>
  </xsl:template>

  <!-- EMIT A BOOLEAN FIELD FROM A  PBCore extension.
       Many extensions encode a boolean as an affirmative phrase or its negation, e.g.
       'tekstet:tekstet' (true) / 'tekstet:ikke tekstet' (false). Given the whole extension set, this
       emits <f:boolean key="{outputKey}"> true when the affirmative form is present, false when the
       negative form is present, and nothing otherwise. It tests the full set with an existential '=',
       so callers pass $pbcExtensions directly - no per-extension loop and no pre-filtering needed. The
       phrases are not derivable from the prefix (e.g. ttv -> 'tekst-tv'), so all three are passed in. -->
  <xsl:function name="my:extensionBooleanField" visibility="public">
    <xsl:param name="extensions" as="xs:string*"/>
    <xsl:param name="prefix" as="xs:string"/>
    <xsl:param name="affirmative" as="xs:string"/>
    <xsl:param name="nonAffirmative" as="xs:string"/>
    <xsl:param name="outputKey" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="$extensions = $prefix || ':' || $nonAffirmative">
        <f:boolean key="{$outputKey}"><xsl:value-of select="false()"/></f:boolean>
      </xsl:when>
      <xsl:when test="$extensions = $prefix || ':' || $affirmative">
        <f:boolean key="{$outputKey}"><xsl:value-of select="true()"/></f:boolean>
      </xsl:when>
    </xsl:choose>
  </xsl:function>

  <!-- Emit <f:string key="{outputKey}"> from a PBCoreExtension carrying the value of the first "prefix:value"
       extension in the set,
       or nothing if none is present. Given the whole extension set (existential match), so callers pass
       $pbcExtensions directly - the mirror of my:extensionBooleanField for value-bearing fields. Preserves
       the old substring-after semantics: an empty value (e.g. "serie_id:") still produces an empty field. -->
  <xsl:function name="my:extensionStringField" visibility="public">
    <xsl:param name="extensions" as="xs:string*"/>
    <xsl:param name="prefix" as="xs:string"/>
    <xsl:param name="outputKey" as="xs:string"/>
    <xsl:variable name="marker" select="$prefix || ':'"/>
    <xsl:variable name="matching" select="$extensions[f:starts-with(., $marker)]"/>
    <xsl:if test="exists($matching)">
      <f:string key="{$outputKey}">
        <xsl:value-of select="f:substring-after($matching[1], $marker)"/>
      </f:string>
    </xsl:if>
  </xsl:function>


  <!-- Template for access metadata.-->
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
          <xsl:value-of select="true()"/>
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
              <xsl:if test="f:string-length(normalize-space(overlapLength)) > 0
                            and string(number(normalize-space(overlapLength))) != 'NaN'">
                <f:number key="overlap_length">
                  <xsl:value-of select="number(normalize-space(overlapLength))"/>
                </f:number>
              </xsl:if>
              <f:string key="overlap_type">
                <xsl:value-of select="overlapType"/>
              </f:string>
            </f:map>
          </xsl:for-each>
        </f:array>
      </xsl:when>
      <xsl:otherwise>
        <f:string key="kb:program_structure_overlaps">
          <xsl:value-of select="false()"/>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Template to strip namespace from elements.
        This is needed as DOMS records are defining namespace prefixes for each and every child,
        while preservica records only create them for parent records. -->
  <xsl:template match="*" mode="strip-ns">
  <xsl:element name="{local-name()}">
    <xsl:apply-templates select="@*|node()" mode="strip-ns"/>
  </xsl:element>
  </xsl:template>

  <!-- Template to strip namespace from attributes.
        This is needed as DOMS records are defining namespace prefixes for each and every child,
        while preservica records only create them for parent records.-->
  <xsl:template match="@*" mode="strip-ns">
  <xsl:attribute name="{local-name()}">
    <xsl:value-of select="."/>
  </xsl:attribute>
  </xsl:template>

</xsl:transform>
