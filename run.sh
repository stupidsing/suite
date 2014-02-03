#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${BASE}"

SRCTIME=$(find "${BASE}/pom.xml" "${BASE}/src/main/" -type f | xargs stat -c %Y | sort -g | tail -1)
TARGETTIME=$(echo "${JAR}" "${BASE}/STANDARD.rpn" | xargs -I {} sh -c 'stat -c %Y {} 2> /dev/null || echo 0' | sort -g | head -1)

(
	[ ${SRCTIME} -le ${TARGETTIME} ] ||
	(
		mvn -Dmaven.test.skip=true install assembly:single &&
		(java ${OPTS} -jar "${JAR}" -precompile STANDARD < /dev/null || (rm -f ${BASE}/STANDARD.rpn && false))
	)
) &&

CMD="java ${OPTS} -jar ${JAR} $@" &&
(which rlwrap > /dev/null && rlwrap -D2 -H ${HOME}/.suite_history ${CMD} || ${CMD})
