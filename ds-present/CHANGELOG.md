# Changelog
All notable changes to ds-present will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added [1.4.0]
- Add solr fields for temporal searching. 

### Added [1.3.0]
- Added ReflectUtil class containing methods used for testing. Using these methods instead of internal mockito methods makes the project not depend on mockitos old internal class.
- Add mTime from ds-storage to solr documents
- Add support for XSLT transformation of Preservica records from preservica 6.

### Changed [1.3.0]
- Updated OpenAPI YAML with new example values



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
