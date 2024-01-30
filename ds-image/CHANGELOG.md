# Changelog
All notable changes to ds-image will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
-  Accept header transfered to image server request
-  Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag

### Changed

- A IIIF or IIIP  call will determine it the image call is a thumbnail or fullsize call. If height/width is over a given limit it
will be classified a fullsize. Call to the licence module for access will then use "Thumbnails" or "Fullsize" as presentationtype for the call.
So it will be possible only to allow thumbnail calls etc. This implementation is very conservative and will determine thumbnail also if most non size-parameters are defined.  It is better to be conservative and later loosen up than giving too much control over thumbnail extraction.

- new properties to defines maximum size of height/width that defines thumbnail limit.

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
