# Common I/O utilities for Java 

[![Download](https://img.shields.io/github/release/nbbrd/java-io-util.svg)](https://github.com/nbbrd/java-io-util/releases/latest)
[![Changes](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fnbbrd%2Fjava-io-util%2Fbadges%2Funreleased-changes.json)](https://github.com/nbbrd/java-io-util/blob/develop/CHANGELOG.md)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/com/github/nbbrd/java-io-util/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/com/github/nbbrd/java-io-util/README.md)

This library contains common code used for I/O operations in Java.  
While not being rocket science, its purpose is to be useful, well documented and well tested.

## Dependency graph

```mermaid
flowchart BT

    x-jaxb{{javax.xml.bind:jaxb-api}}
    x-picocsv{{<a href='https://github.com/nbbrd/picocsv'>com.github.nbbrd.picocsv:picocsv}}

    base
    xml
    xml-bind
    picocsv
    win

    xml --> base
    xml-bind --> xml
    xml-bind -.-> x-jaxb
    win --> base
    picocsv --> base
    picocsv -.-> x-picocsv

    classDef x fill:#00000000,stroke:#00000000,font-style:italic
    class x-jaxb,x-picocsv x
```
