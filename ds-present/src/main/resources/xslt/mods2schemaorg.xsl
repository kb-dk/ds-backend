<?xml version="1.0" encoding="UTF-8" ?>
<xsl:transform xmlns:m="http://www.loc.gov/mods/v3"
               xmlns:t="http://www.tei-c.org/ns/1.0"
               xmlns:f="http://www.w3.org/2005/xpath-functions"
               xmlns:h="http://www.w3.org/1999/xhtml"
               xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:my="urn:my"
               version="3.0">

  
  <xsl:output method="text" />
  <!-- xsl:output method="xml" / -->

  <xsl:param name="sep_string" select="'/'"/>

  <xsl:variable name="roles">
    <roles>
      <role key="act" href="https://schema.org/actor">actor</role>
      <role key="art" href="https://schema.org/artist ">artist</role>
      <role key="aut" href="https://schema.org/author">author</role>
      <role key="cre" href="https://schema.org/creator">creator</role>
      <role key="creator" href="https://schema.org/creator">creator</role>
      <role key="ctb" href="https://schema.org/contributor">contributor</role>
      <role key="rcp" href="https://schema.org/recipient">recipient</role>
      <role key="scr" href="http://id.loc.gov/vocabulary/relators/art">relator:scr</role>
      <role key="src" href="http://id.loc.gov/vocabulary/relators/art">relator:scr</role>
      <role key="trl" href="https://schema.org/translator">translator</role>
      <role key="pat" href="https://schema.org/funder">funder</role>
      <role key="prt" href="http://id.loc.gov/vocabulary/relators/art">relator:prt</role>
    </roles>
  </xsl:variable>
  
  <xsl:template match="/">
    
    <xsl:variable name="json">

      <f:map>
        <f:string key="@context">http://schema.org/</f:string>
        <f:string key="@type">DataFeed</f:string>
        <f:array  key="dataFeedElement">
          <xsl:for-each select="//m:mods">

            <xsl:variable name="cataloging_language">
              <xsl:for-each select="m:recordInfo/m:languageOfCataloging/m:languageTerm[1]">
                <xsl:value-of select="."/>              
              </xsl:for-each>
            </xsl:variable>
            
            <xsl:variable name="dom" select="."/>

            <xsl:variable name="record-id-in">
              <xsl:choose>
                <xsl:when test="processing-instruction('cobject_id')">
                  <xsl:value-of select="replace(processing-instruction('cobject_id'),'^/','')"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="replace(m:recordInfo/m:recordIdentifier,'^/','')"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>

            <xsl:variable name="record-id">
              <xsl:value-of select="replace($record-id-in,'/',$sep_string,'s')"/>
            </xsl:variable>

            <xsl:variable name="output_data">
              <f:map>

                <f:array key="@context">
		  <f:string>http://schema.org/</f:string>
		  <f:map>
                    <f:string key="kb">http://kb.dk/vocabs/</f:string>
		    <f:string key="relator">https://id.loc.gov/vocabulary/relators/</f:string>
                  </f:map>
                </f:array>
                
                <!-- record identification, admin data etc  -->

                <xsl:variable name="edition">
                  <xsl:value-of select="substring-before($record-id,concat($sep_string,'object'))"/>
                </xsl:variable>

                <f:string key="@type">
                  <xsl:choose>
                    <xsl:when test="contains($record-id,'images')">VisualArtwork</xsl:when>
                    <xsl:when test="contains($record-id,'manus')">Manuscript</xsl:when>
                    <xsl:when test="contains($record-id,'letters')">Message</xsl:when>
                    <xsl:when test="contains($record-id,'books')">Book</xsl:when>
                    <xsl:otherwise>CreativeWork</xsl:otherwise>
                  </xsl:choose>
                </f:string>
                
                <f:string key="id">
                  <xsl:value-of select="$record-id"/>
                </f:string>

                <f:string key="url">
                  <xsl:choose>
                    <xsl:when test="contains($record-id,'luftfo')">
                      <xsl:value-of select="concat('http://www5.kb.dk/danmarksetfraluften/',$record-id)"/>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="concat('http://www5.kb.dk/',$record-id,'/en/')"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </f:string>
                
                <f:map key="kb:admin_data">
                  <f:array>
                    <xsl:attribute name="key">kb:last_modified_by</xsl:attribute>
                    <xsl:for-each select="m:name[@type='cumulus' and m:role/m:roleTerm = 'last-modified-by']">
                      <xsl:call-template name="get-names">
                        <xsl:with-param name="record_identifier" select="$record-id"/>
                        <xsl:with-param name="cataloging_language" select="$cataloging_language" />
                        <xsl:with-param name="agent_type" select="'Person'" />
                      </xsl:call-template>
                    </xsl:for-each>
                  </f:array>
                  
                  <xsl:for-each select="m:recordInfo/m:recordCreationDate">
                    <f:string key="kb:record_created"><xsl:value-of select="."/></f:string>
                  </xsl:for-each> 

                  <xsl:for-each select="m:recordInfo/m:recordChangeDate">
                    <f:string key="kb:record_revised"><xsl:value-of select="."/></f:string>
                  </xsl:for-each>

                  <f:string key="kb:cataloging_language">
                    <xsl:value-of select="$cataloging_language"/>
                  </f:string>
                  
                </f:map>
                
                <!-- basic bibliographic metadata -->

                <xsl:if test="m:titleInfo/m:title">
                  <xsl:call-template name="get-title">
                    <xsl:with-param name="cataloging_language" select="$cataloging_language" />
                  </xsl:call-template>
                </xsl:if>
                
                <xsl:for-each select="distinct-values(m:name/m:role/m:roleTerm)">
                  <xsl:variable name="term" select="."/>
                  <xsl:if test="not(contains($term,'last-modified-by'))">
                    <f:array>
                      <xsl:attribute name="key"><xsl:value-of select="$roles/roles/role[@key=$term]"/></xsl:attribute>
                      <xsl:for-each select="$dom//m:name[m:role/m:roleTerm = $term]">
                        <xsl:call-template name="get-names">
                          <xsl:with-param name="record_identifier" select="$record-id"/>
                          <xsl:with-param name="cataloging_language" select="$cataloging_language" />
                        </xsl:call-template>
                      </xsl:for-each>
                    </f:array>
                  </xsl:if>
                </xsl:for-each>

                <!-- *********************** misc notes               ******************** -->

                <xsl:if test="m:note[not(@type or @displayLabel)]">
                  <f:array key="description">
                    <xsl:for-each select="m:note[not(@type or @displayLabel)]">
                      <f:string><xsl:value-of select="."/></f:string>
                    </xsl:for-each>
                  </f:array>
                </xsl:if>

                <xsl:if test="m:note[@type or @displayLabel]">
                  <xsl:for-each select="m:note[@type or @displayLabel and not(contains(@type,'situation'))]">
                    <xsl:variable name="the_field">
                      <xsl:choose>
                        <xsl:when test="@type">
                          <xsl:choose>
                            <xsl:when test="contains(@type,'citation/reference')">citation</xsl:when>
                            <xsl:when test="matches( @displayLabel,'ript$')">kb:script</xsl:when>
                            <xsl:when test="contains( @displayLabel,'ript:')">kb:scriptDetail</xsl:when>
                            <xsl:otherwise><xsl:value-of select="concat('kb:',my:escape_stuff(@type))"/></xsl:otherwise>
                          </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:value-of select="concat('kb:',my:escape_stuff(@displayLabel),generate-id(.))"/>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:variable>
                    <f:string>
                      <xsl:attribute name="key">
                        <xsl:value-of select="$the_field"/>
                      </xsl:attribute>
                      <xsl:value-of select="."/>
                    </f:string>
                  </xsl:for-each>

                </xsl:if>
                
                <xsl:if test="m:subject/m:hierarchicalGeographic">
                  <f:array key="contentLocation">
                    <f:map>
                      <f:string key="@type">Place</f:string>
                      <f:map key="kb:address">
                          <f:string key="@type">kb:place</f:string>
                          <xsl:for-each select="m:subject/m:hierarchicalGeographic">
                            <xsl:for-each select="m:area">
                              <xsl:element name="f:string">
                                <xsl:attribute name="key">kb:<xsl:value-of select="@areaType"/></xsl:attribute>
                                <xsl:value-of select="."/>
                              </xsl:element>
                            </xsl:for-each>
                            <xsl:for-each select="m:citySection">
                              <xsl:element name="f:string">
                                <xsl:attribute name="key">kb:<xsl:value-of select="@citySectionType"/></xsl:attribute>
                                <xsl:value-of select="."/>
                              </xsl:element>
                            </xsl:for-each>
                            <xsl:if test="m:city">
                              <f:string key="kb:city"><xsl:value-of select="m:city"/></f:string>
                            </xsl:if>
                        </xsl:for-each>
                      </f:map>
                      <xsl:for-each select="m:subject/m:cartographics/m:coordinates[1]">
                        <xsl:if test="not(contains(.,'0.0,0.0'))">
                          <f:string key="latitude"><xsl:value-of select="substring-before(.,',')"/></f:string>
                          <f:string key="longitude"><xsl:value-of select="substring-after(.,',')"/></f:string>
                        </xsl:if>
                      </xsl:for-each>

                      
                    </f:map>


                    
                  </f:array>
                </xsl:if>
                
                <f:array key="about">

                  <xsl:if test="./m:relatedItem[@type='event']/node() or m:note[@type='situation']/node()">
                    <f:map>

                      <f:string key="@type">Event</f:string>
                      
                      <f:string key="name">
                        <xsl:for-each select="./m:note[@type='situation']">
                          <xsl:value-of select="."/>
                        </xsl:for-each>
                      </f:string>

                      <xsl:for-each select="./m:relatedItem[@type='event']">
                        <xsl:if test="./m:note">
                          <f:array key="description">
                            <xsl:for-each select="./m:note">
                              <f:string><xsl:value-of select="."/></f:string>
                            </xsl:for-each>
                          </f:array>
                        </xsl:if>

                        <xsl:for-each select="m:originInfo">
                          <f:string key="startDate">
                            <xsl:value-of select="m:dateIssued"/>
                          </f:string>
                          <f:string key="endDate">
                            <xsl:value-of select="m:dateIssued"/>
                          </f:string>
                          <f:string key="location">
                            <xsl:value-of select="m:place/m:placeTerm"/>
                          </f:string>
                        </xsl:for-each>
                        
                      </xsl:for-each>
                      
                    </f:map>
                    
                  </xsl:if>


                  
                  <!-- *********************** Subjects, Categories etc ******************** -->

                  <xsl:for-each select="distinct-values(m:subject/m:name/m:role/m:roleTerm)">
                    <xsl:variable name="term" select="."/>
                    <xsl:for-each select="$dom//m:subject/m:name[m:role/m:roleTerm = $term]">
                      <xsl:call-template name="get-names">
                        <xsl:with-param name="record_identifier" select="$record-id"/>
                        <xsl:with-param name="cataloging_language" select="$cataloging_language" />
                      </xsl:call-template>
                    </xsl:for-each>
                  </xsl:for-each>
                  
                  <xsl:for-each select="distinct-values(m:subject/m:topic)">
                    <f:string><xsl:value-of select="."/></f:string>
                  </xsl:for-each>

                  <xsl:if test="m:subject/m:geographic">
                    <xsl:for-each select="distinct-values(m:subject/m:geographic)">
                      <f:string><xsl:value-of select="."/></f:string>
                    </xsl:for-each>
                  </xsl:if>
                  
                  <xsl:variable name="categories" as="xs:string *">
                    <xsl:for-each select="m:extension/h:div">
                      <xsl:for-each select="h:a[@href]">
                        <xsl:sort select="@href" data-type="text" />
                        <xsl:if test="not(contains(@href,'editions'))">
                          <xsl:variable name="cat"><xsl:value-of
                          select="replace(@href,'^.*subject(\d+).*$','subject$1')"/></xsl:variable>
                          <xsl:value-of select="$cat"/>
                        </xsl:if>
                      </xsl:for-each>
                    </xsl:for-each>
                  </xsl:variable>

                  <xsl:for-each select="distinct-values($categories)" >
                    <xsl:variable name="subject"
                                  select="concat(replace(.,'(.*/sub)([^/]+)','sub$2'),'')"/>
                    <f:map>

                      <f:string key="@type">DefinedTerm</f:string>
                      <f:string key="id"><xsl:value-of select="concat(substring-before($record-id,'object'),$subject)"/></f:string>
                      <f:array key="name">
                        <f:map>
                          <f:string key="@language">da</f:string>
                          <f:string key="@value">
                            <xsl:for-each
                                select="distinct-values($dom//h:a[contains(@href,$subject) and @xml:lang='da'])">
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                          </f:string>
                        </f:map>
                        <f:map>
                          <f:string key="@language">en</f:string>
                          <f:string key="@value">
                            <xsl:for-each
                                select="distinct-values($dom//h:a[contains(@href,$subject) and @xml:lang='en'])">
                              <xsl:value-of select="."/>
                            </xsl:for-each>
                          </f:string>
                        </f:map>
                      </f:array>
                    </f:map>
                  </xsl:for-each>
                </f:array>
                
                <!-- dating and origin -->
             
                  <xsl:variable name="to_date">
                    <xsl:choose>
                      <xsl:when test="m:originInfo/m:dateCreated/@t:notAfter">
                        <xsl:for-each select="m:originInfo/m:dateCreated/@t:notAfter">
                          <xsl:value-of select="."/>
                      </xsl:for-each>
                    </xsl:when>
                     <xsl:when test="m:dateCreated/@t:notAfter">
                       <xsl:for-each select="m:dateCreated/@t:notAfter">
                           <xsl:value-of select="."/>
                       </xsl:for-each>
                     </xsl:when>
                     <xsl:otherwise>
                       <xsl:if test="processing-instruction('cobject_not_after')">
                           <xsl:value-of select="processing-instruction('cobject_not_after')"/>
                       </xsl:if>
                     </xsl:otherwise>
                  </xsl:choose>
                  </xsl:variable>

                  <xsl:variable name="from_date">
                    <xsl:choose>
                      <xsl:when test="m:originInfo/m:dateCreated/@t:notBefore">
                        <xsl:for-each select="m:originInfo/m:dateCreated/@t:notBefore">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:when test="m:dateCreated/@t:notBefore">
                        <xsl:for-each select="m:dateCreated/@t:notBefore">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:if test="processing-instruction('cobject_not_before')">
                            <xsl:value-of select="processing-instruction('cobject_not_before')"/>
                        </xsl:if>
                      </xsl:otherwise>
                    </xsl:choose>
                  </xsl:variable>

                  <xsl:variable name="visible_date">
                    <xsl:for-each select="m:originInfo/m:dateCreated|m:dateCreated">
                      <f:string><xsl:value-of select="."/></f:string>
                    </xsl:for-each>
                  </xsl:variable>
                  
                  <xsl:if test="$visible_date/string() or $to_date/string() or $from_date/string()">
                    <f:map key="publication">
                      <f:string key="@type">PublicationEvent</f:string>

                      <xsl:if test="$to_date/string()">
                        <f:string key="endDate"><xsl:value-of select="$to_date"/></f:string>
                      </xsl:if>

                      <xsl:if test="$from_date/string()">
                        <f:string key="startDate"><xsl:value-of select="$from_date"/></f:string>
                      </xsl:if>

                      <xsl:if test="$visible_date/string()">
                        <f:array key="description"><xsl:copy-of select="$visible_date"/></f:array>
                      </xsl:if>

                    </f:map>
                  </xsl:if>

                <!-- physical description and the like -->
                
                <xsl:if test="m:physicalDescription[m:form
                              | m:reformattingQuality
                              | m:internetMediaType
                              | m:extent
                              | m:digitalOrigin
                              | m:note[not(@type='pageOrientation')]]">

                    <xsl:for-each select="m:physicalDescription">
                      <xsl:variable name="label">
                        <xsl:choose>
                          <xsl:when test="@displayLabel"><xsl:value-of select="@displayLabel"/></xsl:when>
                          <xsl:otherwise>
                            <xsl:choose>
                              <xsl:when test="@type"><xsl:value-of select="@type"/></xsl:when>
                              <xsl:otherwise><xsl:value-of select="local-name(.)"/></xsl:otherwise>
                            </xsl:choose>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:variable>

                      <xsl:for-each select="m:form
                                            | m:reformattingQuality
                                            | m:internetMediaType
                                            | m:extent
                                            | m:digitalOrigin
                                            | m:note[not(@type='pageOrientation')]">

                        <xsl:variable name="the_field">
                           <xsl:choose>
                              <xsl:when test="string-length($label) &gt; 0">
                                <xsl:value-of select="my:escape_stuff(lower-case($label))"/>
                              </xsl:when>
                              <xsl:otherwise>
                                <xsl:choose>
                                  <xsl:when test="@type">
                                  <xsl:value-of select="my:escape_stuff(lower-case(@type))"/></xsl:when>
                                  <xsl:otherwise><xsl:value-of select="my:escape_stuff(lower-case(local-name(.)))"/></xsl:otherwise>
                                </xsl:choose>
                              </xsl:otherwise>
                           </xsl:choose>
                        </xsl:variable>
                        
                        <f:map>
                          <xsl:attribute name="key">
                            <xsl:value-of select="$the_field"/>
                          </xsl:attribute>
                          <xsl:choose>
                            <xsl:when test="contains($the_field,'xtent')">
                              <f:string key="@type">QuantitativeValue</f:string>
                              <f:string key="unitText">
                                <xsl:choose>
                                  <xsl:when test="matches(.,'^.*(fol|blad).*$')">folios</xsl:when>
                                  <xsl:otherwise>pages</xsl:otherwise>
                                </xsl:choose>
                              </f:string>
                            </xsl:when>
                            <xsl:otherwise>
                              <xsl:choose>
                                <xsl:when test="$the_field='size'">
                                  <f:string key="@type">QuantitativeValue</f:string>
                                </xsl:when>
                                <xsl:otherwise>
                                  <f:string key="@type">Product</f:string>
                                </xsl:otherwise>
                              </xsl:choose>
                            </xsl:otherwise>
                          </xsl:choose>

                          <f:string key="@value">
                            <xsl:choose>
                              <xsl:when test="contains($the_field,'xtent')">
                                <!-- xsl:value-of select="replace(.,'^.*(\d+).*$','$1')"/ -->
                                <xsl:value-of select="."/>
                              </xsl:when>
                              <xsl:otherwise>
                                <xsl:value-of select="."/>
                              </xsl:otherwise>
                            </xsl:choose>
                          </f:string>
                        </f:map>

                      </xsl:for-each>


                      
                    </xsl:for-each>

                </xsl:if>

                <!-- A library object usually have a more identifiers than
                     a software developer would like -->

                <f:array key="identifier">                
                  <xsl:if test="m:location/m:physicalLocation[@displayLabel='Shelf Mark' and not(@transliteration)]">
                    <xsl:for-each select="m:location/m:physicalLocation[@displayLabel='Shelf Mark' and not(@transliteration)]">
                      <f:map>
                        <f:string key="@type">PropertyValue</f:string>
                        <f:string key="additionalType">shelf_mark</f:string>
                        <f:string key="@value"><xsl:value-of select="."/></f:string>
                      </f:map>
                    </xsl:for-each>
                  </xsl:if>

                  <xsl:for-each select="m:identifier[@type='local'][1]">
                    <f:map>
                      <f:string key="@type">PropertyValue</f:string>
                      <f:string key="additionalType">local_identifier</f:string>
                      <f:string key="@value"><xsl:value-of select="."/></f:string>
                    </f:map>
                  </xsl:for-each>

                  <xsl:for-each select="m:relatedItem[@type='original']/m:identifier">
                    <f:map>
                      <f:string key="@type">PropertyValue</f:string>
                      <f:string key="additionalType">original_object_identifier</f:string>
                      <f:string key="@value"><xsl:value-of select="."/></f:string>
                    </f:map>
                  </xsl:for-each>

                  <xsl:for-each select="m:identifier[@type='domsGuid']">
                    <f:map>
                      <f:string key="@type">PropertyValue</f:string>
                      <f:string key="additionalType">domsGuid</f:string>
                      <f:string key="@value"><xsl:value-of select="."/></f:string>
                    </f:map>
                  </xsl:for-each>
                  
                </f:array>
                
                <xsl:if test="m:relatedItem[m:typeOfResource/@collection='yes']/m:titleInfo/m:title">
                  <f:array key="isPartOf">
                    <xsl:for-each select="m:relatedItem[m:typeOfResource/@collection='yes']">
                      <f:map>
                        <f:string key="@type">Collection</f:string>
                        <f:string key="headline"><xsl:value-of select="m:titleInfo/m:title"/></f:string>
                        <f:string key="description">
                          <xsl:value-of select="m:typeOfResource[@collection='yes']"/>
                        </f:string>
                      </f:map>
                    </xsl:for-each>
                  </f:array>
                </xsl:if>
                

                <xsl:if test="m:language/m:languageTerm[@authority='rfc4646']">
                  <f:array key="inLanguage">
                    <xsl:for-each select="m:language/m:languageTerm[@authority='rfc4646']">
                      <f:string><xsl:value-of select="."/></f:string>
                    </xsl:for-each>
                  </f:array>
                </xsl:if>

                
                <!--
                    This is perhaps not obvious: Normally text is stored
                    in the order it is to be read. However, from the point
                    of view of people used to read western languages (LTR
                    scripts) it might seem odd click on a left-arrow to
                    get next page, but that is the way people reading
                    Chinese, Arabic and Hebrew thinks (RTL scripts). And
                    that is true for languages using those scripts, like
                    Persian (using Arabic script) and Yiddish and Ladino
                    using Hebrew script. Judeo-Arabic is a dialect of
                    Arabic written using Hebrew script. We have all these
                    in our collections.

                    We have all these in our digital
                    collections. However, around 2005-2010 someone
                    decided that the staff doing the digitization
                    cannot learn to recognize RTL or LTR objects, so a
                    lot of texts has been digitized in what was
                    claimed to be the "logical" direction, namely LTR.
                    Instead of correcting the data we have done this
                    in software.

                -->

                <!-- kb:read_direction is in a kb namespace because
                     there isn't any corresponding element or anything
                     in schema.org -->
                
                <xsl:for-each select="m:physicalDescription/m:note[@type='pageOrientation'][1]">
                  <f:string key="kb:read_direction"><xsl:value-of select="."/></f:string>
                </xsl:for-each>

                <xsl:element name="f:array">
                  <xsl:attribute name="key">encoding</xsl:attribute>
                  <xsl:for-each select="m:relatedItem[m:identifier]">
                    <xsl:call-template name="make_page_field"/>
                  </xsl:for-each>
                </xsl:element>
              </f:map>
            </xsl:variable>
            
            <xsl:apply-templates select="$output_data/f:map">
              <xsl:with-param name="record_identifier" select="$record-id"/>
            </xsl:apply-templates>

          </xsl:for-each>
        </f:array>
      </f:map>
    </xsl:variable>
    <xsl:value-of select="f:xml-to-json($json)"/>
    <!-- xsl:copy-of select="$json"/ -->
  </xsl:template>

  <xsl:template name="get-title">
    <xsl:param name="cataloging_language"/>

    <xsl:if test="m:titleInfo[not(@type)]">
      <f:array key="headline">
        <xsl:for-each select="m:titleInfo[not(@type)]">
          <xsl:variable name="xml_lang">
            <xsl:choose>
              <xsl:when test="@xml:lang"><xsl:value-of select="@xml:lang"/></xsl:when>
              <xsl:otherwise><xsl:value-of select="$cataloging_language"/></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:for-each select="m:title">
            <f:map>
              <f:string key="@value"><xsl:value-of select="."/></f:string>
              <f:string key="@language"><xsl:value-of select="$xml_lang"/></f:string>
            </f:map>
          </xsl:for-each>
        </xsl:for-each>
      </f:array>
    </xsl:if>
    <xsl:if test="m:titleInfo/@type">
      <f:array key="alternativeHeadline">
        <xsl:for-each select="m:titleInfo[@type]">
          <xsl:variable name="xml_lang"><xsl:value-of select="@xml:lang"/></xsl:variable>
          <xsl:for-each select="m:title">
            <f:map>
              <f:string key="@value"><xsl:value-of select="."/></f:string>
              <f:string key="@language"><xsl:value-of select="$xml_lang"/></f:string>
            </f:map>
          </xsl:for-each>
        </xsl:for-each>
      </f:array>
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="make_page_field">
    
    <xsl:choose>
      <xsl:when test="m:identifier[@displayLabel='iiif']">

        <f:map>
          <f:string key="@type">MediaObject</f:string>
          <xsl:call-template name="get-title"/>
          <f:array key="url">
            <xsl:for-each select="m:identifier[@displayLabel='iiif'][string()]">
              <xsl:call-template name="find-pages"/>
            </xsl:for-each>
          </f:array>
        </f:map>          
        <xsl:for-each select="m:relatedItem[@type='constituent'][m:identifier[@displayLabel='iiif']]">
          <xsl:call-template name="make_page_field"/>
        </xsl:for-each>
        
      </xsl:when>
      <xsl:otherwise>

        <f:map>
          <f:string key="@type">MediaObject</f:string>
          <xsl:call-template name="get-title"/>
          
          <f:array key="url">
            <xsl:for-each select="m:identifier[contains(.,'.tif')]">
              <xsl:call-template name="find-pages"/>
            </xsl:for-each>
          </f:array>
        </f:map>          

        <xsl:for-each select="m:relatedItem[@type='constituent'][m:identifier[contains(.,'.tif')]]">
          <xsl:call-template name="make_page_field"/>
        </xsl:for-each>

      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>
  
  <xsl:template name="find-pages">
    <xsl:variable name="img">
      <xsl:choose>	 
	<xsl:when test="./@displayLabel='iiif'">
	  <xsl:value-of select="."/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:choose>
	    <xsl:when test="contains(.,'.tif')">
	      <xsl:value-of select="substring-before(.,'.tif')"/>
	    </xsl:when>
	    <xsl:when test="contains(.,'.TIF')">
	      <xsl:value-of select="substring-before(.,'.TIF')"/>
	    </xsl:when>
	    <xsl:when test="contains(.,'.jp2')">
	      <xsl:value-of select="substring-before(.,'.jp2')"/>
	    </xsl:when>
	  </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="string-length($img) &gt; 0">
      <f:string>
	<xsl:choose>
	  <xsl:when test="contains($img,'.json')">
	    <xsl:value-of select="$img"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="concat('https://kb-images.kb.dk/',$img,'/info.json')"/>
	  </xsl:otherwise>
	</xsl:choose>
      </f:string>
    </xsl:if>
  </xsl:template>

  <xsl:template name="get-names">
    <xsl:param name="record_identifier"/>
    <xsl:param name="cataloging_language"/>
    <xsl:param name="agent_type" select="''" />

    <xsl:variable name="inferred_originator_type">
      <xsl:choose>
        <xsl:when test="$agent_type"><xsl:value-of select="$agent_type"/></xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="contains($record_identifier,'pamphlets')">Organization</xsl:when>
            <xsl:otherwise>Person</xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <f:map>
      <f:string key="@type">
        <xsl:choose>
          <xsl:when test="contains(@type,'corporate')">Organization</xsl:when>
          <xsl:otherwise><xsl:value-of select="$inferred_originator_type"/></xsl:otherwise>
        </xsl:choose>
      </f:string>
      <xsl:if test="@authorityURI">
        <f:string key="sameAs"><xsl:value-of select="@authorityURI"/></f:string>
      </xsl:if>
      <xsl:variable name="language"><xsl:value-of select="@xml:lang"/></xsl:variable>
      <f:map key="name">
        <xsl:element name="f:string">
          <xsl:attribute name="key">@language</xsl:attribute>
          <xsl:choose>
            <xsl:when test="$language"><xsl:value-of select="$language"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="$cataloging_language"/></xsl:otherwise>            
          </xsl:choose>
        </xsl:element>
        <xsl:element name="f:string">
          <xsl:attribute name="key">@value</xsl:attribute>
          <xsl:for-each select="m:namePart">
            <xsl:choose>
              <xsl:when test="@type = 'date'"> (<xsl:value-of select="."/>)</xsl:when>
              <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:element>
      </f:map>
      <xsl:if test="t:residence">
        <xsl:element name="f:array">
          <xsl:attribute name="key">
            <xsl:choose>
              <xsl:when test="contains(@type,'corporate')">location</xsl:when>
              <xsl:otherwise>homeLocation</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:for-each select="t:residence/*">
            <f:string><xsl:value-of select="."/></f:string>
          </xsl:for-each>
        </xsl:element>
      </xsl:if>
    </f:map>
  </xsl:template>

  <xsl:template match="f:map">
    <xsl:param name="record_identifier"/>
    <f:map>
      <xsl:apply-templates select="@*"/>
      <xsl:choose>
        <xsl:when test="not(f:string[@key='id'])">
          <!-- f:string key="id">
               <xsl:value-of select="concat($record_identifier,$sep_string,'disposable',$sep_string,'subrecord',$sep_string,generate-id())"/>
               </f:string -->
          <xsl:apply-templates select="*">
            <xsl:with-param name="record_identifier" select="$record_identifier"/>
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise>
          <!-- xsl:apply-templates select="*[not(@key='id')]"/ -->
          <xsl:apply-templates select="*">
            <xsl:with-param name="record_identifier" select="$record_identifier"/>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
    </f:map>
  </xsl:template>

  <xsl:function name="my:escape_stuff">
    <xsl:param name="arg"/>
    <xsl:choose>
      <xsl:when test="contains($arg,'medium')">material</xsl:when>
      <xsl:when test="contains($arg,'extent')">materialExtent<!-- numberOfPages--></xsl:when>
      <xsl:when test="contains($arg,'physicaldescription')">material</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="replace($arg,'\s',$sep_string,'s')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <xsl:template match="*|@*">
    <xsl:param name="record_identifier"/>
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()">
        <xsl:with-param name="record_identifier" select="$record_identifier"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  
  
</xsl:transform>
