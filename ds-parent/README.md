# Developer documentation

This project is a repository found at [github](https://github.com/kb-dk/ds-parent)
from the Royal Danish Library.

The information in this document is aimed at developers who are going to work at the Digitale Samlinger project


## Initial use

This project is a parent project for the ds-xxx modules.
It contains a pom.xml which is the parent pom for all submodules (ds-xxx).
The submodules can be found in pom.xml.

Start by cloning this repository into a folder called e.g. "ds-parent"
and inside that folder clone all the submodules one by one.

All modules can be build at once by running 

```
mvn clean package
```
or 
```
mvn clean install
```
in the root of "ds-parent" folder.

## Purpose

The purpose of a maven-multi-project is to consolidate all versions in the parent pom
so that the submodules only depend on dependencies noted in the parent pom file.
If a version changes in the parent pom all submodules are updated at once.

If a submodule use a specific dependency only for that module it can be added in the submodule
having the version specified - hence it is not added to the parent pom.

We have chosen to define the build version in the parent pom which is inherited by all submodules.
In that way the build ensures that the dependencies between submodules are consistent.
This choice implies that all modules should be released when doing a hotfix or a new major release.
In the future the dependencies between the submodules must be replaced by loose coupling REST services.