# Changelog
All notable changes to ds-present will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
### Changed
### Fixed


## [2.2.8](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.8) 2025-03-05

### Added
- ds-present-transformation-errors log which logs only transformation errors.
- Added injection of Oauth token on all service methods when using DsPresentClient

### Changed
- Bumped multiple OpenAPI dependency versions
- Changed the response format for the JSON response in /records to include information on failed transformations.
- Bumped kb-util to v1.6.9 for service2service oauth support.
- IIIF metods not supported by service2service Oauth since no java client is generated for these methods. Also not intended to be called by other modules.
- Using new DsLicense and DsStorage client

### Fixed
- Fixed /api-docs wrongly showing petstore example API spec
- Fixed genre unittests failing wrongly


## [2.2.6](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.6) 2025-01-27
### Changed
- Bumped SwaggerUI dependency to v5.18.2

### Fixed
- Fixed inclusion of the same dependencies from multiple sources.
- Fixed that not all records did get a fallback genre.
- Solr 1.7.9: Fixed field genre_facet not being added as a facet field.


## [2.2.5](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.5) 2025-01-06
### Added 
- Added functionality to the solr cloud script `cloud_ds.sh` 
  Six collections prefixed with `ds` are kept.
- Upgraded dependency cxf-rt-transports-http to v.3.6.4 (fix memory leak)


### Fixed
- Make HoldbackDatePicker.getHoldbackDateForRecord threadsafe

## [2.2.4](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.4) 2024-11-29
### Changed 
- Split genre `Rodekassen` to `TV-rodekasse` and `Radio-rodekasse`.

## [2.2.3](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.3) 2024-11-28
### Fixed
- Fixed an issue where holdback name for records with the holdback value for "Undervisning" did not get populated correctly.

## [2.2.2](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.2) 2024-11-22
### Changed:
- Changed which method present uses to call storage. Effectively cutting SQL calls to DS-Storage database in half.


## [2.2.1](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.1) 2024-11-21
### Added: 
- Added functionality to change own production threshold in configuration files, with a default value which includes own-, co- and enterprise-production for DR records.

### Changed
- Renamed solr fields `own_production` and `own_production_code` to `production_code_allowed` and `production_code_value`. 

## [2.2.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.2.0) 2024-11-12
### Added:
- Functionality to look up if a record has been restricted by DR by production ID.  
- Added field own_production to all radio records, so that all records have a value for this field.
- Solr 1.7.7: Add boolean field to show if record is restricted by DR.
- Add boolean fields related to which kind of extra metadata that have been added to the records after OAI harvest to the transformations. These fields are also present in solr 
  schema version 1.7.7
- Add boolean field `has_kaltura_id`.

### Changed:
- Changed configuration naming for YAML key `holdback.dr` to `dr.holdback` in preparation for addition of `dr.restrictions`.
- Moved unresolved genres to the genre `Rodekassen`.

## [2.1.6](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.6) 2024-11-05
### Added
- Added a more thorough parsing of categories into pretty genres.

### Changed 
- solr 1.7.6: Changes related to search ranking.
- make all loggers static

### Removed
- Removed Schema.org field `conditionsOfAccess` and solr field `conditions_of_access` as this field was never used and the DR rights management couldn't be done in a single value 
  field.

### Fixed
- In the prior version all DOMS migrated records were set as malfunctioning. This is fixed in this release.

## [2.1.5](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.5) 2024-10-23
### Added
- Added check for DOMS records having a valid access representation.
- Added client method for solr schema transformation
- solr 1.7.5: Added field genre_facet to solr schema

### Changed
- Updated genre mappings with more values.
- solr 1.7.4 : New field type as text_general but without stopwords. The title field now uses this field instead. It needed for better title match.
- solr 1.7.5: Update configuration of suggest component to filter on own_production status

### Fixed
- Fixed categories not transforming correct, when they contained a `:`-character.
- Fixed handling of `episodenr: 2+3` where a proper digit is expected. 
- Fixed cases where transformation errors wasn't handled correct. Now all errors gets written to the transformed documents.

## [2.1.4](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.4) 2024-10-08
### Added 
- solr maxBooleanClauses increased from 1024 til 8192. This is because licensmodule queries can have several 100 terms. (solr.config=1.7.2)
  For this to also work on a solr cloud setup the solr start script must have:  export SOLR_OPTS="-Dsolr.allowPaths=/solr-c-backup -Dsolr.max.booleanClauses=8192

