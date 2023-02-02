<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:m="http://www.loc.gov/mods/v3"
               xmlns:mets="http://www.loc.gov/METS/"
               xmlns:t="http://www.tei-c.org/ns/1.0"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               version="3.0">

    
  <xsl:output method="text" />

  <xsl:param name="sep_string" select="'!'"/>
  
  <xsl:template match="/">
  
    <xsl:variable name="json">           
         <!--This is the mets element with the bibliographic metadata.  -->
        <xsl:for-each select="//mets:dmdSec[@ID='Mods1']//m:mods">
          <xsl:variable name="record-id">
            dummy_identifier
          </xsl:variable>
         
         
         <!-- Start XSLT logic -->
          <xsl:variable name="output_data">
            <f:map>
             <!-- Here can be multiple values -->                   
            <xsl:for-each select="m:recordInfo/m:languageOfCataloging/m:languageTerm[1]">
              <f:string key="cataloging_language">
                <xsl:value-of select="."/>              
              </f:string>
            </xsl:for-each>
            <f:string key="id">
              <xsl:value-of select="m:identifier[@type='uri']"/>
            </f:string>
            <f:string key="identifier_local">
              <xsl:value-of select="m:identifier[@type='local']"/>
            </f:string>            
           </f:map>
          </xsl:variable>
          <!-- End XSLT logic -->

          <xsl:apply-templates select="$output_data/f:map">
            <xsl:with-param name="record_identifier" select="$record-id"/>
          </xsl:apply-templates>

        </xsl:for-each>      
    </xsl:variable>
 
 
    <!-- Define output -->
    <xsl:value-of select="f:xml-to-json($json)"/>
 
 
  </xsl:template>
  
  
  
  <xsl:template match="*|@*">
    <xsl:param name="record_identifier"/>
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()">
        <xsl:with-param name="record_identifier" select="$record_identifier"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  
</xsl:transform>
