# Changelog
All notable changes to ds-image will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Kaltura thumbnail API extended to two allow seeking between startSeconds and endSeconds into the stream. Default behaviour unchanged.
- Added injection of Oauth token on all service methods when using DsImageClient. But no methods  are exposed in the client.
- Bumped kb-util to v1.6.9 for service2service oauth support.
- Removed auto generated DsImageClient class that was a blocker for better exception handling. All DsImageClient methods now only throws ServiceException mapped to HTTP status in same way calling the API directly.


### Changed
- Bumped SwaggerUI dependency to v5.18.2
- Bumped multiple OpenAPI dependency versions

### Fixed
- Fixed /api-docs wrongly showing petstore example API spec
- Fixed that placeholder images for non-existing and not-allowed to view images are shown again. 

### Changed

### Fixed
- Fixed inclusion of the same dependencies from multiple sources.

## [1.7.1](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.7.1) - 2025-01-07
### Changed
- Changed http error code, when thumbnails cant be found at Kaltura from 400 to 404.
- Upgraded dependency cxf-rt-transports-http to v.3.6.4 (fix memory leak)
- Upgraded ds-kaltura to v.1.2.6


## [1.7.0](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.7.0) - 2024-10-15
### Changed
- Upgraded ds-kaltura to v.1.2.5
- Kaltura client uses kaltura app-tokens instead of mastersecret. Two new properties added: kaltura.token and katura.tokenId. Mastersecret can be set to null in property


## [1.6.0](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.6.0) - 2024-09-10
### Changed
-  Upgrade to latest Oauth2 classes

### Removed
- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of the URL by bumping kb util to v1.5.10



## [1.5.0](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.5.0) - 2024-07-18
### Changed
- Using ds-kaltura client v.1.2.3 instead of custom kaltura client implementation. 

### Removed
-  Kaltura integration test since this is already done in the ds-kaltura module.


## [1.4.1](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.4.1) - 2024-07-01
- Update dependency ds-license to version 1.4.2

## [1.4.0](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.4.0) - 2024-07-01
### Changed
- Bumped KB-util version

## [1.3.2](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.3.2) - 2024-05-14
### Added
- Support for dynamically updating values in OpenAPI spec through internal JIRA issue [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139).
- Added sample config files and documentation to distribution tar archive. [DRA-414](https://kb-dk.atlassian.net/browse/DRA-414)
- Exception handling for unexpected response for Kaltura for id lookup.

### Changed
- bump sbforge-parent to v25
- Change configuration style to camelCase [DRA-431](https://kb-dk.atlassian.net/browse/DRA-431)
- Changed parent POM

### Fixed
- Switch from Jersey to Handy URI Templates to handle parameters containing '{' and for cleaner code [DRA-338](https://kb-dk.atlassian.net/browse/DRA-338)
- Correct resolving of maven build time in project properties. [DRA-414](https://kb-dk.atlassian.net/browse/DRA-414)

=======
## [1.3.1](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.3.1) - 2024-02-23
### Changed

- A IIIF or IIIP  call will determine it the image call is a thumbnail or fullsize call. If height/width is over a given limit it
will be classified a fullsize. Call to the licence module for access will then use "Thumbnails" or "Fullsize" as presentationtype for the call.
So it will be possible only to allow thumbnail calls etc. This implementation is very conservative and will determine thumbnail also if most non size-parameters are defined.  It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.

- new properties to defines maximum size of height/width that defines thumbnail limit.
- Changed port to 9077 to better fit the selection of the other ds services.

### Added
-  Accept header transfered to image server request
-  Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag
-  Method to get thumbnail links from Kaltura using our external referenceId as identifier. Kaltura thumbnail API only supports using internal Kaltura identifier.



=======
## [1.3.0](https://github.com/kb-dk/ds-image/releases/tag/ds-image-1.3.0) - 2024-01-22
### Changed 
- logback template changes


## [1.1.0](https://github.com/kb-dk/ds-image/releases/tag/v1.1.0) - 2023-12-05
### Added
- Client for the service, to be used by external projects

### Changed 
- General style of YAML configuration files, by removing the first level of indentation.


## [1.0.0] - 2022-08-16
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/ds-image/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-image/releases/tag/v1.0.0)
