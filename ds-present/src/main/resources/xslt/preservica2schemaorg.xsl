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
  <!-- This entry stylesheet holds the injected params/globals, the match="/" dispatch by object type,
       the media-type orchestration templates (video/audio/generic-transformation) and the small shared
       primitives. The bulk of the mapping lives in the included modules below, grouped by source schema;
       xsl:include is a compile-time textual merge, so all of it shares this file's global scope. -->
  <xsl:include href="xslt/utils.xsl"/>                              <!-- shared my:* helper functions -->
  <xsl:include href="xslt/preservica/pbcore-descriptive.xsl"/>      <!-- pbc-metadata: PBCore -> schema.org core -->
  <xsl:include href="xslt/preservica/pbcore-internal.xsl"/>         <!-- kb:internal fields + extension helper functions -->
  <xsl:include href="xslt/preservica/access.xsl"/>                  <!-- radiotv_access + DOMS program-structure -->

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
        pbcExtensions: All PBCore Extensions, for retrieval of specific extensions during the transformation.

        This is an INTENTIONAL extension point. AudioObjects are dispatched here (rather than straight to
        generic-transformation) so audio-specific mapping can be added in one place without touching the
        generic path shared with MediaObject. There is no audio-specific logic yet, so it currently just
        delegates to generic-transformation - add audio-only fields here when they arise. -->
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


              <!-- Extract contributor if any present in metadata. see https://schema.org/contributor and the JSON.LD example -->
              <xsl:if test="$pbCore/pbcoreContributor/contributorRole = 'medvirkende' and ./pbcoreContributor/contributor != ''">
                <f:array key="contributor">
                  <xsl:for-each select="./pbcoreContributor">
                    <xsl:if test="./contributorRole = 'medvirkende' and ./contributor != ''">
                      <f:map>
                        <f:string key="@type">Person</f:string>
                        <f:string key="name">
                          <xsl:value-of select="normalize-space(./contributor)"/>
                        </f:string>
                      </f:map>
                    </xsl:if>
                  </xsl:for-each>
                </f:array>
              </xsl:if>
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
