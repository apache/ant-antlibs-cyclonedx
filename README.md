# Apache CyclondeDX Ant Library

This is a library of Ant tasks and types providing ways to create
[CycloneDX](https://cyclonedx.org]) SBOMs for Apache Ant built
artifacts.

[Homepage](https://ant.apache.org/antlibs/cyclonedx/),
[Binary Releases](https://ant.apache.org/antlibs/bindownload.cgi).

This library provides the basic infrastructure that should be good
enough to create SBOMs for projects that use manual management of
dependencies (or don't have any dependencies at all). An extension for
projects that use Ivy and can reduce the manual work is planned.

The library provides types for licenses, organizations and external
references that can be reused in multiple components as well a a type
for components that may be referenced from multiple entries. A task
allows the creation of CycloneDX SBOMs based on these types in JSON
and XML format.

The heavy lifting is performed by [CycloneDX Core
(Java)](https://github.com/CycloneDX/cyclonedx-core-java).

Right now the library is in an early stage of development and the main
audience is Ant itself. This doesn't mean it couldn't be used for
other projects and of course we are open to extend it to make things
easier.

Both the Java API as well as the Ant build-file API (attributes and
nested elements) may change with future releases.
