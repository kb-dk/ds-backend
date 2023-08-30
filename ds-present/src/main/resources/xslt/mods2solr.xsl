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
               xmlns:functx="http://www.functx.com"
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
       <xsl:param name="access_foto_aftale"/>
       <xsl:param name="access_billede_aftale"/>
       <xsl:param name="access_ophavsret_tekst"/>
       <xsl:param name="imageserver"/>
       <xsl:param name="old_imageserver"/>
       <xsl:param name="recordID"/> <!-- Guaranteed to be set -->


  <xsl:template match="/">
  
    <xsl:variable name="json">   
    <f:map>        
    
     <!-- Use the externally provided parameter. Only set if they are in input-->

     <!-- Some fields always here. Required in schema -->
     <f:string key="access_blokeret"><xsl:value-of select="$access_blokeret"/></f:string>        
     <f:string key="access_materiale_type"><xsl:value-of select="$access_materiale_type"/></f:string>
     <f:string key="access_foto_aftale"><xsl:value-of select="$access_foto_aftale"/></f:string>
     <f:string key="access_billede_aftale"><xsl:value-of select="$access_billede_aftale"/></f:string>
     <f:string key="access_ophavsret_tekst"><xsl:value-of select="$access_ophavsret_tekst"/></f:string>


     <!-- Only add fields if not empty -->
     <xsl:if test="$access_skabelsesaar!= ''">
      <f:string key="access_skabelsesaar"><xsl:value-of select="$access_skabelsesaar"/></f:string>    
     </xsl:if>
     
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
   
      <!-- End externally provided parameters -->


      <!--This is the mets element with the bibliographic metadata.  -->
      <xsl:for-each select="//mets:amdSec/mets:techMD[@ID='PremisObject1']//premis:objectCharacteristics">
        <xsl:if test="premis:size">
          <f:string key="file_byte_size">
            <xsl:value-of select="xs:long(premis:size)"/>
          </f:string>
        </xsl:if>
        <!-- Extracts the height of the digital image-->
        <xsl:if test="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight">
        <f:string key="image_height">
          <xsl:value-of select="xs:long(premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight)"/>
        </f:string>
        </xsl:if>
        <!-- Extracts the width of the digital image-->
        <xsl:if test="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth">
        <f:string key="image_width">
          <xsl:value-of select="xs:long(premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth)"/>
        </f:string>
        </xsl:if>
        <!-- Calculates the size of the digital image-->
        <xsl:if test="xs:long(premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight * premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth)">
        <f:string key="image_size_pixels">
          <xsl:value-of select="xs:long(premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight * premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth)"/>
        </f:string>
        </xsl:if>
      </xsl:for-each>
      <!--- This is the METS element with image metadata from the PremisObject
       This extraction is used to provide information on image size-->
      <xsl:for-each select="//mets:dmdSec[@ID='Mods1']//m:mods">
        <!-- Start XSLT logic -->
        <!-- Here can be multiple values -->
        <!-- Extracting cataloging language if it exists-->
        <xsl:if test="m:recordInfo/m:languageOfCataloging/m:languageTerm">
          <xsl:for-each select="m:recordInfo/m:languageOfCataloging/m:languageTerm[1]">
            <f:string key="cataloging_language">
              <xsl:value-of select="."/>
            </f:string>
          </xsl:for-each>
          <!-- Extracting physical location if it exists-->
          <!-- Physical location is part of MODS, but is not present in any of our test files. -->
        </xsl:if>
        <xsl:if test="m:location/m:physicalLocation">
          <f:string key="physical_location">
            <xsl:value-of select="m:location/m:physicalLocation"/>
          </f:string>
        </xsl:if>
        <!-- Extracting shelf location if it exists. In our test data it seems that metadata which might benefit from
             being split between physicalLocation and shelfLocator has been combined into the shelfLocator.-->
        <xsl:if test="m:location/m:shelfLocator">
          <f:string key="location">
            <xsl:value-of select="m:location/m:shelfLocator"/>
          </f:string>
        </xsl:if>
        <!-- Extracts id and strips if for urn:uuid:-->
        <!-- TODO: Should 'urn:uuid' be included? -->
        <f:string key="id">
            <!-- TODO: We should store the original ID in some field, e.g. origin_id or source_id -->
          <!-- <xsl:value-of select="substring-after(m:identifier[@type='uri'],'urn:uuid:')"/>-->
          <xsl:value-of select="$recordID"/>
        </f:string>
        <!-- Extracts local identifier,
             Which in other terms is a local filename. -->
        <f:string key="filename_local">
          <xsl:value-of select="m:identifier[@type='local']"/>
        </f:string>
        <xsl:if test="m:identifier[@type='accession number']">
          <f:string key="accession_number">
            <xsl:value-of select="m:identifier[@type='accession number']"/>
          </f:string>
        </xsl:if>
        <!-- Categories seems to be a collection of multiple categories seperated by commas. -->
        <!-- According to the MODS standard genre should contain info more specific than typeOfResource. In our case, this is not the case. -->
        <xsl:if test="m:genre[@type='Categories']">
          <f:string key="categories">
            <xsl:value-of select="m:genre[@type='Categories']"/>
          </f:string>
          <!-- Creates an array of categories split on commas.
                Regex removes dates from categories-->
          <f:array key="list_of_categories">
            <xsl:for-each select="distinct-values(tokenize(m:genre[@type='Categories'], ','))">
              <xsl:if test=". != '' and not(matches(. , '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}'))">
                <f:string>
                  <xsl:value-of select="normalize-space(.)"/>
                </f:string>
              </xsl:if>
            </xsl:for-each>
          </f:array>
          <xsl:variable name="categories_count">
            <xsl:for-each select="distinct-values(tokenize(m:genre[@type='Categories'], ','))">
              <xsl:if test=". != '' and not(matches(. , '\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}'))">
                <xsl:text>a</xsl:text>
              </xsl:if>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="categories_count">
            <xsl:value-of select="f:string-length($categories_count)"/>
          </f:string>
        </xsl:if>
        <!-- Different things can be represented in note. -->
        <!-- If catalog name exists, extract it-->
        <xsl:if test="m:note[@displayLabel='Catalog Name']">
          <f:string key="catalog">
            <xsl:value-of select="m:note[@displayLabel='Catalog Name']"/>
          </f:string>
        </xsl:if>
        <!-- If host collection exist, extract it-->
        <xsl:if test="m:relatedItem[@type='host' and @displayLabel='Samling']">
          <f:string key="collection">
            <xsl:value-of select="normalize-space(m:relatedItem[@type='host' and @displayLabel='Samling'])"/>
          </f:string>
        </xsl:if>
        <xsl:if test="m:relatedItem[@type='host' and @displayLabel='Publication']">
          <f:string key="published_in">
            <xsl:value-of select="normalize-space(m:relatedItem[@type='host' and @displayLabel='Publication'])"/>
          </f:string>
        </xsl:if>
        <!-- if note field of type content exists extract it-->
        <xsl:if test="m:note[@type='content'] or m:note[@displayLabel='Description']">
          <f:array key="notes">
            <xsl:if test="m:note[@type='content']">
            <xsl:for-each select="m:note[@type='content']">
              <xsl:if test=". != ''">
                <f:string>
                  <!-- First use of replace 'zh|'.
                       This check is done multiple times to remove 'zh|' in front of chinese characters -->
                  <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                </f:string>
              </xsl:if>
            </xsl:for-each>
            </xsl:if>
            <!-- Ideally this transformation has a check, that the content of m:note[@displayLabel='Description']
                 isn't present in a string or substring of m:note[@type='content'] as it seems like they could de duplicated there. -->
            <xsl:for-each select="m:note[@displayLabel='Description']">
              <xsl:analyze-string select="." regex="(.*\.)([a-zA-Z].*)">
                <xsl:matching-substring>
                  <f:string>
                    <xsl:value-of select="f:concat(regex-group(1), ' ', f:regex-group(2))"/>
                  </f:string>
                </xsl:matching-substring>
                <xsl:non-matching-substring>
                  <f:string>
                    <xsl:value-of select="."/>
                  </f:string>
                </xsl:non-matching-substring>
              </xsl:analyze-string>
            </xsl:for-each>
          </f:array>
          <!--Count hack - writing to a variable for each entry,
              then counting length of variable string to get amount -->
          <xsl:variable name="notes_count">
            <xsl:for-each select="m:note[@type='content']">
              <xsl:if test=". != ''">
                <xsl:text>a</xsl:text>
              </xsl:if>
            </xsl:for-each>
            <xsl:for-each select="m:note[@displayLabel='Description']">
              <xsl:text>a</xsl:text>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="notes_count">
            <xsl:value-of select="f:string-length($notes_count)"/>
          </f:string>

          <!-- TODO: This only counts from note:type=content and not from note[@displayLabel='Description'] as well  -->
          <xsl:if test="m:note[@type='content']">
            <f:string key="notes_length">
              <xsl:variable name="noteslength">
                <xsl:for-each select="m:note[@type='content']">
                  <xsl:value-of select="f:concat(f:replace(., 'zh\|', ''), ' ')"/>
                </xsl:for-each>
                <xsl:for-each select="m:note[@displayLabel='Description']">
                  <xsl:value-of select="f:concat(f:replace(., 'zh\|', ''), ' ')"/>
                </xsl:for-each>
              </xsl:variable>
              <xsl:value-of select="f:string-length($noteslength)"/>
            </f:string>
          </xsl:if>
        </xsl:if>
        <!-- if note field of type internal note exist extracts it, but remove empty prefixes for notes-->
        <xsl:if test="m:note[@type='Intern note']">
          <f:array key="internal_note">
            <xsl:for-each select="m:note[@type='Intern note']">
              <xsl:if test=". !='' and . !='Intern note:' and . !='Ekstern note:' and . !='Alt. navn:' and . != 'Kunstnernote:' and . !='Aktiv:'">
                <f:string>
                  <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                </f:string>
              </xsl:if>
            </xsl:for-each>
          </f:array>
        </xsl:if>
        <!--Extracts descriptions from two different fields if at least one of them are present -->
        <!-- Checks all possible variations of physical description from MODS that are not related to page orientation-->
        <xsl:if test="m:physicalDescription/not(m:note[@displayLabel='Pageorientation'])">
          <f:array key="physical_description">
            <xsl:for-each select="m:note[@displayLabel='Description']">
              <f:string>
                <xsl:value-of select="."/>
              </f:string>
            </xsl:for-each>
            <xsl:for-each select="m:physicalDescription">
              <!-- Selects all fields on physical description that are not page orientation-->
              <xsl:for-each select="m:form
                                      | m:reformattingQuality
                                      | m:internetMediaType
                                      | m:extent
                                      | m:digitalOrigin
                                      | m:note[not(@displayLabel='Pageorientation')]">
                <f:string><xsl:value-of select="normalize-space(.)"/></f:string>
              </xsl:for-each>
            </xsl:for-each>
          </f:array>
        </xsl:if>
        <!-- Extract title if present.-->
        <xsl:if test="m:titleInfo/m:title">
          <f:array key="title">
            <xsl:for-each select="m:titleInfo/m:title">
              <f:string>
                <xsl:value-of select="f:replace(., 'zh\|', '')"/>
              </f:string>
            </xsl:for-each>
          </f:array>
          <!--Count hack - writing to a variable for each entry,
              then counting length of variable string to get amount -->
          <xsl:variable name="title_count">
            <xsl:for-each select="m:titleInfo/m:title">
              <xsl:text>a</xsl:text>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="title_count">
            <xsl:value-of select="f:string-length($title_count)"/>
          </f:string>
          <!-- SINGLE TITEL EXTRACTION -->
          <!-- <f:string key="title">
            <xsl:value-of select="f:replace(m:titleInfo/m:title[1], 'zh\|', '')"/>
          </f:string> -->
        </xsl:if>
        <!-- Extract subtitle if present.-->
        <xsl:if test="m:titleInfo/m:subTitle">
          <f:string key="subtitle">
            <xsl:value-of select="f:replace(m:titleInfo/m:subTitle[1], 'zh\|', '')"/>
          </f:string>
        </xsl:if>
        <!-- Extract alternative title if present.-->
        <xsl:if test="m:titleInfo[@type='alternative']/m:title">
          <f:string key="alternative_title">
            <xsl:value-of select="f:replace(m:titleInfo[@type='alternative']/m:title[1], 'zh\|', '')"/>
          </f:string>
        </xsl:if>
        <!-- Extracts information on creator of item. This comes from one of three roleTerm codes,
             which are MARC identifiers: https://www.loc.gov/marc/relators/relacode.html-->
        <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'aut' or 'art'">
          <xsl:if test="m:name/m:namePart and m:name/m:namePart !=''">
            <!-- Some creators are represented by only their given or family names.
            This field contains whatever is present, extracted through an XSLT choose logic -->
            <f:array key="creator_name">
              <xsl:for-each select="m:name">
                <xsl:if test="./m:namePart[@type='family'] or ./m:namePart[@type='given']">
                  <f:string>
                    <xsl:choose>
                      <xsl:when test="m:namePart[@type='family'] and m:namePart[@type='given']
                                        and m:namePart[@type='family'] != '' and m:namePart[@type='given'] != ''">
                        <xsl:value-of select="normalize-space(concat(f:replace(m:namePart[@type='family'], 'zh\|', '')
                                                ,', ', f:replace(m:namePart[@type='given'],'zh\|', '')))"/>
                      </xsl:when>
                      <xsl:when test="m:namePart[@type='family'] and not (m:namePart[@type='given'])">
                        <xsl:value-of select="f:replace(m:namePart[@type='family'], 'zh\|', '')"/>
                      </xsl:when>
                      <xsl:when test="m:namePart[@type='given'] and not (m:namePart[@type='family'])">
                        <xsl:value-of select="f:replace(m:namePart[@type='given'], 'zh\|', '')"/>
                      </xsl:when>
                    </xsl:choose>
                  </f:string>
                </xsl:if>
              </xsl:for-each>
            </f:array>
            <!-- Extracts family and given name and combines into a full name-->
            <f:array key="creator_full_name">
              <xsl:for-each select="m:name">
                <xsl:if test="./m:namePart[@type='family'] or ./m:namePart[@type='given']">
                  <f:string>
                    <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='given'], ' ',
                                                       m:namePart[@type='family'])), 'zh\|', '')"/>
                  </f:string>
                </xsl:if>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <!-- Extract family name if present-->
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'aut' or 'art'">
            <xsl:if test="m:name/m:namePart[@type='family']">
              <f:array key="creator_family_name">
                <xsl:for-each select="m:name">
                  <xsl:if test="m:namePart[@type='family']">
                    <f:string>
                      <xsl:value-of select="f:replace(m:namePart[@type='family'], 'zh\|', '')"/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </f:array>
            </xsl:if>
          </xsl:if>
          <!-- Extract given name if present-->
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'aut' or 'art'">
            <xsl:if test="m:name/m:namePart[@type='given']">
              <f:array key="creator_given_name">
                <xsl:for-each select="m:name">
                  <xsl:if test="m:namePart[@type='given']">
                    <f:string>
                      <xsl:value-of select="f:replace(m:namePart[@type='given'], 'zh\|', '')"/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </f:array>
            </xsl:if>
          </xsl:if>
          <!-- Extract terms of address if present-->
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'art' or 'aut'">
            <xsl:if test="m:name/m:namePart[@type='termsOfAddress']">
              <f:array key="creator_terms_of_address">
                <xsl:for-each select="m:name">
                  <xsl:if test="m:namePart[@type='termsOfAddress']">
                    <f:string>
                      <xsl:value-of select="f:replace(m:namePart[@type='termsOfAddress'], 'zh\|', '')"/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </f:array>
            </xsl:if>
          </xsl:if>
          <!-- Extract creator date of birth and death if present.
               Complex extraction that saves the first value for a creator and then matches this date
               up against three different patterns to determine which dates are present.
               TODO: This works, but might not return all death dates if the first result doesn't have a death date -->
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'art' or 'aut'">
            <xsl:if test="m:name/m:namePart[@type='date']">
              <!-- Select single testDate -->
              <xsl:variable name="testDate" select="m:name[1]"/>
              <!-- Scenario 1 -->
              <xsl:choose>
                <xsl:when test="matches($testDate, '\d+-\d+-\d+/\d+-\d+-\d+')">
                  <f:array key="creator_date_of_birth">
                    <xsl:for-each select="m:name">
                      <xsl:if test="substring-before(m:namePart[@type='date'], '/') != ''">
                        <f:string>
                          <xsl:value-of select="substring-before(m:namePart[@type='date'], '/')"/>
                        </f:string>
                      </xsl:if>
                    </xsl:for-each>
                  </f:array>
                  <f:array key="creator_date_of_death">
                    <xsl:for-each select="m:name">
                      <xsl:if test="substring-after(m:namePart[@type='date'], '/') != ''">
                        <f:string>
                          <xsl:value-of select="substring-after(m:namePart[@type='date'], '/')"/>
                        </f:string>
                      </xsl:if>
                    </xsl:for-each>
                  </f:array>
                </xsl:when>
                <!-- Scenario 2 -->
                <xsl:when test="matches($testDate, '\d+-\d+-\d+/')">
                  <f:array key="creator_date_of_birth">
                    <xsl:for-each select="m:name">
                      <xsl:if test="substring-before(m:namePart[@type='date'], '/') != ''">
                        <f:string>
                          <xsl:value-of select="substring-before(m:namePart[@type='date'], '/')"/>
                        </f:string>
                      </xsl:if>
                    </xsl:for-each>
                  </f:array>
                </xsl:when>
                <!-- Scenario 3 -->
                <xsl:when test="matches($testDate, '/\d+-\d+-\d+')">
                  <f:array key="creator_date_of_death">
                    <xsl:for-each select="m:name">
                      <xsl:if test="substring-after(m:namePart[@type='date'], '/') != ''">
                        <f:string>
                          <xsl:value-of select="substring-after(m:namePart[@type='date'], '/')"/>
                        </f:string>
                      </xsl:if>
                    </xsl:for-each>
                  </f:array>
                </xsl:when>
              </xsl:choose>
            </xsl:if>
            <!-- If there is an affiliation for the creator it gets extracted here-->
            <xsl:if test="m:name/m:affiliation">
              <f:array key="creator_affiliation">
                <xsl:for-each select="m:name/m:affiliation">
                  <xsl:if test=". != ''">
                    <f:string>
                      <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </f:array>
            </xsl:if>
            <!-- Description of affiliation, maybe this could have a better field name-->
            <xsl:if test="m:name/m:description">
              <f:array key="creator_affiliation_description">
                <xsl:for-each select="m:name/m:description">
                  <xsl:if test=". != ''">
                    <f:string>
                      <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                    </f:string>
                  </xsl:if>
                </xsl:for-each>
              </f:array>
            </xsl:if>
          </xsl:if>
          <!-- Not ideal workaround for counting number of creators for a resource.
               For each creator an 'a' char is written to a string, then the length of this string
               is returned as the amount of creators.-->
          <xsl:variable name="creator_count">
            <xsl:for-each select="m:name">
              <xsl:text>a</xsl:text>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="creator_count">
            <xsl:value-of select="f:string-length($creator_count)"/>
          </f:string>
        </xsl:if>
        <!-- Information on the original gets extracted here. Production date primarily-->
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
              <f:string key="production_date">
                <xsl:value-of select="m:originInfo[@altRepGroup='original']/m:dateCreated"/>
              </f:string>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <xsl:if test="m:originInfo/m:place">
          <f:string key="production_place">
            <xsl:value-of select="normalize-space(m:originInfo/m:place)"/>
          </f:string>
        </xsl:if>
        <xsl:if test="m:originInfo[@altRepGroup='surrogate']/m:dateCaptured">
          <f:string key="production_date_digital_surrogate">
            <xsl:value-of select="m:originInfo[@altRepGroup='surrogate']/m:dateCaptured"/>
          </f:string>
        </xsl:if>
        <!-- The topic of given item gets extracted here, if present-->
        <xsl:if test="m:subject/m:topic[@lang]">
          <f:array key="topic">
            <xsl:for-each select="m:subject">
              <xsl:for-each select="m:topic[@lang]">
                <xsl:if test=". != ''">
                  <f:string>
                    <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                  </f:string>
                </xsl:if>
              </xsl:for-each>
            </xsl:for-each>
          </f:array>
          <xsl:variable name="topic_count">
            <xsl:for-each select="m:subject/m:topic[@lang]">
              <xsl:text>a</xsl:text>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="topic_count">
            <xsl:value-of select="f:string-length($topic_count)"/>
          </f:string>
        </xsl:if>
        <!-- geographical data gets extracted here -->
        <xsl:if test="m:subject/m:hierarchicalGeographic">
          <xsl:for-each select="m:subject/m:hierarchicalGeographic">
            <f:string key="area">
              <xsl:value-of select="m:area"/>
            </f:string>
          </xsl:for-each>
        </xsl:if>
        <!-- Data on subject person gets extracted here. Including names, dates of birth, death and terms of address -->
        <xsl:if test="m:subject/m:name">
          <f:array key="subject_name">
            <xsl:for-each select="m:subject/m:name">
              <f:string>
                <xsl:choose>
                  <xsl:when test="m:namePart[@type='family'] and m:namePart[@type='given']">
                    <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='family'],', ',
                                                                    m:namePart[@type='given'])), 'zh\|', '')"/>
                  </xsl:when>
                  <xsl:when test="m:namePart[@type='family'] and not (m:namePart[@type='given'])">
                    <xsl:value-of select="f:replace(m:namePart[@type='family'], 'zh\|', '')"/>
                  </xsl:when>
                  <xsl:when test="m:namePart[@type='given'] and not (m:namePart[@type='family'])">
                    <xsl:value-of select="f:replace(m:namePart[@type='given'], 'zh\|', '')"/>
                  </xsl:when>
                </xsl:choose>
              </f:string>
            </xsl:for-each>
          </f:array>
          <f:array key="subject_full_name">
            <xsl:for-each select="m:subject/m:name">
              <f:string>
                <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='given'],' ',
                                                                     m:namePart[@type='family'])), 'zh\|', '')"/>
              </f:string>
            </xsl:for-each>
          </f:array>
          <xsl:if test="m:subject/m:name/m:namePart[@type='family'] and m:subject/m:name/m:namePart[@type='family'] != ''">
            <f:array key="subject_family_name">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="f:replace(m:namePart[@type='family'], 'zh\|', '')"/>
                </f:string>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:if test="m:subject/m:name/m:namePart[@type='given'] and m:subject/m:name/m:namePart[@type='given'] != ''">
            <f:array key="subject_given_name">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="f:replace(m:namePart[@type='given'], 'zh\|', '')"/>
                </f:string>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:if test="m:subject/m:name/m:namePart[@type='date'] and m:subject/m:name/m:namePart[@type='date'] != ''">
            <f:array key="subject_date_of_birth">
              <xsl:for-each select="m:subject/m:name">
                <xsl:variable name="subjectDateOfBirth" select="substring-before(m:namePart[@type='date'], '/')"/>
                <xsl:choose>
                  <xsl:when test="matches($subjectDateOfBirth, '\d{4}-0-0')">
                    <f:string>
                      <xsl:value-of select="substring-before($subjectDateOfBirth, '-')"/>
                    </f:string>
                  </xsl:when>
                  <xsl:when test="matches($subjectDateOfBirth, '\d{4}-\d{2}-0')">
                    <xsl:analyze-string select="$subjectDateOfBirth" regex="(\d{{4}}-\d{{2}})-0">
                      <xsl:matching-substring>
                        <f:string>
                          <xsl:value-of select="f:regex-group(1)"/>
                        </f:string>
                      </xsl:matching-substring>
                    </xsl:analyze-string>
                  </xsl:when>
                  <xsl:otherwise>
                    <f:string>
                      <xsl:value-of select="$subjectDateOfBirth"/>
                    </f:string>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </f:array>
            <f:array key="subject_date_of_death">
              <xsl:for-each select="m:subject/m:name">
                <xsl:variable name="subjectDateOfDeath" select="substring-after(m:namePart[@type='date'], '/')"/>
                <xsl:choose>
                  <xsl:when test="matches($subjectDateOfDeath, '\d{4}-0-0')">
                    <f:string>
                      <xsl:value-of select="substring-before($subjectDateOfDeath, '-')"/>
                    </f:string>
                  </xsl:when>
                  <xsl:when test="matches($subjectDateOfDeath, '\d{4}-\d{2}-0')">
                    <xsl:analyze-string select="$subjectDateOfDeath" regex="(\d{{4}}-\d{{2}})-0">
                      <xsl:matching-substring>
                        <f:string>
                          <xsl:value-of select="f:regex-group(1)"/>
                        </f:string>
                      </xsl:matching-substring>
                    </xsl:analyze-string>
                  </xsl:when>
                  <xsl:otherwise>
                    <f:string>
                      <xsl:value-of select="$subjectDateOfDeath"/>
                    </f:string>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:if test="m:subject/m:name/m:namePart[@type='termsOfAddress'] and m:subject/m:name/m:namePart[@type='termsOfAddress'] != ''">
            <f:array key="subject_terms_of_address">
              <xsl:for-each select="m:subject/m:name">
                <f:string>
                  <xsl:value-of select="f:replace(m:namePart[@type='termsOfAddress'], 'zh\|', '')"/>
                </f:string>
              </xsl:for-each>
            </f:array>
          </xsl:if>
          <xsl:variable name="subject_count">
            <xsl:for-each select="m:subject/m:name">
              <xsl:text>a</xsl:text>
            </xsl:for-each>
          </xsl:variable>
          <f:string key="subject_count">
            <xsl:value-of select="f:string-length($subject_count)"/>
          </f:string>
        </xsl:if>
        <xsl:if test="m:subject/m:cartographics/m:scale">
          <f:string key="map_scale">
            <xsl:value-of select="m:subject/m:cartographics/m:scale"/>
          </f:string>
        </xsl:if>
        <!-- Extract resource id-->
        <xsl:if test="m:relatedItem[@type='otherFormat']/m:identifier[@displayLabel='image'][@type='uri']">

          <xsl:variable name="imageIdentifier">
            <xsl:value-of select="substring-after(m:relatedItem[@type='otherFormat']/
                                  m:identifier[@displayLabel='image'][@type='uri'], $old_imageserver)"/>
          </xsl:variable>
          <xsl:variable name="imageIdentifierNoExtension">
            <xsl:value-of select="f:substring-before($imageIdentifier, '.jp')"/>
          </xsl:variable>
          <xsl:variable name="imageIdentifierDoubleEncoded">
            <xsl:value-of select="f:encode-for-uri(f:encode-for-uri($imageIdentifierNoExtension))"/>
          </xsl:variable>
          <xsl:variable name="thumbnailUrl">
            <xsl:value-of select="concat($imageserver, $imageIdentifierDoubleEncoded, '/full/', f:encode-for-uri('!150,150'), '/0/default.jpg')"/>
          </xsl:variable>
          <f:array key="resource_id">
            <f:string>
              <xsl:value-of select="$imageIdentifierNoExtension"/>
            </f:string>
          </f:array>
          <f:string key="image_iiif_id">
            <xsl:value-of select="concat($imageserver, $imageIdentifierDoubleEncoded)"/>
          </f:string>
          <f:string key="thumbnail">
            <xsl:value-of select="$thumbnailUrl"/>
          </f:string>
        </xsl:if>
        <xsl:if test="m:genre[not(@*)]">
          <f:string key="genre">
            <xsl:value-of select="m:genre[not(@*)]"/>
          </f:string>
        </xsl:if>
        <!-- resource_type should get data from either typeOfResource[@displayLabel='Resource Description'] or
             typeOfResource[@displayLabel='Generel Resource Description']. Resource Description contains a specific
             description of the resource, while Generel Resource Description is a catch-all-description for broader
             categories. -->
        <xsl:choose>
          <xsl:when test="m:typeOfResource[@displayLabel='Resource Description']">
            <f:string key="resource_description">
              <xsl:value-of select="m:typeOfResource[@displayLabel='Resource Description']"/>
            </f:string>
          </xsl:when>
          <xsl:otherwise>
            <f:string key="resource_description">
              <xsl:value-of select="m:typeOfResource[@displayLabel='Generel Resource Description']"/>
            </f:string>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="m:typeOfResource[@displayLabel='Generel Resource Description']">
          <f:string key="resource_description_general">
            <xsl:value-of select="m:typeOfResource[@displayLabel='Generel Resource Description']"/>
          </f:string>
        </xsl:if>
        <!-- Create audience. This is not in our test files
             but part of MODS standard as target_audience and Dublin Core as audience.
             Could be in use in other collections. -->
        <xsl:if test="m:targetInfo">
          <f:string key="audience">
            <xsl:value-of select="m:targetInfo"/>
          </f:string>
        </xsl:if>

      </xsl:for-each>
      <!--- End XSLT logic -->
    </f:map>
   </xsl:variable>

    <!--
    <xsl:variable name="cleanJson" select="remove($json, 20)"/>
    -->

    <!-- Define output -->
    <xsl:value-of select="f:xml-to-json($json)"/>
  </xsl:template>

</xsl:transform>
