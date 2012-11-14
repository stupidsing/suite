#!/bin/sh

# Builds by mvn -Dmaven.test.skip=true install assembly:single
# Pre-compiles by java -cp "${JAR}" org.suite.PrecompileMain

HOMEDIR="`dirname ${0}`"
JAR="${HOMEDIR}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss1m ${DEBUGOPTS} -Dhome.dir=${HOMEDIR}"

rlwrap -H ${HOME}/.suite_history java ${OPTS} -jar "${JAR}" $*
