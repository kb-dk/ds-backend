# ocp folder

The `conf/ocp/`-folder contains files intended for OpenShift.

# logback.xml

Logback configuration that outputs everything to stdout, as expected when running in an OpenShift pod.

The log level might be adjusted, but otherwise it should be left alone. If the application
is running outside of OpenShift, i.e. under Tomcat, use the file `conf/<application-ID>-logback.xml` 
as template for Logback setup instead.

# ds-image.xml

Tomcat configuration, used as-is by OpenShift and indirectly with Tomcat by copying and modifying paths.
See [DEVELOPER.md](../../DEVELOPER.md) for details on Tomcat deployment.
