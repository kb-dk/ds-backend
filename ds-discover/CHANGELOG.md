# Changelog
All notable changes to ds-discover will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added 
- Support for dynamically updating values in OpenAPI spec through internal JIRA issue [DRA-139](https://kb-dk.atlassian.net/browse/DRA-139). 
- spellcheck.maxCollationRetries=10 is injected per default for Solr /select [DRA-319](https://kb-dk.atlassian.net/browse/DRA-319)

### Fixed
- Switch from Jersey to Apache URI Builder to handle parameters containing '{' [DRA-338](https://kb-dk.atlassian.net/browse/DRA-338)

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
