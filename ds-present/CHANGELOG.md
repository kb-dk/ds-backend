# Changelog
All notable changes to ds-present will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
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
