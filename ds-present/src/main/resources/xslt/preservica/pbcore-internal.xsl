<?xml version="1.0" encoding="UTF-8" ?>
<!-- Auto-split module of preservica2schemaorg.xsl (DRA-2541). Included by that entry stylesheet;
     compiled as one logical stylesheet via xsl:include, so it shares its global params/variables. -->
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
      <xsl:choose>
        <xsl:when test="$kalturaID != ''">
          <xsl:value-of select="true()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="false()"/>
        </xsl:otherwise>
      </xsl:choose>
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
      <xsl:variable name="maxMissingSeconds">
        <xsl:value-of select="90"/>
      </xsl:variable>

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
                <xsl:when test="holes/hole/holeLength[text() &gt; $maxMissingSeconds]"><xsl:value-of select="false()"/></xsl:when>
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
      <xsl:choose>
        <xsl:when test="/XIP/Metadata[@schemaUri = 'http://id.kb.dk/schemas/dr_archive_supplementary_rights_metadata']">
          <xsl:value-of select="f:true()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="false()"/>
        </xsl:otherwise>
      </xsl:choose>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra tvmeter fragment -->
    <f:boolean key="kb:contains_tvmeter">
      <xsl:choose>
        <xsl:when test="//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_tvmeter_metadata']">
          <xsl:value-of select="f:true()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="false()"/>
        </xsl:otherwise>
      </xsl:choose>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra nielsen fragment -->
    <f:boolean key="kb:contains_nielsen">
      <xsl:choose>
        <xsl:when test="//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_nielsen_metadata']">
          <xsl:value-of select="f:true()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="f:false()"/>
        </xsl:otherwise>
      </xsl:choose>
    </f:boolean>

    <!-- Create a field with a boolean value representing if the record has the extra ritzau fragment -->
    <f:boolean key="kb:contains_ritzau">
      <xsl:choose>
        <xsl:when test="//*[namespace-uri() = 'http://id.kb.dk/schemas/supplementary_ritzau_metadata']">
          <xsl:value-of select="f:true()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="f:false()"/>
        </xsl:otherwise>
      </xsl:choose>
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
    <xsl:choose>
      <xsl:when test="$pbCore/pbcoreInstantiation/formatColors = 'farve'">
        <f:boolean key="kb:color"><xsl:value-of select="true()"/></f:boolean>
      </xsl:when>
      <xsl:otherwise>
        <f:boolean key="kb:color"><xsl:value-of select="false()"/></f:boolean>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- EMIT A BOOLEAN FIELD FROM A 'prefix:affirmative' / 'prefix:nonAffirmative' PBCore extension.
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

  <!-- EXTRACT VALUES FROM PBCORE EXTENSIONS TO KB:INTERNAL MAP. These extensions can contain many different values.
       Some might be relevant in relation to schema.org and can be elevated to the correct structure.-->
  <!-- Emit <f:string key="{outputKey}"> carrying the value of the first "prefix:value" extension in the set,
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


</xsl:transform>
