source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/cache.sh)

GIT_HD=$(cchs "echo git@github.com:stupidsing/home-data.git" "#git-clone")
GIT_PRIMAL=$(cchs "echo git@github.com:stupidsing/primal.git" "#git-clone")
GIT_SUITE=$(cchs "echo git@github.com:stupidsing/suite.git" "#git-clone")
ECLIPSE_HOME=$(cchs "echo http://ftp.jaist.ac.jp/pub/eclipse/technology/epp/downloads/release/2019-09/R/eclipse-java-2019-09-R-linux-gtk-x86_64.tar.gz" "#curl" "#tar-zxf" "#dir")
GOROOT=$(cchs "echo https://dl.google.com/go/go1.13.4.linux-amd64.tar.gz" "#curl" "#tar-zxf" "#dir")
GRADLE_HOME=$(find ~/ -maxdepth 1 -name gradle-\* | sort | tail -1)
JAVA_HOME=$(cchs "curl -sL https://jdk.java.net/13/" "grep 'https://download.java.net/' | grep -v sha256 | grep linux | grep '\.tar\.gz'" "cut -d'\"' -f2" "#curl" "#tar-zxf" "#dir")
M2_HOME=$(cchs "echo http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz" "#curl" "#tar-zxf" "#dir")
NODE_HOME=$(cchs "echo https://nodejs.org/dist/v12.13.1/node-v12.13.1-linux-x64.tar.xz" "#curl" "#tar-xf" "#dir")

TOOLS_PATH="${GIT_HD:9}/bin:${GOROOT}/bin:${GRADLE_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${NODE_HOME}/bin"

echo "tools path = ${TOOLS_PATH}"
export PATH=${TOOLS_PATH}:${PATH}

cchs "echo ${GIT_PRIMAL}" "{V} ${M2_HOME}/bin/mvn install"
cchs "echo ${GIT_SUITE}" "{V} ./build.sh"
cchs "echo '/ dump yes #'" "${GIT_SUITE:9}/run.sh"
cchs "echo ${GOROOT}" "{}/bin/go help"
cchs "echo ${JAVA_HOME}" "{}/bin/javac -version"
cchs "echo ${M2_HOME}" "{}/bin/mvn --version"
cchs "echo ${NODE_HOME}" "{}/bin/npm version"

${ECLIPSE_HOME}/eclipse -data ~/workspace/