### Fixed 
- Fixed mime-type for JSON responses to `/records`-endpoint.
- Fixed download of solr in solr setup scripts.

### Changed
- Renamed all fields named `internal_acces_...` to `access_...` as they are to be used in ds-license.

## [2.1.3](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.3) 2024-09-30
### Added 
- Added normalization of values in fields representing DR production IDs.


### Changed
- Changed field used for ownproduction calculation from country_of_origin to origin as specified by DR metadata specialists.
- Check for valid numbers in preservica2schemaorg transformation
- Rename field `migrated_from` to `originates_from` and add preservica as default value.

### Fixed
- Some documents did not get the channel facet correctly created. A check for these DR1 and DR2 records have been added in the preservica2schemaorg XSLT.

## [2.1.2](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.2) 2024-09-17
### Added
- Added field `dr_production_id` to transformations and solr schema.
- Added the query used to build frontpage of the webapp to warmup of solr searchers. Lowering Qtime from approx 300 ms to 50 ms on a local setup.

## [2.1.1](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.1) 2024-09-10
### Changed
- Bumped ds-storage and ds-license dependency.

## [2.1.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.1.0) 2024-09-10
### Added
- Added string normalization of title fields to XSLTs.
- Added validation of ':' in datetime timezone values in XSLT transformations.
- Added field migrated_from to documents.
- Added fields own_production and own_production_code to documents.
- Added _count fields to count entries in all multiValued solr fields for radio and tv.
- Added _length fields to count the length of content in all analysed solr text fields for radio and tv.
- Added metadata from gallup tvmeter as input for holdback calculation.
- Added more synonyms to dr_synonyms.txt. This require a new deploy to solr (and version bump)

- Enabled OAuth2 on module. Much is copy-paste from ds-image to see it working in two different modules.  Plans are to refactor common functionality out into kb-util/
Only method getRecord (show full-record called by frontend) has OAuth enabled

### Changed
- Bumped solr version to 1.6.15.
- Changed extraction of field videoFrameSize to clean values as '16:9,' and '16:9, ' to '16:9'.
- Changed how values are extracted from Preservica records. Now all needed values are extracted at once, saving iterations on the XML stream.
- Changed logic in holdback filter, when values form=7000 or purpose=6000 are met.
- Changed values used in solr field creator_affiliation_facet and cleaned the generic channel names.

### Removed
- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of the URL by bumping kb util to v1.5.10

### Fixed
- Transformation of genres starting with _ are now corrected to not contain the underscore.


## [2.0.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-2.0.0) 2024-07-17
### Added
- Added support for Preservica records migrated from DOMS.

### Changed
- Changed how the solr field creator_affiliation_facet is constructed.
- Elevated some fields in XSLTs from the kb:internal map to proper schema.org values and non internal solr fields.
- Bumped solr-config to v. 1.6.12
- Updated XSLTs to handle edge cases, where multiple 'hovedgenre' are present in data. If multiple are present, the shortest string is used as genre in the schema.org JSON.
- Introduce kaltura_id in solr documents and solr schema
- Introduce KalturaID as identifier in schema.org JSON
- Updated solr tests, to use Preservica 7 setup.
- Update logging in HoldbackDatePicker class making it clearer, that origins in a holdback context comes from TVMeter data.
- Updated XSLT to strip namespace prefixes from PbcoreDescriptionDocuments, to make them alike for all records
- Bumped storage dependency to 2.1.0

### Removed
- Removed all traces of streaming_url and contentUrl for video and audio records.




## [1.9.3](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.9.3) 2024-07-03
### Changed
- Changed how duplicate metadata fragments are handled in XSLT transformers.

## [1.9.2](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.9.2) 2024-07-01
### Changed
- Bumped solr-config to v.1.6.8


### Changed
- Update dependency ds-storage to version 2.0.0
- Update dependency ds-license to version 1.4.2

## [1.9.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.9.0) 2024-07-01

## Changed
- Bumped kb-util version to improve YAML logging.

## [1.8.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.8.0)
### Added
- Added XSLT support for parsing Preservica and DOMS records through the same transformation.

