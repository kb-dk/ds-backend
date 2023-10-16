/**
 * The transform package is used for transforming a textual input to a textual output.
 * Common use is to transform an XML input to a XML or JSON output, e.g. to deliver JSON-LD from a MODS source.
 *
 * The transform package uses a ServiceLoader to discover and control transformers:
 * New transformers are added by creating a {@link dk.kb.present.transform.DSTransformer} with a corresponding
 * {@link dk.kb.present.transform.DSTransformerFactory} and registering the factory in the file
 * {@code src/main/resources/META-INF/services/dk.kb.present.transform.DSTransformerFactory}.
 *
 * Transformer instances are created by {@link dk.kb.present.transform.TransformerController}.
 *
 * Overview of mapping between fields from preservica.
 * The XSLT Transformers created by the {@link dk.kb.present.transform.XSLTFactory} using XSLTs related to preservica
 * metadata are connected in the following way:
 * <table>
 * 	<caption><b>Mapping between preservica fields</b></caption>
 * 	<thead>
 * 	<tr>
 * 		<th>Preservica OAI-PMH XML</th>
 * 		<th>SolrDoc</th>
 * 		<th>Schema.org JSON</th>
 * 		<th>Comment</th>
 * 	</tr>
 * 	</thead>
 * 	<tbody>
 * 	<tr>
 * 		<td></td>
 * 		<td>id</td>
 * 		<td>id or identifier/RecordID</td>
 * 		<td>This ID is a DS-id, which is not present in preservica and is created from the name of the record in the
 * 	       	preservica OAI-PMH harvest.</td>
 * 	</tr>
 * 	<tr>
 * 		<td></td>
 * 		<td>origin</td>
 * 		<td>identifier/Origin</td>
 * 		<td>Origin is not present in preservica. It is created from where the metadata originates from. In this case,
 * 	        the Radio/TV collection.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreGenre/genre</td>
 * 		<td>categories</td>
 * 		<td>keywords</td>
 * 		<td>All types of genres from preservica are present here.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/<br/>formatLocation</td>
 * 		<td>collection</td>
 * 		<td>isPartOf/Collection</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreGenre/genre</td>
 * 		<td>genre</td>
 * 		<td></td>
 * 		<td>Extracted from the field which contains 'hovedgenre:'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/<br/>formatMediaType</td>
 * 		<td>resource_description</td>
 * 		<td>type</td>
 * 		<td>A description of what the resource is, not what it represents or contains. e.g. video or image.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreTitle/title</td>
 * 		<td>title</td>
 * 		<td>name</td>
 * 		<td>Is extracted when pbcoreTitle/titleType = 'titel'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreTitle/title</td>
 * 		<td>original_title</td>
 * 		<td>alternateName</td>
 * 		<td>Is extracted when pbcoreTitle/titleType = 'originaltitel'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcorePublisher/<br/>publisherRole</td>
 * 		<td>creator_affiliation</td>
 * 		<td>publishedOn/broadcastDisplayName</td>
 * 		<td>The solr field has the same name across collections.</td>
 * 	</tr>
 * 	<tr>
 * 	    <td>pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart</td>
 * 	    <td></td>
 * 	    <td>datePublished</td>
 * 	    <td>Is only extracted when the PBCore document contains the extension 'premiere' with the value premiere,
 * 	        as this field only is to be created if it is the first time this resource has been shown. </td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreDescription</td>
 * 		<td>notes</td>
 * 		<td>Does not exist, the values are present in either 'abstract' or 'description'</td>
 * 		<td>The notes field originates from the mods2solr XSLT and combines different metadata, which didn't have their
 * 	        own field in the MODS standard. Descriptions from preservica are cramped into this field, however, they are
 * 	        also extracted to their own fields 'abstract' for short description and 'description' for longer descriptions</td>
 * 	</tr>
 * 	<tr>
 * 		<td>xip:Manifestation/<br/>ComponentManifestation/<br/>FileRef</td>
 * 		<td>resource_id</td>
 * 		<td>contentUrl</td>
 * 		<td>This id is not directly available in the DeliverableUnit from preservica, but originates from a manifestation.
 * 	       	It is the ID of a presentation manifestation related to the DeliverableUnit. The connection between them is
 * 	       	created in the backing DS-storage. Then the XSLT looks for the child and accesses it through its URI.
 * 	       	Furthermore there is a difference between the solr resource_id and the schema.org contentUrl. The first is
 * 	       	just the ID, while the second is a full URL, to where the content can be accessed.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreTitle/title</td>
 * 		<td>episode_title</td>
 * 		<td>encodesCreativeWork/name</td>
 * 		<td>Is extracted when pbcoreTitle/titleType = 'episodetitel'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreIdentifier</td>
 * 		<td>ritzau_id</td>
 * 		<td>identifier/ritzau_id</td>
 * 		<td>Extracted from pbcoreIdentifier where identifierSource = 'ritzauId'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreIdentifier</td>
 * 		<td>tvmeter_id</td>
 * 		<td>identifier/tvmeter_id</td>
 * 		<td>Extracted from pbcoreIdentifier where identifierSource = 'tvmeterId'</td>
 * 	</tr>
 * 	<tr>
 * 	    <td>pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart</td>
 * 	    <td></td>
 * 	    <td>startTime</td>
 * 	    <td></td>
 * 	</tr>
 * 	<tr>
 * 	    <td>pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd</td>
 * 	    <td></td>
 * 	    <td>endTime</td>
 * 	    <td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/formatDuration <br/> or calculated from <br/>
 * 	       	pbcoreInstantiation/pbcoreDateAvailable/dateAvailableStart and
 * 	       	pbcoreInstantiation/pbcoreDateAvailable/dateAvailableEnd</td>
 * 		<td>duration_ms</td>
 * 		<td>duration</td>
 * 		<td>Resources from preservica can contain the duration in miliseconds, however not all resources does. Therefore,
 * 	        the XSLT looks for this field and if it isn't present, the duration is calculated from the available fields.
 * 	        Time is UTC.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/formatColors</td>
 * 		<td>color</td>
 * 		<td></td>
 * 		<td>Boolean value for colors. Resource in colors = true,  resource in greytones = false.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/formatStandard</td>
 * 		<td>video_quality</td>
 * 		<td>videoQuality</td>
 * 		<td>Quality of the video. Preservica can contain values 'hd' and 'ikke hd'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/formatChannelConfiguration</td>
 * 		<td>surround_sound</td>
 * 		<td>kb:internal/kb:surround_sound</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>premiere</td>
 * 		<td></td>
 * 		<td>Extracted when the extension field starts with 'premiere:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/formatAspectRatio</td>
 * 		<td>aspect_ratio</td>
 * 		<td>kb:internal/kb:aspect_ratio</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>xip:Manifestation/ComponentManifestation/ComponentType</td>
 * 		<td>manifestation_type</td>
 * 		<td>not present and not needed as the field 'type' describes what type of resource is in hand.</td>
 * 		<td>Is extracted from a presentation manifestation related to the DeliverableUnit.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>episode</td>
 * 		<td>encodesCreativeWork/episodeNumber</td>
 * 		<td>Extracted when the extension field starts with 'episodenr:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>number_of_episodes</td>
 * 		<td>encodesCreativeWork/partOfSeason/numberOfEpisodes</td>
 * 		<td>Extracted when the extension field starts with 'antalepisoder:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>live_broadcast</td>
 * 		<td>isLiveBroadcast</td>
 * 		<td>Extracted when the extension field starts with 'live:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>retransmission</td>
 * 		<td>kb:internal/kb:retransmission</td>
 * 		<td>Extracted when the extension field starts with 'genudsendelse:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreDescription</td>
 * 		<td>abstract</td>
 * 		<td>abstract</td>
 * 		<td>Is extracted when ./descriptionType = 'kortomtale'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreDescription</td>
 * 		<td>description</td>
 * 		<td>description</td>
 * 		<td>is extracted when ./descriptionType = 'langomtale1' </td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreGenre/genre</td>
 * 		<td>genre_sub</td>
 * 		<td></td>
 * 		<td>Extracted when pbcoreGenre/genre contains undergenre:</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>has_subtitles</td>
 * 		<td>kb:internal/kb:has_subtitles</td>
 * 		<td>Extracted when the extension field starts with 'tekstet:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>has_subtitles_for_hearing_impaired</td>
 * 		<td>kb:internal/kb:has_subtitles_for_hearing_impaired</td>
 * 		<td>Extracted when the extension field starts with 'th:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pidhandle:pidhandle/handle</td>
 * 		<td>pid</td>
 * 		<td>identifier/PID</td>
 * 		<td>A long lasting reference to the resource.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>xip:DeliverableUnit/AccessionRef</td>
 * 		<td>internal_accession_ref</td>
 * 		<td>identifier/InternalAccessionRef</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>"pbcoreInstantiation/pbcoreFormatID</td>
 * 		<td>internal_format_identifier_ritzau</td>
 * 		<td>kb:internal/kb:format_identifier_ritzau</td>
 * 		<td>Is extracted when ./formatIdentifierSource = 'ritzau'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreInstantiation/pbcoreFormatID</td>
 * 		<td>internal_format_identifier_nielsen</td>
 * 		<td>kb:internal/kb:format_identifier_nielsen</td>
 * 		<td>Is extracted when ./formatIdentifierSource = 'nielsen'</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_maingenre_id</td>
 * 		<td>kb:internal/kb:maingenre_id</td>
 * 		<td>Extracted when the extension field starts with 'hovedgenre_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_channel_id</td>
 * 		<td>kb:internal/kb:channel_id</td>
 * 		<td>Extracted when the extension field starts with 'kanalid:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_country_of_origin_id</td>
 * 		<td>kb:internal/kb:country_of_origin_id</td>
 * 		<td>Extracted when the extension field starts with 'produktionsland_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_ritzau_program_id</td>
 * 		<td>kb:internal/kb:ritzau_program_id</td>
 * 		<td>Extracted when the extension field starts with 'program_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_subgenre_id</td>
 * 		<td>kb:internal/kb:subgenre_od</td>
 * 		<td>Extracted when the extension field starts with 'undergenre_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_episode_id</td>
 * 		<td>kb:internal/kb:episode_id</td>
 * 		<td>Extracted when the extension field starts with 'afsnit_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_season_id</td>
 * 		<td>kb:internal/kb:season_id</td>
 * 		<td>Extracted when the extension field starts with 'saeson_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_series_id</td>
 * 		<td>kb:internal/kb:series_id</td>
 * 		<td>Extracted when the extension field starts with 'serie_id:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_program_ophold</td>
 * 		<td>kb:internal/kb:program_ophold</td>
 * 		<td>Extracted when the extension field starts with 'program_indhold:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_is_teletext</td>
 * 		<td>kb:internal/kb:is_teletext</td>
 * 		<td>Extracted when the extension field starts with 'ttv:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>pbcoreExtension/extension</td>
 * 		<td>internal_showviewcode</td>
 * 		<td>kb:internal/kb:showviewcode</td>
 * 		<td>Extracted when the extension field starts with 'showviewcode:'.</td>
 * 	</tr>
 * 	<tr>
 * 		<td>padding:padding/paddingSeconds</td>
 * 		<td>internal_padding_seconds</td>
 * 		<td>kb:internal/kb:padding_seconds</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/access:access/individuelt_forbud</td>
 * 		<td>internal_access_individual_prohibition</td>
 * 		<td>kb:internal/kb:access_individual_prohibition</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/access:access/klausuleret</td>
 * 		<td>internal_access_claused</td>
 * 		<td>kb:internal/kb:acess_claused</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/access:access/defekt</td>
 * 		<td>internal_access_malfunction</td>
 * 		<td>kb:internal/kb:access_malfunction</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/access:access/kommentarer</td>
 * 		<td>internal_access_comments</td>
 * 		<td>kb:internal/kb:access_comments</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/program_structure:program_structure/missingStart/missingSeconds</td>
 * 		<td>internal_program_structure_missing_seconds_start</td>
 * 		<td>kb:internal/kb:program_structure_missing_seconds_start</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/program_structure:program_structure/missingEnd/missingSeconds</td>
 * 		<td>internal_program_structure_missing_seconds_end</td>
 * 		<td>kb:internal/kb:program_structure_missing_seconds_end</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/program_structure:program_structure/holes</td>
 * 		<td>internal_program_structure_holes</td>
 * 		<td>kb:internal/kb:program_structure_holes</td>
 * 		<td></td>
 * 	</tr>
 * 	<tr>
 * 		<td>Metadata/program_structure:program_structure/overlaps</td>
 * 		<td>internal_program_structure_holes</td>
 * 		<td>kb:internal/kb:program_structure_overlaps</td>
 * 		<td></td>
 * 	</tr>
 * 	<tbody>
 * </table>
 */
package dk.kb.present.transform;