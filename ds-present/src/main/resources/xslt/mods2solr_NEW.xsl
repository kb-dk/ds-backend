<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:m="http://www.loc.gov/mods/v3"
               xmlns:mets="http://www.loc.gov/METS/"
               xmlns:t="http://www.tei-c.org/ns/1.0"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               xmlns:premis="http://www.loc.gov/premis/v3"
               xmlns:mix="http://www.loc.gov/mix/v20"
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
            <f:string key="genre">
            <xsl:value-of select="m:genre"/>
            </f:string>
            <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre'">
              <f:array key="creator">
                  <xsl:for-each select="m:name">
                    <f:string>
                    <xsl:value-of select="concat(m:namePart[@type='family'],', ',m:namePart[@type='given'])"/>
                    </f:string>
                  </xsl:for-each>
              </f:array>
              <f:array key="creator_family_name">
                <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre'">
                  <xsl:for-each select="m:name">
                    <f:string>
                      <xsl:value-of select="m:namePart[@type='family']"/>
                    </f:string>
                  </xsl:for-each>
                </xsl:if>
              </f:array>
              <f:array key="creator_given_name">
                <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre'">
                  <xsl:for-each select="m:name">
                    <f:string>
                      <xsl:value-of select="m:namePart[@type='given']"/>
                    </f:string>
                  </xsl:for-each>
                </xsl:if>
              </f:array>
            </xsl:if>
            <f:string key="collection">
              <xsl:value-of select="m:relatedItem[@type='host']/m:titleInfo/m:title"/>
            </f:string>
            <f:array key="subject">
              <xsl:for-each select="m:subject">
                <xsl:for-each select="m:topic[@lang]">
                  <f:string>
                    <xsl:value-of select="."/>
                  </f:string>
                </xsl:for-each>
              </xsl:for-each>
            </f:array>
            <f:array key="subject_name">
              <xsl:for-each select="m:subject/m:name">
                  <f:string>
                    <xsl:value-of select="concat(m:namePart[@type='family'],', ', m:namePart[@type='given'])"/>
                  </f:string>
              </xsl:for-each>
            </f:array>
            <f:array key="subject_date_of_birth">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="substring-before(m:namePart[@type='date'], '/')"/>
                </f:string>
              </xsl:for-each>
            </f:array>
            <f:array key="subject_date_of_death">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="substring-after(m:namePart[@type='date'], '/')"/>
                </f:string>
              </xsl:for-each>
            </f:array>
            <f:array key="subject_terms_of_address">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="m:namePart[@type='termsOfAddress']"/>
                </f:string>
              </xsl:for-each>
            </f:array>
            <f:array key="type_of_resource">
              <xsl:for-each select="m:typeOfResource">
                <f:string>
                  <xsl:value-of select="."/>
                </f:string>
              </xsl:for-each>
            </f:array>
            <f:string key="file_size">
              <!-- This is the METS element with image metadata. Path might be optimised -->
              <!-- PATH: "../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object"-->
              <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:size"/>
            </f:string>
            <f:string key="image_height">
              <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight"/>
            </f:string>
            <f:string key="image_width">
              <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth"/>
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
