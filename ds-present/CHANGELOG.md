# Changelog
All notable changes to ds-present will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Changed how the solr field creator_affiliation_facet is constructed.
- Elevated some fields in XSLTs from the kb:internal map to proper schema.org values and non internal solr fields.
- Bumped solr-config to v. 1.6.10
- Updated XSLTs to handle edge cases, where multiple 'hovedgenre' are present in data. If multiple are present, the shortest string is used as genre in the schema.org JSON.

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
