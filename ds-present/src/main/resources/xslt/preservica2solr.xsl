<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:t="http://www.test.com/"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:pbc="http://www.pbcore.org/PBCore/PBCoreNamespace.html"
               xmlns:xip="http://www.tessella.com/XIP/v4"
               xmlns:padding="http://kuana.kb.dk/types/padding/0/1/#"
               xmlns:access="http://doms.statsbiblioteket.dk/types/access/0/1/#"
               xmlns:pidhandle="http://kuana.kb.dk/types/pidhandle/0/1/#"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               version="3.0">

  <xsl:output method="text" />
  <!-- Declare all externally provided parameters here -->
  <!-- Provided by the Java code that calls the Transformer. (Key,Value) pairs are given in a map -->
  <xsl:param name="streamingserver"/>
  <xsl:param name="recordID"/>
  <xsl:param name="origin"/>
  <xsl:param name="childID"/>

  <xsl:template match="/" >
    <xsl:variable name="solrjson">
      <f:map>

        <f:string key="id">
          <xsl:value-of select="$recordID"/>
        </f:string>
        <f:string key="origin">
          <xsl:value-of select="$origin"/>
        </f:string>
        <!-- TODO: Add extraction for all internal fields. -->
        <!-- Accession ref is != accession number from the collection 'samlingsbilleder' this is to be saved as an
             internal value, which is always prefixed with internal_-->
        <f:string key="internal_accession_ref">
          <xsl:value-of select="/xip:DeliverableUnit/AccessionRef"/>
        </f:string>

        <xsl:for-each select="/xip:DeliverableUnit/Metadata/pbc:PBCoreDescriptionDocument">
          <xsl:call-template name="pbc-metadata"/>
        </xsl:for-each>



        <!-- Manifestations are extracted here. I would like to create a template for this.
             However, this is quite tricky when using the document() function -->
        <xsl:if test="$childID != '' and doc-available($childID)">
          <f:array key="resource_id">
            <xsl:for-each select="document($childID)/xip:Manifestation/ComponentManifestation/FileRef">
              <f:string>
                <xsl:value-of select="document($childID)/xip:Manifestation/ComponentManifestation/FileRef"/>
              </f:string>
            </xsl:for-each>
          </f:array>
          <f:string key="manifestation_type">
            <xsl:value-of select="document($childID)/xip:Manifestation/ComponentManifestation/ComponentType"/>
          </f:string>
        </xsl:if>

        <f:string key="internal_padding_seconds">
          <xsl:value-of select="/xip:DeliverableUnit/Metadata/padding:padding/paddingSeconds"/>
        </f:string>

        <xsl:for-each select="/xip:DeliverableUnit/Metadata/access:access">
          <xsl:call-template name="access-template"/>
        </xsl:for-each>

        <xsl:if test="/xip:DeliverableUnit/Metadata/pidhandle:pidhandle/handle">
          <f:string key="pid">
            <xsl:value-of select="substring-after(/xip:DeliverableUnit/Metadata/pidhandle:pidhandle/handle, 'hdl:')"/>
          </f:string>
        </xsl:if>

      </f:map>
    </xsl:variable>

    <xsl:value-of select="f:xml-to-json($solrjson)"/>
  </xsl:template>

  <!-- TEMPLATE FOR ACCESSING ACCESS METADATA -->
  <xsl:template name="access-template">
    <xsl:if test="individuelt_forbud">
      <f:string key="internal_access_individual_prohibition">
        <xsl:value-of select="individuelt_forbud"/>
      </f:string>
    </xsl:if>
    <xsl:if test="klausuleret">
      <f:string key="internal_access_claused">
        <xsl:value-of select="klausuleret"/>
      </f:string>
    </xsl:if>
    <xsl:if test="defekt">
      <f:string key="internal_access_malfunction">
        <xsl:value-of select="defekt"/>
      </f:string>
    </xsl:if>
    <xsl:if test="kommentarer and kommentarer != ''">
      <f:string key="internal_access_comments">
        <xsl:value-of select="kommentarer"/>
      </f:string>
    </xsl:if>
  </xsl:template>

  <!-- TEMPLATE FOR ACCESSING PBC METADATA. CALLED ABOVE-->
  <xsl:template name="pbc-metadata">
    <f:string key="collection">
      <xsl:value-of select="pbcoreInstantiation/formatLocation"/>
    </f:string>

    <xsl:if test="pbcoreGenre">
      <f:array key="categories">
        <xsl:for-each select="pbcoreGenre/genre">
          <f:string>
            <xsl:value-of select="normalize-space(substring-after(., ':'))"/>
          </f:string>
        </xsl:for-each>
      </f:array>

      <xsl:for-each select="pbcoreGenre/genre">
        <xsl:choose>
          <xsl:when test="f:contains(., 'hovedgenre:')">
            <f:string key="genre">
              <xsl:value-of select="normalize-space(substring-after(., 'hovedgenre:'))"/>
            </f:string>
          </xsl:when>
          <xsl:when test="f:contains(., 'undergenre:')">
            <f:string key="genre_sub">
              <xsl:value-of select="normalize-space(substring-after(., 'undergenre:'))"/>
            </f:string>
          </xsl:when>
        </xsl:choose>
      </xsl:for-each>

    </xsl:if>

    <f:string key="resource_description">
      <xsl:value-of select="pbcoreInstantiation/formatMediaType"/>
    </f:string>


    <xsl:for-each select="pbcoreTitle">
      <xsl:if test="titleType = 'titel' and title != ''">
        <f:string key="title">
          <xsl:value-of select="title"/>
        </f:string>
      </xsl:if>
      <xsl:if test="titleType = 'originaltitel' and title != ''">
        <f:string key="original_title">
          <xsl:value-of select="title"/>
        </f:string>
      </xsl:if>
      <xsl:if test="titleType = 'episodetitel' and title != ''">
        <f:string key="episode_title">
          <xsl:value-of select="title"/>
        </f:string>
      </xsl:if>
    </xsl:for-each>

    <!--TODO: Figure out what the difference is between kanalnavn and channel_name in the metadata. -->
    <xsl:for-each select="pbcorePublisher">
      <xsl:if test="publisherRole = 'kanalnavn'">
        <f:string key="creator_affiliation">
          <xsl:value-of select="publisher"/>
        </f:string>
      </xsl:if>
    </xsl:for-each>

    <!-- When looking at the mods2solr XSLT the field 'notes' is an array of different notes and metadata.
         It works like a "you can define your metadata here if you don't know where else it fits" field.
         The same type of notes array is created here, however I believe that these descriptions deserves
         a more specific field as well, as they are quite well-structured.-->
    <xsl:if test="pbcoreDescription">
      <f:array key="notes">
        <xsl:for-each select="pbcoreDescription">
          <f:string>
            <xsl:value-of select="normalize-space(description)"/>
          </f:string>
        </xsl:for-each>
      </f:array>
    </xsl:if>

    <!-- From the metadata it is clear, that 'kortomtale' and 'langomtale1' can contain completely different values.
         'kortomtale' is therefore not just a shorter form of 'langomtale1'.-->
    <xsl:for-each select="pbcoreDescription">
      <xsl:choose>
        <xsl:when test="descriptionType = 'kortomtale' and description != ''">
          <f:string key="abstract">
            <xsl:value-of select="normalize-space(description)"/>
          </f:string>
        </xsl:when>
        <xsl:when test="descriptionType = 'langomtale1' and description != ''">
          <f:string key="description">
            <xsl:value-of select="normalize-space(description)"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="description != '' ">
      </xsl:if>
    </xsl:for-each>

    <!-- Video specific transformations -->
    <xsl:for-each select="pbcoreIdentifier">
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
      <xsl:when test="pbcoreInstantiation/formatDuration">
        <f:string key="duration_ms">
          <xsl:value-of select="pbcoreInstantiation/formatDuration"/>
        </f:string>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="startTime">
          <xsl:value-of select="pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart"/>
        </xsl:variable>
        <xsl:variable name="endTime">
          <xsl:value-of select="pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd"/>
        </xsl:variable>
        <xsl:variable name="durationInMilliseconds">
          <xsl:value-of select="my:toMilliseconds($startTime, $endTime)"/>
        </xsl:variable>
        <f:string key="duration_ms">
          <xsl:value-of select="$durationInMilliseconds"/>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>


      <xsl:if test="pbcoreInstantiation/formatStandard">
        <f:string key="video_quality">
          <xsl:value-of select="pbcoreInstantiation/formatStandard"/>
        </f:string>
      </xsl:if>



    <xsl:choose>
      <xsl:when test="pbcoreInstantiation/formatColors = 'farve'">
        <f:string key="color">true</f:string>
      </xsl:when>
      <xsl:otherwise>
        <f:string key="color">false</f:string>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:for-each select="pbcoreExtension/extension">
      <xsl:choose>
        <xsl:when test="f:starts-with(., 'premiere:') and f:contains(., 'ikke premiere')">
          <f:string key="premiere">
            <xsl:value-of select="false()"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(., 'premiere:') and f:contains(., ':premiere')">
          <f:string key="premiere">
            <xsl:value-of select="true()"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(., 'live:') and f:contains(., 'ikke live')">
          <f:string key="live_broadcast">
            <xsl:value-of select="false()"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(., 'live:') and f:contains(., ':live')">
          <f:string key="live_broadcast">
            <xsl:value-of select="true()"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(., 'episodenr:') and substring-after(., ':') != ''">
          <f:string key="episode">
            <xsl:value-of select="substring-after(., ':')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(., 'antalepisoder:') and . != 'antalepisoder:0' and substring-after(., ':') != ''">
          <f:string key="number_of_episodes">
            <xsl:value-of select="substring-after(., ':')"/>
          </f:string>
        </xsl:when>
        <xsl:when test=". = 'genudsendelse:ikke genudsendelse'">
          <f:string key="retransmission">
            <xsl:value-of select="false()"/>
          </f:string>
        </xsl:when>
        <xsl:when test=". = 'genudsendelse:genudsendelse'">
          <f:string key="retransmission">
            <xsl:value-of select="true()"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'hovedgenre_id:')">
          <f:string key="internal_maingenre_id">
            <xsl:value-of select="f:substring-after(. , 'hovedgenre_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'kanalid:')">
          <f:string key="internal_channel_id">
            <xsl:value-of select="f:substring-after(. , 'kanalid:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'produktionsland_id:')">
          <f:string key="internal_country_of_origin_id">
            <xsl:value-of select="f:substring-after(. , 'produktionsland_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'program_id:')">
          <f:string key="internal_ritzau_program_id">
            <xsl:value-of select="f:substring-after(. , 'program_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'undergenre_id:')">
          <f:string key="internal_subgenre_id">
            <xsl:value-of select="f:substring-after(. , 'undergenre_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'afsnit_id:')">
          <f:string key="internal_episode_id">
            <xsl:value-of select="f:substring-after(. , 'afsnit_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'saeson_id:')">
          <f:string key="internal_season_id">
            <xsl:value-of select="f:substring-after(. , 'saeson_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'serie_id:')">
          <f:string key="internal_series_id">
            <xsl:value-of select="f:substring-after(. , 'serie_id:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'showviewcode:')">
          <f:string key="internal_showviewcode">
            <xsl:value-of select="f:substring-after(. , 'showviewcode:')"/>
          </f:string>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'program_ophold:')">
          <!-- inner XSLT Choose which determines if program_ophold is false or true -->
          <xsl:choose>
            <xsl:when test=". = 'program_ophold:ikke program ophold'">
              <f:string key="internal_program_ophold">
                <xsl:value-of select="false()"/>
              </f:string>
            </xsl:when>
            <xsl:when test=". = 'program_ophold:program ophold'">
              <f:string key="internal_program_ophold">
                <xsl:value-of select="true()"/>
              </f:string>
            </xsl:when>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'tekstet:')">
          <!-- Inner XSLT  choose to determine value of boolean -->
          <xsl:choose>
            <xsl:when test=". = 'tekstet:ikke tekstet'">
              <f:string key="has_subtitles">
                <xsl:value-of select="false()"/>
              </f:string>
            </xsl:when>
            <xsl:when test=". = 'tekstet:tekstet'">
              <f:string key="has_subtitles">
                <xsl:value-of select="true()"/>
              </f:string>
            </xsl:when>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'th:')">
          <!-- Inner XSLT  choose to determine value of boolean -->
          <xsl:choose>
            <xsl:when test=". = 'th:ikke tekstet for hørehæmmede'">
              <f:string key="has_subtitles_for_hearing_impaired">
                <xsl:value-of select="false()"/>
              </f:string>
            </xsl:when>
            <xsl:when test=". = 'th:tekstet for hørehæmmede'">
              <f:string key="has_subtitles_for_hearing_impaired">
                <xsl:value-of select="true()"/>
              </f:string>
            </xsl:when>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="f:starts-with(. , 'ttv:')">
          <!-- Inner XSLT  choose to determine value of boolean -->
          <xsl:choose>
            <xsl:when test=". = 'ttv:ikke tekst-tv'">
              <f:string key="internal_is_teletext">
                <xsl:value-of select="false()"/>
              </f:string>
            </xsl:when>
            <xsl:when test=". = 'ttv:tekst-tv'">
              <f:string key="internal_is_teletext">
                <xsl:value-of select="true()"/>
              </f:string>
            </xsl:when>
          </xsl:choose>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>

    <xsl:if test="pbcoreInstantiation/formatAspectRatio">
      <f:string key="aspect_ratio">
        <xsl:value-of select="pbcoreInstantiation/formatAspectRatio"/>
      </f:string>
    </xsl:if>

    <xsl:choose>
      <xsl:when test="pbcoreInstantiation/formatChannelConfiguration = 'surround'">
        <f:string key="surround_sound"><xsl:value-of select="true()"/></f:string>
      </xsl:when>
      <xsl:when test="pbcoreInstantiation/formatChannelConfiguration = 'ikke surround'">
        <f:string key="surround_sound"><xsl:value-of select="false()"/></f:string>
      </xsl:when>
    </xsl:choose>

    <xsl:for-each select="pbcoreInstantiation/pbcoreFormatID">
      <xsl:choose>
        <xsl:when test="formatIdentifierSource = 'ritzau'">
          <f:string key="internal_format_identifier_ritzau">
            <xsl:value-of select="formatIdentifier"/>
          </f:string>
        </xsl:when>
        <xsl:when test="formatIdentifierSource = 'nielsen'">
          <f:string key="internal_format_identifier_nielsen">
            <xsl:value-of select="formatIdentifier"/>
          </f:string>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>


  </xsl:template>

  <!-- FUNCTIONS -->
  <!-- Get milliseconds between two dates. -->
  <xsl:function name="my:toMilliseconds" as="xs:integer">
    <xsl:param name="startDate" as="xs:dateTime"/>
    <xsl:param name="endDate" as="xs:dateTime"/>
    <xsl:value-of select="($endDate - $startDate) div xs:dayTimeDuration('PT0.001S')"/>
  </xsl:function>
</xsl:transform>