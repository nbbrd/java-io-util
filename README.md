# Common I/O utilities for Java 

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