# Changelog
All notable changes to ds-license will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag

 ### Fixed 



## [1.4.0](https://github.com/kb-dk/ds-license/releases/tag/ds-license-1.4.0) 2024-01-11
### Changed
- logback template changes

## [1.3.1]((https://github.com/kb-dk/ds-license/releases/tag/v1.0.0)) 2024-01-11
- Update exception throwing and catching throughout the service.
- Client jar not longer has logback.xml included. Using maven jar-plugin instead that can handle exclusions.
- Added service endpoints to webapp, which makes the service endpoints reachable.


## [1.3.0]((https://github.com/kb-dk/ds-license/releases/tag/v1.0.0)) 2023-12-05
### Changed
- General style of YAML configuration files, by removing the first level of indentation.


## [1.2.0] - 2023-11-30
### Added
- Ds-license client
- Option for caching to prevent DOS from image zooming calls.

### Changed
- Solr calls changed from GET to POST 


## [1.0.0] - YYYY-MM-DD
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/ds-license/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-license/releases/tag/v1.0.0)
