# Changelog
All notable changes to bff will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]
- enable messages to be an object

## [1.2.2]
### Changed
- make all loggers static

## [1.2.1]
###
- make SecretKey for decrypting singleton for performance

## [1.2.0]
### Added
- added messaging service to BFF

## [1.1.0] - 2024-09-12
### Changed
- Updated KB util version from 1.4.22 to 1.5.11.
- Send Unauthorized on missing, expired cookies and if secret salt has changed
- Proxy error responses from proxied url's in stead of return status 502

### Removed
- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of URLs by bumping kb util to v1.5.10

## [1.0.1] - 2024-05-13
### Added
- Added sample config files and documentation to distribution tar archive. [DRA-441](https://kb-dk.atlassian.net/browse/DRA-441)
- Added POM profiles for handling test levels

### Changed
- Change configuration style to camelCase [DRA-431](https://kb-dk.atlassian.net/browse/DRA-431)
- Changed parent POM for release to internal nexus

### Fixed
- Correct resolving of maven build time in project properties. [DRA-441](https://kb-dk.atlassian.net/browse/DRA-441)



## [1.0.0] - YYYY-MM-DD
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/bff/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/bff/releases/tag/v1.0.0)
