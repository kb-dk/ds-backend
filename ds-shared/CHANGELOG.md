# Changelog

All notable changes to ds-shared will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- `httpCallWithOAuthToken`, `httpCallWithOAuthTokenAsDtoList` and `getHttpURLConnection` serialization now support
  `JavaTimeModule` where `WRITE_DATES_AS_TIMESTAMPS` is disabled, so `Java Time` classes don't get converted to longs.
- Explicit handle `NotFoundServiceException` in `handleException` method, so it logs on debug level instead and keeps
  its `HTTP` code 404.
- Explicit handle `NotFoundServiceException` in `httpCallWithOAuthToken` and `httpCallWithOAuthTokenAsDtoList` methods,
  so it logs on debug level instead.
- `mapServiceException` method now give the error response body when returning Exceptions, so the exception message is
  not lost when calling from one service to another.

## [7.0.0](https://github.com/kb-dk/ds-backend/releases/tag/v7.0.0) - 2026-09-28

## [6.0.0](https://github.com/kb-dk/ds-backend/releases/tag/v6.0.0) - 2026-08-19

### Changed

- Removed kb-util dependency and moved classes to this ds-shared module
- Added `description` and `name` in `pom.xml`.
- Formatted `pom.xml`.