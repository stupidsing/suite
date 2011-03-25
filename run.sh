#!/bin/sh

cd "`dirname ${0}`" &&
LIBS=$(cat .classpath | grep var | cut -d\" -f4 | tr \\n : | sed s/M2_REPO/\\/home\\/ywsing\\/.m2\\/repository/g) &&
rlwrap -H ~/.suite_history java -cp ${LIBS}:target/suite-1.0.jar org.suite.Main
