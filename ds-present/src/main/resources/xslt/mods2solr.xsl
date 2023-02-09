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

  <!-- Declare all externally provided parameters here -->
  <!-- Provided by the Java code that calls the Transformer. (Key,Value) pairs are given in a map -->            
       <xsl:param name="access_blokeret"/>        
       <xsl:param name="access_pligtafleveret"/>
       <xsl:param name="access_ejermaerke"/>
       <xsl:param name="access_note"/>
       <xsl:param name="access_skabelsesaar"/>
       <xsl:param name="access_ophavsperson_doedsaar"/>
       <xsl:param name="access_searlige_visningsvilkaar"/>
       <xsl:param name="access_materiale_type"/>
       <xsl:param name="imageurl"/>
              
  <xsl:template match="/">
  
    <xsl:variable name="json">   
    <f:map>        
    
     <!-- Use the externally provided parameter. Only set if they are in input-->

     <!-- Some fields always here. Required in schema -->
     <f:string key="access_blokeret"><xsl:value-of select="$access_blokeret"/></f:string>
     <f:string key="access_skabelsesaar"><xsl:value-of select="$access_skabelsesaar"/></f:string>
     <f:string key="access_materiale_type"><xsl:value-of select="$access_materiale_type"/></f:string>



     <!-- Only add fields if not empty -->
     <xsl:if test="$access_ophavsperson_doedsaar!= ''">
        <f:string key="access_ophavsperson_doedsaar"><xsl:value-of select="$access_ophavsperson_doedsaar"/></f:string>
     </xsl:if>

     <xsl:if test="$access_pligtafleveret!= ''">
       <f:string key="access_pligtafleveret"><xsl:value-of select="$access_pligtafleveret"/></f:string>
     </xsl:if>

     <xsl:if test="$access_ejermaerke!= ''">
       <f:string key="access_ejermaerke"><xsl:value-of select="$access_ejermaerke"/></f:string>
     </xsl:if>

      <xsl:if test="$access_note!= ''">
        <f:string key="access_note"><xsl:value-of select="$access_note"/></f:string>
      </xsl:if>

      <xsl:if test="$access_searlige_visningsvilkaar!= ''">
        <f:string key="access_searlige_visningsvilkaar"><xsl:value-of select="$access_searlige_visningsvilkaar"/></f:string>
      </xsl:if>

      <xsl:if test="$imageurl!= ''">
        <f:string key="imageurl"><xsl:value-of select="$imageurl"/></f:string>
      </xsl:if>
      <!-- End externally provided parameters -->


      <!--This is the mets element with the bibliographic metadata.  -->
      <xsl:for-each select="//mets:dmdSec[@ID='Mods1']//m:mods">

       <!-- Start XSLT logic -->
           <!-- Here can be multiple values -->
          <xsl:for-each select="m:recordInfo/m:languageOfCataloging/m:languageTerm[1]">
            <f:string key="cataloging_language">
              <xsl:value-of select="."/>
            </f:string>
          </xsl:for-each>
          <xsl:if test="m:location/m:shelfLocator">
            <f:string key="shelf_location">
              <xsl:value-of select="m:location/m:shelfLocator"/>
            </f:string>
          </xsl:if>
          <f:string key="id">
            <xsl:value-of select="m:identifier[@type='uri']"/>
          </f:string>
          <f:string key="identifier_local">
            <xsl:value-of select="m:identifier[@type='local']"/>
          </f:string>
          <!-- Categories seems to be a collection of other fields. -->
          <f:string key="categories">
          <xsl:value-of select="m:genre[@type='Categories']"/>
          </f:string>
          <!-- Different things can be represented in note. -->
          <xsl:if test="m:note[@displayLabel='Catalog Name']">
            <f:string key="catalog_name">
              <xsl:value-of select="m:note[@displayLabel='Catalog Name']"/>
            </f:string>
          </xsl:if>
          <f:string key="collection">
            <xsl:value-of select="m:relatedItem[@type='host']/m:titleInfo/m:title"/>
          </f:string>
          <xsl:if test="m:note[@type='content']">
            <f:array key="content">
              <xsl:for-each select="m:note[@type='content']">
                <xsl:if test=". != ''">
                  <f:string>
                    <xsl:value-of select="."/>
                  </f:string>
                </xsl:if>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:if test="m:note[@displayLabel='Description']">
            <f:string key="description">
              <xsl:value-of select="m:note[@displayLabel='Description']"/>
            </f:string>
          </xsl:if>
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre'">
            <f:array key="creator_name">
                <xsl:for-each select="m:name">
                  <f:string>
                  <xsl:value-of select="concat(m:namePart[@type='family'],', ',m:namePart[@type='given'])"/>
                  </f:string>
                </xsl:for-each>
            </f:array>
            <f:array key="creator_full_name">
              <xsl:for-each select="m:name">
                <f:string>
                  <xsl:value-of select="concat(m:namePart[@type='given'], ' ',m:namePart[@type='family'])"/>
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
            <f:array key="creator_terms_of_address">
              <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre'">
                <xsl:for-each select="m:name">
                  <f:string>
                    <xsl:value-of select="m:namePart[@type='termsOfAddress']"/>
                  </f:string>
                </xsl:for-each>
              </xsl:if>
            </f:array>
          </xsl:if>
          <xsl:if test="m:originInfo[@altRepGroup='original']/m:dateCreated">
            <xsl:choose>
              <xsl:when test="m:originInfo[@altRepGroup='original']/m:dateCreated[@point='start']">
                <f:string key="production_date_start">
                  <xsl:value-of select="m:originInfo[@altRepGroup='original']/m:dateCreated[@point='start']"/>
                </f:string>
                <f:string key="production_date_end">
                  <xsl:value-of select="m:originInfo[@altRepGroup='original']/m:dateCreated[@point='end']"/>
                </f:string>
              </xsl:when>
              <xsl:otherwise>
                <f:string key="date_created">
                  <xsl:value-of select="m:originInfo[@altRepGroup='original']/m:dateCreated"/>
                </f:string>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="m:subject/m:topic[@lang]">
            <f:array key="topic">
              <!-- TODO: Skip empty elements -->
              <xsl:for-each select="m:subject">
                <xsl:for-each select="m:topic[@lang]">
                  <xsl:if test=". != ''">
                    <f:string>
                      <xsl:value-of select="."/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:if test="m:subject/m:hierarchicalGeographic">
            <xsl:for-each select="m:subject/m:hierarchicalGeographic">
                <f:string key="area">
                  <xsl:value-of select="m:area"/>
                </f:string>
            </xsl:for-each>
          </xsl:if>
          <xsl:if test="m:subject/m:name">
            <f:array key="subject_name">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="concat(m:namePart[@type='family'],', ', m:namePart[@type='given'])"/>
                </f:string>
              </xsl:for-each>
              </f:array>
              <f:array key="subject_full_name">
                <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="concat(m:namePart[@type='given'],' ',m:namePart[@type='family'])"/>
                </f:string>
                </xsl:for-each>
              </f:array>
              <f:array key="subject_family_name">
                <xsl:for-each select="m:subject/m:name">
                <f:string>
                    <xsl:value-of select="m:namePart[@type='family']"/>
                </f:string>
                </xsl:for-each>
              </f:array>
              <f:array key="subject_given_name">
                <xsl:for-each select="m:subject/m:name">
                <f:string>
                    <xsl:value-of select="m:namePart[@type='given']"/>
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
          </xsl:if>
          <f:array key="type_of_resource">
            <xsl:for-each select="m:typeOfResource">
              <f:string>
                <xsl:value-of select="."/>
              </f:string>
            </xsl:for-each>
          </f:array>
          <f:string key="file_size">
             <!--- This is the METS element with image metadata. Path might be optimised -->
             <!--- PATH: "../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object"-->
            <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:size"/>
          </f:string>
          <f:string key="image_height">
            <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight"/>
          </f:string>
          <f:string key="image_width">
            <xsl:value-of select="../../../../mets:amdSec/mets:techMD/mets:mdWrap/mets:xmlData/premis:object/premis:objectCharacteristics/premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth"/>
          </f:string>
         <!--- End XSLT logic -->
      </xsl:for-each>
   </f:map>
   </xsl:variable>
 
    <!-- Define output -->
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>
  
</xsl:transform>
