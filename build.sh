#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0.jar"

{ stat --version 2> /dev/null | grep GNU > /dev/null; } && STAT="stat -c %Y" || STAT="stat -f %m"

getLatestTimestamp() {
	TS=$(find "$@" -type f 2> /dev/null | xargs ${STAT} 2> /dev/null | sort -g | tail -1)
	[ ${TS} ] && echo ${TS} || echo 0
}

(
	SOURCETIME=$(getLatestTimestamp "${BASE}/pom.xml" "${BASE}/src/main/")
	TARGETTIME=$(getLatestTimestamp "${JAR}")
	[ ${SOURCETIME} -le ${TARGETTIME} ] ||
	{ cd ${BASE} && mvn dependency:build-classpath -Dmdep.outputFile=${BASE}/target/classpath && mvn -Dmaven.test.skip=true install; }
)
