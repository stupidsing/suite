#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${BASE}"
CMD="java ${OPTS} -jar ${JAR}"
FULLCMD="${CMD} $@"

getLatestTimestamp() {
	TS=$(find "$@" -type f 2> /dev/null | xargs stat -c %Y 2> /dev/null | sort -g | tail -1)
	[ ${TS} ] && echo ${TS} || echo 0
}

(
	SOURCETIME=$(getLatestTimestamp "${BASE}/pom.xml" "${BASE}/src/main/java/" "${BASE}/src/main/resources/")
	TARGETTIME=$(getLatestTimestamp "${JAR}")
	[ ${SOURCETIME} -le ${TARGETTIME} ] || (cd ${BASE} && mvn -Dmaven.test.skip=true -T4 install assembly:single)
) &&

(
	SOURCETIME=$(getLatestTimestamp "${JAR}")
	TARGETTIME=$(getLatestTimestamp "${BASE}/precompiled/STANDARD.node.gz")
	[ ${SOURCETIME} -le ${TARGETTIME} ] ||
	(echo | ${CMD} precompile-all || (rm -f ${BASE}/precompiled/STANDARD.node.gz && false))
) &&

if which rlwrap > /dev/null; then
	rlwrap -D2 -H "${HOME}/.suite_history" --history-no-dupes 2 -i ${FULLCMD}
else
	${FULLCMD}
fi
