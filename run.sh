#!/bin/sh

BASE="`dirname ${0}`"
OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -Dsuite.dir=${BASE}"

${BASE}/build.sh &&

CMD="java ${OPTS} -cp $(cat ${BASE}/target/classpath):${BASE}/target/suite-1.0.jar suite.cli.Main $@" &&

if which rlwrap > /dev/null; then
	rlwrap -D2 -H "${HOME}/.suite_history" --history-no-dupes 2 -i ${CMD}
else
	${CMD}
fi
