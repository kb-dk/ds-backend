# How to test XSLT for transformations

* XSLTs are located in `src/main/resources/xslt/`.
* Sample XMLs for Cumulus Preservation Service are located in `src/test/resources/xml/cps1/`

The script `transform.sh` is a simple wrapper for calling the Saxon-HE. Call it with

```shell
XSLT="../main/resources/xslt/mods2solr.xsl" ./transform.sh resources/xml//000332.tif.xml
```