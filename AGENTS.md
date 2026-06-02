# AGENTS.md — Java IO utils

## Overview

**java-io-util** is a Java library providing common, well-tested I/O utilities.
It offers type-safe abstractions for parsing and formatting data across multiple formats (text, XML, CSV, HTTP), functional I/O interfaces, and platform-specific helpers.

Key capabilities:
- Type-safe `FileParser`/`FileFormatter` and `TextParser`/`TextFormatter` interfaces for stream-based and character-based I/O
- Functional I/O interfaces (`IOFunction`, `IOSupplier`, `IOConsumer`, `IOPredicate`, …) mirroring `java.util.function` with `IOException` support
- Text parsing/formatting with `Parser`/`Formatter` (null-safe `CharSequence` conversion) and typed `Property` accessors
- XML parsing (SAX, StAX) and formatting with secure defaults
- JAXB marshalling/unmarshalling integration
- CSV reading/writing via [picocsv](https://github.com/nbbrd/picocsv)
- Lightweight HTTP client abstraction over `HttpURLConnection`
- curl-backed `HttpURLConnection` implementation for environments without native HTTP
- Windows-specific process wrappers (registry, PowerShell, CScript, where)
- Licensed under [EUPL](https://joinup.ec.europa.eu/page/eupl-text-11-12); maintained by the [National Bank of Belgium](https://www.nbb.be)

## Architecture

The project is a Maven multi-module build with a layered dependency structure:

### 1. Base (`java-io-base`)

Foundation module with zero external runtime dependencies. All other modules depend on this.
- **I/O interfaces**: `FileParser`, `FileFormatter` (stream-based), `TextParser`, `TextFormatter` (character-based)
- **Functional I/O**: `IOFunction`, `IOSupplier`, `IOConsumer`, `IOPredicate`, `IORunnable`, `IOUnaryOperator`, `IOBiConsumer`
- **Text utilities**: `Parser`, `Formatter` (null-safe `CharSequence` conversion), `Property`, `BooleanProperty`, `IntProperty`, `LongProperty`, `DoubleProperty`
- **Other**: `Resource`, `IOIterator`, `MediaType`, `ProcessReader`, `OS`, `SystemProperties`, `Zip`, `Properties2`, `BlockSizer`

### 2. Format-specific modules

| Module             | Depends on | Purpose                                                                                           |
|--------------------|------------|---------------------------------------------------------------------------------------------------|
| `java-io-xml`      | `base`     | XML parsing/formatting via SAX (`Sax`) and StAX (`Stax`) with secure defaults; `Xml` facade       |
| `java-io-xml-bind` | `xml`      | JAXB marshalling/unmarshalling (`Jaxb`) bridging `javax.xml.bind` to `FileParser`/`FileFormatter` |
| `java-io-picocsv`  | `base`     | CSV parsing/formatting (`Picocsv`) via the picocsv library                                        |

### 3. Network modules

| Module         | Depends on | Purpose                                                                                                                                          |
|----------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `java-io-http` | `base`     | Lightweight HTTP client abstraction: `HttpClient`, `HttpRequest`, `HttpResponse`, `HttpAuthenticator`, `URLQueryBuilder`, `URLConnectionFactory` |
| `java-io-curl` | `base`     | curl-backed `HttpURLConnection` implementation (`CurlHttpURLConnection`) for proxy/TLS scenarios                                                 |

### 4. Platform module

| Module        | Depends on | Purpose                                                                                        |
|---------------|------------|------------------------------------------------------------------------------------------------|
| `java-io-win` | `base`     | Windows-specific wrappers: `RegWrapper`, `PowerShellWrapper`, `CScriptWrapper`, `WhereWrapper` |

### 5. Distribution

| Module        | Purpose                                       |
|---------------|-----------------------------------------------|
| `java-io-bom` | Maven Bill of Materials for version alignment |


## Build & Test

```shell
mvn clean install                 # full build + tests + enforcer checks
mvn clean install -Pyolo          # skip all checks (fast local iteration)
mvn test -pl <module-name> -Pyolo # fast test a single module
mvn test -pl <module-name> -am    # full test a single module
```

- **Java 8 target** with JPMS `module-info.java` compiled separately on JDK 9+ (see `java8-with-jpms` profile in root POM)
- **JUnit 5** with parallel execution enabled (`junit.jupiter.execution.parallel.enabled=true`); **AssertJ** for assertions

## Key Conventions

- **Lombok**: use lombok annotations when possible. Config in `lombok.config`: `addNullAnnotations=jspecify`, `builder.className=Builder`
- **Nullability**: `@org.jspecify.annotations.Nullable` for nullable; `@lombok.NonNull` for non-null parameters. Return types use `@Nullable` or the `OrNull` suffix (e.g., `getThingOrNull`)
- **Design annotations** use annotations from `java-design-util` such as `@VisibleForTesting`, `@StaticFactoryMethod`, `@DirectImpl`, `@MightBeGenerated`, `@MightBePromoted`
- **Internal packages**: `internal.<project>.*` are implementation details; public API lives in the root and `spi` packages
- **Static analysis**: `forbiddenapis` (no `jdk-unsafe`, `jdk-deprecated`, `jdk-internal`, `jdk-non-portable`, `jdk-reflection`), `modernizer`
- **Reproducible builds**: `project.build.outputTimestamp` is set in the root POM
- **Formatting/style**: 
  - Use IntelliJ IDEA default code style for Java
  - Follow existing formatting and match naming conventions exactly
  - Follow the principles of "Effective Java"
  - Follow the principles of "Clean Code"
- **Java/JVM**: 
  - Target version defined in root POM properties; some modules may require higher versions
  - Use modern Java feature compatible with defined version

## Agent behavior

- Do respect existing architecture, coding style, and conventions
- Do prefer minimal, reviewable changes
- Do preserve backward compatibility
- Do not introduce new dependencies without justification
- Do not rewrite large sections for cleanliness
- Do not reformat code
- Do not propose additional features or changes beyond the scope of the task
