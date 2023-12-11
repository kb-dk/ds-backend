<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:f="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:my="urn:my"
                version="3.0">

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
      <xsl:when test="f:empty($object)"><xsl:sequence select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get($object, $map1))"><xsl:sequence select="''"/></xsl:when>
      <xsl:when test="f:empty(map:get(map:get($object, $map1), $array))"><xsl:sequence select="''"/></xsl:when>
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
      <xsl:otherwise> <xsl:value-of select="map:get(map:get($object, $map1),$map2)"/></xsl:otherwise>
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

</xsl:stylesheet>