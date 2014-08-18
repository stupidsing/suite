#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${BASE}"
CMD="java ${OPTS} -jar ${JAR} $@"

getLatestTimestamp() {
	xargs -I {} sh -c 'stat -c %Y "{}" 2> /dev/null || echo 0' | sort -g | tail -1
}

(
	SRCTIME=$(find "${BASE}/pom.xml" "${BASE}/src/main/java/" "${BASE}/src/main/resources/" -type f 2> /dev/null | getLatestTimestamp)
	TARGETTIME=$(echo "${JAR}" | getLatestTimestamp)
	[ ${SRCTIME} -le ${TARGETTIME} ] || mvn -Dmaven.test.skip=true install assembly:single
) &&

(
	SRCTIME=$(echo "${JAR}" | getLatestTimestamp)
	TARGETTIME=$(echo "${BASE}/precompiled/STANDARD.node.gz" | getLatestTimestamp)
	[ ${SRCTIME} -le ${TARGETTIME} ] ||
	(echo | java ${OPTS} -jar "${JAR}" precompile-all || (rm -f ${BASE}/precompiled/STANDARD.node.gz && false))
) &&

if which rlwrap > /dev/null; then
	rlwrap -D2 -H "${HOME}/.suite_history" -i ${CMD}
else
	${CMD}
fi
