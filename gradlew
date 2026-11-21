#!/bin/sh
set -e

APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
APP_BASE_NAME="${0##*/}"

DEFAULT_JVM_OPTS='"-Xmx2048m" "-Dfile.encoding=UTF-8"'

if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
