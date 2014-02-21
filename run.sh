#!/bin/sh

BASE="`dirname ${0}`"
JAR="${BASE}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${BASE}"

SRCTIME=$(find "${BASE}/pom.xml" "${BASE}/src/main/java" "${BASE}/src/main/resources" -type f | xargs stat -c %Y | sort -g | tail -1)
TARGETTIME=$(echo "${JAR}" "${BASE}/precompiled/STANDARD.rpn" | xargs -I {} sh -c 'stat -c %Y {} 2> /dev/null || echo 0' | sort -g | head -1)

(
	[ ${SRCTIME} -le ${TARGETTIME} ] ||
	(
		mvn -Dmaven.test.skip=true install assembly:single &&
		(java ${OPTS} -jar "${JAR}" -precompile-all < /dev/null || (rm -f ${BASE}/precompiled/STANDARD.rpn && false))
	)
) &&

CMD="java ${OPTS} -jar ${JAR} $@" &&
if which rlwrap > /dev/null; then
	rlwrap -D2 -H ${HOME}/.suite_history -i ${CMD}
else
	${CMD}
fi