### Changed
- Changed a lot of transformation tests to use preservica 7 data instead of preservica 5.  

### Removed
- Removed XSLT support for Preservica 5 records.


## [1.7.6](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.6)
### Added
- Added solr configuration to distribution tar

### Fixed
- Fixed assembly descriptor to include solr configuration in distribution tar.


## [1.7.5](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.5)
## Added 
- Added solr field: temporal_start_month
- Added View strategy for Preservica 7, so that manifestations can be extracted correctly for Preservica 5 and 7. [DRA-662](https://kb-dk.atlassian.net/browse/DRA-662)
- Added solr field: temporal_start_hour_da
- Added a needed logback dependency.

## Changed
- Changed field type for solr field: temporal_start_year


## [1.7.4](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.4) - 2024-05-13
## Added 
- Added profiles in POM to control testing

## Changed
- Changed release repository

## [1.7.3](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.3) - 2024-04-26

### Added
- Support for dynamically updating values in OpenAPI spec through internal JIRA issue [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139)
- Added sample config files and documentation to distribution tar archive.
- Added distribution zip of solr configset
- Added solr fields containing start and end dates as strings in Danish local time. [DRA-599](https://kb-dk.atlassian.net/browse/DRA-599)

### Changed
- Switch configuration style to camelCase [DRA-431](https://kb-dk.atlassian.net/browse/DRA-431)
- Updated Solr schema version to 1.6.4

### Removed
- Removed spellcheck.maxCollationTries from spellcheck component. It crashed Solr on core reload (after configuration changes)

### Fixed
- Correct resolving of maven build time in project properties.
- Correct resolving of URL in DsPresentClient

## [1.7.2](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.2) - 2024-03-01

### Added
- Support for transforming raw solr schemas with comments in processing instructions to HTML and Markdown.

## [1.7.1](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.1) - 2024-03-08

### Changed
- Bumped ds-storage client to v.1.18 since changes was not backwards compatible.


## [1.7.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.7.0) - 2024-03-05
### Added
-  New synonyms.txt file to solr configuruation. text_general fieldtype will not also use synonyms
-  Bump sbforge-parent to V25
-  Integration unittest uses aegis configuration
### Changed
-  Spellcheck default enabled in edismax selecthandler in solrconfig.xml
### Removed
- Removed origin as enum in openAPI specification and interface.

## [1.6.0](https://github.com/kb-dk/ds-present/releases/tag/ds-present-1.6.0) - 2024-02-09
### Added
- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag
- Added support for referencing config values in OpenAPI specification through the maven plugin: _injection-maven-plugin_
- Added the Paging-Record-Count header streaming of multiple results.
- Spellcheck compontent added to Solr search. If no results are found, the solr reply will come with query suggestions that will give results.

## [1.5.0](https://github.com/kb-dk/ds-present/releases/tag/v1.5.0) - 2024-01-22
(Updated with all changes since 1.3)

### Added
- Added ReflectUtil class containing methods used for testing. Using these methods instead of internal mockito methods makes the project not depend on mockitos old internal class.
- Add mTime from ds-storage to solr documents
- Add support for XSLT transformation of Preservica records from preservica 6.
- Updated OpenAPI YAML with new example values
- Add solr fields for temporal searching.  

### Changed
- logback template changes

### Fixed
- Configuration update of existing collection now works: bin/cloud_ds.sh update
 

## [1.2.2](https://github.com/kb-dk/ds-present/releases/tag/v1.2.2) - 2023-12-12
### Added
- Preliminary support for token based user group resolving, preparing for OAuth2


## [1.2.1](https://github.com/kb-dk/ds-present/releases/tag/v1.2.1) - 2023-12-11
### Changed
- Enum types define for presentations-formats (FormatDTO)


## [1.2.0](https://github.com/kb-dk/ds-present/releases/tag/v1.2.0) - 2023-12-05
### Changed 
- General style of YAML configuration files, by removing the first level of indentation.


## [1.1.0] - YYYY-MM-DD
- Client for the service, to be used by external projects
- Client upgraded to use kb-util streaming framework and calling ds-storage using streaming client also.


## [1.0.0] - YYYY-MM-DD
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/ds-present/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-present/releases/tag/v1.0.0)
