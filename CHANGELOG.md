# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Add Maven BOM
- Add `DoubleProperty`
- Add URI parser and formatter
- Add `TextResource` as reader/writer utilities
- Add bridge from `Text*` to `File*` utilities

### Changed

### Fixed

## [0.0.15] - 2021-10-01

### Changed

- **Breaking change** : move Jaxb to its own module

## [0.0.14] - 2021-07-08

### Fixed

- Fix closed inputstream on Stax stream flow

## [0.0.13] - 2021-07-07

### Added

- Add property API
- Add enum and failsafe parser/formatter

### Fixed

- Fix raw use of parameterized class

## [0.0.12] - 2021-03-19

### Changed

- Migration to Maven-Central
- Maven groupId is now `com.github.nbbrd.java-io-util`

## [0.0.11] - 2021-01-25

### Added

- Add support of error stream in ProcessReader
- Add URL parser and formatter

## [0.0.10] - 2021-01-18

### Added

- Add system utils

## [0.0.9] - 2021-01-12

### Added

- Add char buffer utils

## [0.0.8] - 2020-10-30

### Added

- Add file parser/formatter
- Add text parser/formatter

### Fixed

- Fix XML encoding

## [0.0.7] - 2020-08-25

### Added

- Add lenient parsing of Locale

### Fixed

- Fix transitive requirement of nbbrd.io.xml module

## [0.0.6] - 2020-02-26

### Added

- Add char parser/formatter API

## [0.0.5] - 2020-02-17

### Added

- Add IOIterator
- Add IOUnaryOperator
- Add Resource#process(URI, IOConsumer<Path)

### Changed

- Big refactoring to split project

### Removed

- Remove Stream utilities

## [0.0.4] - 2019-08-13

### Added

- Add support of JPMS

## [0.0.3] - 2019-05-29

### Added

- Add formatting in XML API
- Add parsing of class loader resources in XML API
- Add withers alongside builders in XML API
- Add composing functions in XML API

### Changed

- Improve reporting of null pointer exceptions in XML API
- Improve reporting of missing resources in XML API

## [0.0.2] - 2018-11-27

### Changed

- Improve XXE desambiguation
- Improve XML error reporting

### Fixed

- Fix JDK11 cleanup

## [0.0.1] - 2018-03-01

### Added

- Initial release

[Unreleased]: https://github.com/nbbrd/java-io-util/compare/v0.0.15...HEAD
[0.0.15]: https://github.com/nbbrd/java-io-util/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/nbbrd/java-io-util/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/nbbrd/java-io-util/compare/v0.0.12...v0.0.13
[0.0.12]: https://github.com/nbbrd/java-io-util/compare/v0.0.11...v0.0.12
[0.0.11]: https://github.com/nbbrd/java-io-util/compare/v0.0.10...v0.0.11
[0.0.10]: https://github.com/nbbrd/java-io-util/compare/v0.0.9...v0.0.10
[0.0.9]: https://github.com/nbbrd/java-io-util/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/nbbrd/java-io-util/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/nbbrd/java-io-util/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/nbbrd/java-io-util/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/nbbrd/java-io-util/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/nbbrd/java-io-util/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/nbbrd/java-io-util/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/nbbrd/java-io-util/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/nbbrd/java-io-util/releases/tag/v0.0.1
