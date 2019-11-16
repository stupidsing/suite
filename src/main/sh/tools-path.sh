BASE="`dirname ${0}`"
. ${BASE}/cache.sh
GIT_HD=$(cchs "echo git@github.com:stupidsing/home-data.git" "#git-clone")
GIT_PRIMAL=$(cchs "echo git@github.com:stupidsing/primal.git" "#git-clone")
GIT_SUITE=$(cchs "echo git@github.com:stupidsing/suite.git" "#git-clone")
GOROOT=$(cchs "echo https://dl.google.com/go/go1.13.4.linux-amd64.tar.gz" "#curl" "#tar-zxf" "#dir")
GRADLE_HOME=$(find ~/ -maxdepth 1 -name gradle-\* | sort | tail -1)
JAVA_HOME=$(cchs "curl -sL https://jdk.java.net/13/" "grep 'https://download.java.net/' | grep -v sha256 | grep linux | grep '\.tar\.gz'" "cut -d'\"' -f2" "#curl" "#tar-zxf" "#dir")
M2_HOME=$(cchs "echo http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz" "#curl" "#tar-zxf" "#dir")
NODE_HOME=$(cchs "echo https://nodejs.org/dist/v12.13.0/node-v12.13.0-linux-x64.tar.xz" "#curl" "#tar-xf" "#dir")

TOOLS_PATH="${GIT_HD}/bin:${GOROOT}/bin:${GRADLE_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${NODE_HOME}/bin:${GIT_HD}/bin"

echo "tools path = ${TOOLS_PATH}"
export PATH=${TOOLS_PATH}:${PATH}

cd ${GIT_PRIMAL} && ${M2_HOME}/bin/mvn install
cchs "echo ${GIT_SUITE}" "{}/build.sh"
cchs "echo '/ dump yes #'" "${GIT_SUITE}/run.sh"
cchs "echo ${GOROOT}" "{}/bin/go help"
cchs "echo ${JAVA_HOME}" "{}/bin/javac -version"
cchs "echo ${M2_HOME}" "{}/bin/mvn --help"
cchs "echo ${NODE_HOME}" "{}/bin/npm help"