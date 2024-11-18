# Changelog
All notable changes to ds-discover will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added 
- Added filtering to suggest component, to make it only suggest documents, that the caller are able to query for.

## [1.4.1](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.4.1) - 2024-10-15
### Added
- The SolrService class now delivers the correct error message and status code from solr, when the backing solr throws an HTTP code outside the range 200-299.


## [1.4.0](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.4.0) - 2024-09-10
### Added
- Enabled OAuth2 on /select (solrSearch) endpoint. Much is copy-paste from ds-image to see it working in two different modules.
Plans are to refactor common functionality out into kb-util/template projects.

### Changed
- TimeMap moved to kb-util

### Removed
- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of the URL by bumping kb util to v1.5.10


## [1.3.2](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.3.2) - 2024-07-17

### Added
- Allow sort parameters to solr

### Changed
- Ds-present dependency bumb to v2.0.0

## [1.3.1](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.3.1) - 2024-07-01
### Changed
- Update dependency ds-license to version 1.4.2
- Update dependency ds-present to version 1.9.0

## [1.3.0](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.3.0) - 2024-07-01
### Changed
- Bumped kb-util version

## [1.2.5](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.5) - 2024-06-18
### Added
- Yaml configuration updated with list of additional allowed Solr query parameters (solr.extraAllowedParameters). SolrShield will also allow these parameters can be returned from Solr.


## [1.2.4](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.4) - 2024-06-12
### Changed
- Changed logging dependencies

## [1.2.3](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.3) - 2024-05-14
### Added 
- Support for dynamically updating values in OpenAPI spec through internal JIRA issue [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139). 
- spellcheck.maxCollationRetries=10 is injected per default for Solr /select [DRA-319](https://kb-dk.atlassian.net/browse/DRA-319)
- Added sample config files and documentation to distribution tar archive. [DRA-413](https://kb-dk.atlassian.net/browse/DRA-413)

## Changed
- Switch from snake_case and nospace to camelCase [DRA-431](https://kb-dk.atlassian.net/browse/DRA-431)
- Changed default param values for /select endpoint to match default solr configuration. [DRA-564](https://kb-dk.atlassian.net/browse/DRA-564)
- Changed parent POM [DRA-590](https://kb-dk.atlassian.net/browse/DRA-592)

### Fixed
- Switch from Jersey to Apache URI Builder to handle parameters containing '{' [DRA-338](https://kb-dk.atlassian.net/browse/DRA-338)
- Correct resolving of maven build time in project properties. [DRA-413](https://kb-dk.atlassian.net/browse/DRA-413)

## [1.2.2](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.2) - 2024-03-11
### Added
- SolrShield implemented for basic search + facet. Currently set to log the result instead of potentially rejecting calls to select/
- Support for transforming raw solr schemas with comments in processing instructions to HTML and Markdown.


## [1.2.1](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.1) - 2024-02-02
- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag
- Added spellcheck parameters to /select method method when querying Solr


## [1.2.0](https://github.com/kb-dk/ds-discover/releases/tag/ds-discover-1.2.0) - 2024-01-22
### Added
- Client for the service, to be used by external projects
- Solr Suggest API method

### Changed
- logback template changes

## [1.1.0](https://github.com/kb-dk/ds-discover/releases/tag/v1.1.0) - 2023-12-05


## [1.0.0] - 2022-05-16
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/ds-discover/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-discover/releases/tag/v1.0.0)
