<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:f="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:my="urn:my"
                version="3.0">

  <!-- Empty map used as return variable for functions-->
  <xsl:param name="emptyMap" as="map(*)">
    <xsl:map>
      <xsl:map-entry key="'empty'" select="''"/>
    </xsl:map>
  </xsl:param>

  <!-- FUNCTIONS -->
  <!-- Get milliseconds between two datetimes. -->
  <xsl:function name="my:toMilliseconds" as="xs:integer">
    <xsl:param name="startDate" as="xs:dateTime"/>
    <xsl:param name="endDate" as="xs:dateTime"/>
    <xsl:value-of select="($endDate - $startDate) div xs:dayTimeDuration('PT0.001S')"/>
  </xsl:function>

  <!-- Get the objects from inside a nested array flattened. If the array doesn't exist an empty string will be returned. -->
  <xsl:function name="my:getArrayFromNestedMap" as="item()*">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="array"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:sequence select="$emptyMap"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:sequence select="$emptyMap"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $array))"><xsl:sequence select="$emptyMap"/></xsl:when>
      <xsl:otherwise><xsl:copy-of select="array:flatten(map:get(map:get($object, $map1), $array))"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Get a value from a nested JSON map. The function checks that each level of the map isn't empty.
       If the map is empty, an empty string will be returned.-->
  <xsl:function name="my:getNestedMapValue2Levels">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="map2"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $map2))"><xsl:value-of select="''"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="map:get(map:get($object, $map1),$map2)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Return the innermost value from a three-level nested JSON object.
       The function checks that each level of the map isn't empty.
       If the map is empty at any level, an empty string will be returned. -->
  <xsl:function name="my:getNestedMapValue3Levels">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="map2"/>
    <xsl:param name="map3"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $map2))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get($object, $map1), $map2), $map3))"><xsl:value-of select="''"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="map:get(map:get(map:get($object, $map1),$map2), $map3)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Get a value from a nested JSON map with four nested levels.
       The function checks that each level of the map isn't empty.
       If the map is empty at any level, an empty string will be returned. -->
  <xsl:function name="my:getNestedMapValue4Levels" as="item()">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="map2"/>
    <xsl:param name="map3"/>
    <xsl:param name="map4"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $map2))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get($object, $map1), $map2), $map3))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get($object, $map1), $map2), $map3), $map4))"><xsl:value-of select="''"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="map:get(map:get(map:get(map:get($object, $map1),$map2), $map3), $map4)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Get a value from a nested JSON map with five nested levels.
       The function checks that each level of the map isn't empty.
       If the map is empty at any level, an empty string will be returned. -->
  <xsl:function name="my:getNestedMapValue5Levels" as="item()">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="map2"/>
    <xsl:param name="map3"/>
    <xsl:param name="map4"/>
    <xsl:param name="map5"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $map2))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get($object, $map1), $map2), $map3))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get($object, $map1), $map2), $map3), $map4))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get(map:get($object, $map1), $map2), $map3), $map4), $map5))"><xsl:value-of select="''"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="map:get(map:get(map:get(map:get(map:get($object, $map1), $map2), $map3), $map4), $map5)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Get a value from a nested JSON map with six nested levels.
       The function checks that each level of the map isn't empty.
       If the map is empty at any level, an empty string will be returned. -->
  <xsl:function name="my:getNestedMapValue6Levels" as="item()">
    <xsl:param name="object"/>
    <xsl:param name="map1"/>
    <xsl:param name="map2"/>
    <xsl:param name="map3"/>
    <xsl:param name="map4"/>
    <xsl:param name="map5"/>
    <xsl:param name="map6"/>
    <xsl:choose>
      <xsl:when test="f:empty($object)"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $map2))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get($object, $map1), $map2), $map3))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get($object, $map1), $map2), $map3), $map4))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get(map:get($object,
                                $map1), $map2), $map3), $map4), $map5))"><xsl:value-of select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get(map:get(map:get(map:get(map:get($object,
                                $map1), $map2), $map3), $map4), $map5), $map6))"><xsl:value-of select="''"/></xsl:when>
      <xsl:otherwise><xsl:value-of select="map:get(map:get(map:get(map:get(map:get(map:get($object,
                                             $map1), $map2), $map3), $map4), $map5), $map6)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Clean generic DR channel code to a  -->
  <xsl:function name="my:cleanDrChannel">
    <xsl:param name="channelCode"/>
    <xsl:choose>
      <!-- TV channels start here -->
      <xsl:when test="$channelCode = 'dr1' or $channelCode = 'dr'">
        <xsl:value-of select="'DR 1'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'dr2'">
        <xsl:value-of select="'DR 2'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'dr3'">
        <xsl:value-of select="'DR 3'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drk'">
        <xsl:value-of select="'DR K'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drhd'">
        <xsl:value-of select="'DR HD'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drram'">
        <xsl:value-of select="'DR RAMASJANG'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drultra'">
        <xsl:value-of select="'DR ULTRA'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drup'">
        <xsl:value-of select="'DR UPDATE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drsydregtv' or $channelCode = 'drtvs'">
        <xsl:value-of select="'TV SYD'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drtv'">
        <xsl:value-of select="'DR TV'"/>
      </xsl:when>
      <!-- Radio channels start here -->
      <xsl:when test="$channelCode = 'drp1'">
        <xsl:value-of select="'DR P1'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp2'">
        <xsl:value-of select="'DR P2'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp3' or $channelCode = 'drmusikradio'">
        <xsl:value-of select="'DR P3'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4o'">
        <xsl:value-of select="'DR P4'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4b' or $channelCode = 'drp4blm'">
        <xsl:value-of select="'DR P4 BORNHOLM'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4f' or $channelCode = 'drp4fyn'">
        <xsl:value-of select="'DR P4 FYN'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4k' or $channelCode = 'drp4k94'">
        <xsl:value-of select="'DR P4 KANAL 94 VEJLE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4miv' or $channelCode = 'drp4mv'">
        <xsl:value-of select="'DR P4 MIDT &amp; VEST'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4n' or $channelCode = 'drp4nj' or $channelCode = 'drp4nor'">
        <xsl:value-of select="'DR P4 NORDJYLLAND'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4oj'">
        <xsl:value-of select="'DR P4 ØSTJYLLAND'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4re'">
        <xsl:value-of select="'DR P4 REGIONALEN NÆSTVED'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp4s'">
        <xsl:value-of select="'DR P4 SYD'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp5' or $channelCode = 'dr5'">
        <xsl:value-of select="'DR P5'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp5000'">
        <xsl:value-of select="'DR P5000'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp6b' or $channelCode = 'dr6'">
        <xsl:value-of select="'DR P6 BEAT'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp7' or $channelCode = 'drp7m'">
        <xsl:value-of select="'DR P7 MIX'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drp8j' or $channelCode = 'dr8'">
        <xsl:value-of select="'DR P8 JAZZ'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drpk'">
        <xsl:value-of select="'DR KLASSISK'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drmamara'">
        <xsl:value-of select="'DR MAMA'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drol'">
        <xsl:value-of select="'DR OLINE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drrar'">
        <xsl:value-of select="'RAMASJANG RADIO'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drboo'">
        <xsl:value-of select="'DR BOOGIE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drkalkortb'">
        <xsl:value-of select="'KALUNDBORG KORTBØLGE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drkallangb'">
        <xsl:value-of select="'KALUNDBORG LANGBØLGE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drkalmellemb'">
        <xsl:value-of select="'KALUNDBORG MELLEMBØLGE'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drkalradio'">
        <xsl:value-of select="'KALUNDBORG RADIO'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drkbhradio'">
        <xsl:value-of select="'KØBENHAVNS RADIO'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drlyngbradio'">
        <xsl:value-of select="'LYNGBY RADIO'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drryvangradio'">
        <xsl:value-of select="'RYVANG RADIO'"/>
      </xsl:when>
      <xsl:when test="$channelCode = 'drsorøradio'">
        <xsl:value-of select="'SORØ RADIO'"/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="f:upper-case($channelCode)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Named template to create a genre string containing the value given as a parameter to the template. -->
  <xsl:template name="my:createGenreString">
    <xsl:param name="genreValue"/>
    <f:string key="genre">
      <xsl:value-of select="$genreValue"/>
    </f:string>
  </xsl:template>

  <!-- Check if a sequence contains values from another sequence. Returns boolean true if sequence A contains a value also present in sequence B. -->
  <xsl:function name="my:sequenceAContainsValueFromSequenceB" as="xs:boolean">
    <xsl:param name="sequenceA"/>
    <xsl:param name="sequenceB"/>

    <!-- Find and display common values -->
    <xsl:variable name="valuesIntersect">
      <xsl:for-each select="$sequenceA">
        <xsl:variable name="item" select="lower-case(.)" />
        <xsl:if test="$sequenceB = $item">
          <xsl:value-of select="true()"/>
        </xsl:if>
        <xsl:if test="$sequenceB != $item">
          <xsl:value-of select="false()"/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="f:contains($valuesIntersect, 'true')">
        <xsl:value-of select="true()"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="false()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:stylesheet>