#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${BASE}"

{ stat --version 2> /dev/null | grep GNU > /dev/null; } && STAT="stat -c %Y" || STAT="stat -f %m"

getLatestTimestamp() {
	TS=$(find "$@" -type f 2> /dev/null | xargs ${STAT} 2> /dev/null | sort -g | tail -1)
	[ ${TS} ] && echo ${TS} || echo 0
}

(
	SOURCETIME=$(getLatestTimestamp "${BASE}/pom.xml" "${BASE}/src/main/" "${BASE}/src/main/resources/")
	TARGETTIME=$(getLatestTimestamp "${JAR}")
	[ ${SOURCETIME} -le ${TARGETTIME} ] ||
	[ "${SKIPBUILD}" ] ||
	{ cd ${BASE} && mvn -Dmaven.test.skip=true install assembly:single; }
) &&

[ "${MAIN}" ] && CMD="java ${OPTS} -cp ${JAR} ${MAIN}" || CMD="java ${OPTS} -jar ${JAR}"
FULLCMD="${CMD} $@"

if which rlwrap > /dev/null; then
	rlwrap -D2 -H "${HOME}/.suite_history" --history-no-dupes 2 -i ${FULLCMD}
else
	${FULLCMD}
fi
