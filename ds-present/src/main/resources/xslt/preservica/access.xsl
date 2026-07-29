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


</xsl:transform>
