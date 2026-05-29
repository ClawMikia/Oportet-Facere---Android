#!/usr/bin/env sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################

if [ -z "" ] ; then
    if [ -n "" ] ; then
        JAVA_CMD=java
    else
        echo "ERROR: JAVA_HOME is not set and java could not be found in PATH."
        exit 1
    fi
else
    JAVA_CMD="/bin/java"
fi

CLASSPATH="/gradle/wrapper/gradle-wrapper.jar"
exec "" -cp "" org.gradle.wrapper.GradleWrapperMain "$@"
