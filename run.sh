#!/bin/sh

# Builds by mvn -Dmaven.test.skip=true install assembly:single
# Pre-compiles by java -cp "${JAR}" org.suite.PrecompileMain

DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss1m ${DEBUGOPTS}"
JAR="`dirname ${0}`/target/suite-1.0-jar-with-dependencies.jar"

rlwrap -H ${HOME}/.suite_history java ${OPTS} -jar "${JAR}" $*
