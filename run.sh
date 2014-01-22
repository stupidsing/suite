#!/bin/sh

HOMEDIR="`dirname ${0}`"
JAR="${HOMEDIR}/target/suite-1.0-jar-with-dependencies.jar"
DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
OPTS="-Xss2m -Xmx256m ${DEBUGOPTS} -Dhome.dir=${HOMEDIR}"

[ -f "${JAR}" ] && BUILDTIME=$(stat -c %Y "${JAR}") || BUILDTIME=0
SRCTIME=$(find "${HOMEDIR}/pom.xml" "${HOMEDIR}/src/main/" -type f | xargs stat -c %Y | sort -g | tail -1)

(
	[ ${SRCTIME} -le ${BUILDTIME} ] ||
	(
		mvn -Dmaven.test.skip=true install assembly:single &&
		java ${OPTS} -jar "${JAR}" -precompile STANDARD < /dev/null
	)
) &&
CMD="java ${OPTS} -jar ${JAR} $@" &&
(which rlwrap > /dev/null && rlwrap -H ${HOME}/.suite_history ${CMD} || ${CMD})
