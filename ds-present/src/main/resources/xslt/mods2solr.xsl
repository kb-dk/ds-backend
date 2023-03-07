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
          <!-- Extracting cataloging language if it exists-->
          <xsl:if test="m:recordInfo/m:languageOfCataloging/m:languageTerm">
            <xsl:for-each select="m:recordInfo/m:languageOfCataloging/m:languageTerm[1]">
              <f:string key="cataloging_language">
                <xsl:value-of select="."/>
              </f:string>
            </xsl:for-each>
          <!-- Extracting physical location if it exists-->
          </xsl:if>
          <xsl:if test="m:location/m:physicalLocation">
            <f:string key="physical_location">
              <xsl:value-of select="m:location/m:physicalLocation"/>
            </f:string>
          </xsl:if>
          <!-- Extracting shelf location if it exists-->
          <xsl:if test="m:location/m:shelfLocator">
            <f:string key="shelf_location">
              <xsl:value-of select="m:location/m:shelfLocator"/>
            </f:string>
          </xsl:if>
          <!-- Extracts id and strips if for urn:uuid:-->
          <f:string key="id">
            <xsl:value-of select="substring-after(m:identifier[@type='uri'],'urn:uuid:')"/>
          </f:string>
          <!-- Extracts local identifier-->
          <f:string key="identifier_local">
            <xsl:value-of select="m:identifier[@type='local']"/>
          </f:string>
          <xsl:if test="m:identifier[@type='accession number']">
            <f:string key="accession_number">
              <xsl:value-of select="m:identifier[@type='accession number']"/>
            </f:string>
          </xsl:if>
          <!-- Categories seems to be a collection of multiple categories seperated by commas. -->
          <!-- According to the MODS standard genre should contain info more specific than typeOfResource -->
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
          </xsl:if>

          <!-- Different things can be represented in note. -->
          <!-- If catalog name exists, extract it-->
          <xsl:if test="m:note[@displayLabel='Catalog Name']">
            <f:string key="catalog_name">
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
          <xsl:if test="m:note[@type='content']">
            <f:array key="content">
              <xsl:for-each select="m:note[@type='content']">
                <xsl:if test=". != ''">
                  <f:string>
                    <!-- First use of replace 'zh|'. This check is done multiple times to remove 'zh|' in front of chinese characters -->
                    <xsl:value-of select="f:replace(., 'zh\|', '')"/>
                  </f:string>
                </xsl:if>
              </xsl:for-each>
            </f:array>
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
          <xsl:if test="m:note[@displayLabel='Description'] or m:physicalDescription/not(m:note[@displayLabel='Pageorientation'])">
            <f:array key="description">
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
            <f:string key="title">
              <xsl:value-of select="f:replace(m:titleInfo/m:title, 'zh\|', '')"/>
            </f:string>
          </xsl:if>
          <!-- Extract subtitle if present.-->
          <xsl:if test="m:titleInfo/m:subTitle">
            <f:string key="subtitle">
              <xsl:value-of select="f:replace(m:titleInfo/m:subTitle, 'zh\|', '')"/>
            </f:string>
          </xsl:if>
          <!-- Extract alternative title if present.-->
          <xsl:if test="m:titleInfo[@type='alternative']/m:title">
            <f:string key="alternative_title">
              <xsl:value-of select="f:replace(m:titleInfo[@type='alternative']/m:title, 'zh\|', '')"/>
            </f:string>
          </xsl:if>
          <!-- Extracts information on creator of item.
          This comes from one of three roleTerm codes, which are in fact MARC identifiers: https://www.loc.gov/marc/relators/relacode.html-->
          <xsl:if test="m:name/m:role/m:roleTerm[@type='code']='cre' or 'aut' or 'art'">
            <xsl:if test="m:name/m:namePart and m:name/m:namePart !=''">
              <!-- Some creators are represented by only their given or family names.
              This field contains whatever is present, extracted through an XSLT choose logic -->
              <f:array key="creator_name">
                <xsl:for-each select="m:name">
                  <xsl:if test="./m:namePart[@type='family'] or ./m:namePart[@type='given']">
                    <f:string>
                      <xsl:choose>
                        <xsl:when test="m:namePart[@type='family'] and m:namePart[@type='given'] and m:namePart[@type='family'] != '' and m:namePart[@type='given'] != ''">
                          <xsl:value-of select="normalize-space(concat(f:replace(m:namePart[@type='family'], 'zh\|', ''),', ', f:replace(m:namePart[@type='given'],'zh\|', '')))"/>
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
                      <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='given'], ' ',m:namePart[@type='family'])), 'zh\|', '')"/>
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
                 This works, but might not return all death dates if the first result doesn't have a death date -->
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
                <f:array key="creator_description">
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
                <f:string key="date_created">
                  <xsl:value-of select="m:originInfo[@altRepGroup='original']/m:dateCreated"/>
                </f:string>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:if test="m:originInfo/m:place">
            <f:string key="place_of_production">
              <xsl:value-of select="normalize-space(m:originInfo/m:place)"/>
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
                      <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='family'],', ', m:namePart[@type='given'])), 'zh\|', '')"/>
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
                <xsl:value-of select="f:replace(normalize-space(concat(m:namePart[@type='given'],' ',m:namePart[@type='family'])), 'zh\|', '')"/>
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
          </xsl:if>
        <!-- Display label can in theory contain anything.
             Resource Description seems to be the field that contains the most precise description of a given resource. -->
        <xsl:if test="m:typeOfResource[@displayLabel='Resource Description']">
          <f:string key="resource_description">
            <xsl:value-of select="m:typeOfResource[@displayLabel='Resource Description']"/>
          </f:string>
        </xsl:if>
          <!-- Type of resource gets extracted here if present -->
          <!-- TODO: Should probably be named something like resource_categories -->
          <xsl:if test="m:typeOfResource">
            <f:array key="type_of_resource">
              <xsl:for-each select="m:typeOfResource">
                <f:string>
                  <xsl:value-of select="."/>
                </f:string>
              </xsl:for-each>
            </f:array>
          </xsl:if>
      </xsl:for-each>
      <!--- This is the METS element with image metadata from the PremisObject
       This extraction is used to provide information on image size-->
      <xsl:for-each select="//mets:amdSec/mets:techMD[@ID='PremisObject1']//premis:objectCharacteristics">
        <xsl:if test="premis:size">
          <f:string key="file_size">
            <xsl:value-of select="premis:size"/>
          </f:string>
        </xsl:if>
        <!-- Extracts the height of the digital image-->
        <xsl:if test="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight">
        <f:string key="image_height">
          <xsl:value-of select="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight"/>
        </f:string>
        </xsl:if>
        <!-- Extracts the width of the digital image-->
        <xsl:if test="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth">
        <f:string key="image_width">
          <xsl:value-of select="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth"/>
        </f:string>
        </xsl:if>
        <!-- Calculates the size of the digital image-->
        <xsl:if test="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight * premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth">
        <f:string key="image_size">
          <xsl:value-of select="premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageHeight * premis:objectCharacteristicsExtension/mix:mix/mix:BasicImageInformation/mix:BasicImageCharacteristics/mix:imageWidth"/>
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
