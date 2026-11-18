# demo
- demo batch project

## Libraries
- https://github.com/4CsOnline/file-processing-commons

The project resolves `file-processing-commons` as a Maven dependency, including
its transitive dependencies. If version `1.6.0-SNAPSHOT` is not available in a
configured Maven repository, install the bundled JAR locally (Maven uses the
POM embedded in the JAR):

```powershell
mvn install:install-file "-Dfile=lib\file-processing-commons-1.6.0-SNAPSHOT.jar"
```

Reload the Maven project in IntelliJ after installing the artifact.

## SFTP Configuration and PGP Configuration
- User Guide: [SFTP/PGP guide](docs/sftp-pgp-guide.md)
