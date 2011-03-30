#!/bin/sh

DEBUGOPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"

cd "`dirname ${0}`" &&
LIBS=$(cat .classpath | grep var | cut -d\" -f4 | tr \\n : | sed s/M2_REPO/\\/home\\/ywsing\\/.m2\\/repository/g) &&
rlwrap -H ~/.suite_history java -cp ${LIBS}:target/suite-1.0.jar ${DEBUGOPTS} org.suite.Main
