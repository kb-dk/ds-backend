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
              <xsl:choose>
                <xsl:when test="f:starts-with($publisherSpecific, '_')">
                  <xsl:value-of select="substring($publisherSpecific, 2)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$publisherSpecific"/>
                </xsl:otherwise>
              </xsl:choose>
            </f:string>
            <xsl:if test="(f:exists($publisherGeneral) and not(f:empty($publisherGeneral)) and $publisherGeneral != '') or
                          ($publisherSpecific = 'DR1' or $publisherSpecific = 'DR2')">
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

                <f:map key="partOfSeason">
                  <f:string key="@type">
                    <xsl:choose>
                      <xsl:when test="$type = 'VideoObject'">TVSeason</xsl:when>
                      <xsl:when test="$type = 'AudioObject'">RadioSeason</xsl:when>
                      <xsl:otherwise>CreativeWorkSeason</xsl:otherwise>
                    </xsl:choose>
                  </f:string>
                  <xsl:variable name="numberOfEpisodes">
                    <xsl:value-of select="number(normalize-space(substring-after($episodeInfo, ':')))"/>
                  </xsl:variable>
                  <xsl:if test="string($numberOfEpisodes) != 'NaN'">
                    <f:number key="numberOfEpisodes">
                      <xsl:value-of select="substring-after($episodeInfo, ':')"/>
                    </f:number>
                  </xsl:if>
                </f:map>
            </xsl:when>
            <xsl:otherwise>
              <!-- Extract metadata from PBC extensions related to episodes -->
              <xsl:for-each select=".">
                <!-- Extract episode number if present.
                     Checks for 'episodenr' in PBC extension and checks that there is a substring after the key.-->
                <xsl:if test="f:contains(., 'episodenr:') and f:string-length(normalize-space(substring-after(., 'episodenr:'))) > 0">
                  <xsl:variable name="episodeNumber">
                      <xsl:value-of select="normalize-space(substring-after(., 'episodenr:'))"/>
                  </xsl:variable>
                  <!-- Check that variable only contains valid digits and not crazy stuff like 2+3 to show that both episode 2 and 3 are present in this program. -->
                  <xsl:if test="string($episodeNumber) != 'NaN' and matches($episodeNumber, '^\d+$')">
                    <f:number key="episodeNumber">
                      <xsl:value-of select="substring-after(., 'episodenr:')"/>
                    </f:number>
                  </xsl:if>
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
                    <xsl:variable name="numberOfEpisodes">
                      <xsl:value-of select="number(normalize-space(substring-after(., 'antalepisoder:')))"/>
                    </xsl:variable>
                    <xsl:if test="string($numberOfEpisodes) != 'NaN'">
                      <f:number key="numberOfEpisodes">
                        <xsl:value-of select="substring-after(., 'antalepisoder:')"/>
                      </f:number>
                    </xsl:if>
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
                      select="('sport', 'blandet sport - nyhedspræget uden', 'fodbold', 'hestesport (gallop trav ridebane'), 'atletik', 'badminton.', 'boksning.', 'blandet sport - nyhedspræget med',
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
        <!-- These values should map to: Diverse-->
        <xsl:variable name="Misc" as="item()*"
                      select="('alle', 'andet', 'andet.', 'blandet', 'ikke formålsfordelt', 'N/A', 'n/a', 'præsentation og services', 'øvrige programsatte udsendelser')"/>

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
            <xsl:variable name="genreValue">
              <xsl:choose>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $NewsPoliticsSociety)">
                  <xsl:value-of select="'Nyheder, politik og samfund'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Music)">
                  <xsl:value-of select="'Musik'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Culture)">
                  <xsl:value-of select="'Kultur og oplysning'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Sport)">
                  <xsl:value-of select="'Sport'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Entertainment)">
                  <xsl:value-of select="'Humor, quiz og underholdning'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $ChildrenYouth)">
                  <xsl:value-of select="'Børn og unge'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Documentary)">
                  <xsl:value-of select="'Dokumentar'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Fiction)">
                  <xsl:value-of select="'Film og serier'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Lifestyle)">
                  <xsl:value-of select="'Livsstil'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $ScienceNature)">
                  <xsl:value-of select="'Natur og videnskab'"/>
                </xsl:when>
                <xsl:when test="my:sequenceAContainsValueFromSequenceB($keywordsSequence, $Misc)">
                  <xsl:choose>
                    <xsl:when test="$type = 'VideoObject'">
                      <xsl:value-of select="'TV-rodekasse'"/>
                    </xsl:when>
                    <xsl:when test="$type = 'AudioObject'">
                      <xsl:value-of select="'Radio-rodekasse'"/>
                    </xsl:when>
                  </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:choose>
                    <xsl:when test="$type = 'VideoObject'">
                      <xsl:value-of select="'TV-rodekasse'"/>
                    </xsl:when>
                    <xsl:when test="$type = 'AudioObject'">
                      <xsl:value-of select="'Radio-rodekasse'"/>
                    </xsl:when>
                  </xsl:choose>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <f:string key="genre">
              <xsl:value-of select="$genreValue"/>
            </f:string>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
      <!-- Adding a fallback to 'Rodekassen' as we have 160K records without genre at all. -->
      <xsl:otherwise>
        <f:string key="genre">
          <xsl:choose>
            <xsl:when test="$type = 'VideoObject'">
              <xsl:value-of select="'TV-rodekasse'"/>
            </xsl:when>
            <xsl:when test="$type = 'AudioObject'">
              <xsl:value-of select="'Radio-rodekasse'"/>
            </xsl:when>
          </xsl:choose>
        </f:string>
      </xsl:otherwise>
    </xsl:choose>

    <!-- Extract directors if any present in metadata. see https://schema.org/director -->
    <!-- In our devel system we dont have any records where there are more than one contributer with the role 'instruktion' therefore this is not implemented as an array. -->
    <xsl:if test="./pbcoreContributor/contributorRole = 'instruktion' and ./pbcoreContributor/contributor != ''">
      <f:array key="director">
        <xsl:for-each select="./pbcoreContributor">
          <xsl:if test="./contributorRole = 'instruktion' and ./contributor != ''">
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

    <!-- Extract authors/creators here we are using creators as these two can be used for the same content and we are using creator for images as well. -->
    <xsl:if test="./pbcoreCreator/creatorRole = 'forfatter' and ./pbcoreCreator/creator != ''">
      <f:array key="creator">
        <xsl:for-each select="./pbcoreCreator">
          <xsl:if test="./creatorRole = 'forfatter' and ./creator != ''">
            <f:map>
              <f:string key="@type">Person</f:string>
              <f:string key="name">
                <xsl:value-of select="normalize-space(./creator)"/>
              </f:string>
            </f:map>
          </xsl:if>
        </xsl:for-each>
      </f:array>
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



</xsl:transform>
