#!/bin/sh

# Builds by mvn -Dmaven.test.skip=true assembly:single

DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss1m ${DEBUGOPTS}"

cd "`dirname ${0}`" &&
rlwrap -H ~/.suite_history java ${OPTS} -jar target/suite-1.0-jar-with-dependencies.jar $*
