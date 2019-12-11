# source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/tools-path.sh | bash -)

curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/cache.sh
echo '
ECLIPSE_HOME=$(cchs "echo http://ftp.jaist.ac.jp/pub/eclipse/technology/epp/downloads/release/2019-09/R/eclipse-java-2019-09-R-linux-gtk-x86_64.tar.gz" "#curl" "#tar-zxf" "#dir")
GIT_HD=$(cchs "echo git@github.com:stupidsing/home-data.git" "#git-clone")
GIT_PIECES=$(cchs "echo git@github.com:stupidsing/pieces.git" "#git-clone")
GIT_PRIMAL=$(cchs "echo git@github.com:stupidsing/primal.git" "#git-clone")
GIT_SUITE=$(cchs "echo git@github.com:stupidsing/suite.git" "#git-clone")
GOROOT=$(cchs "echo https://dl.google.com/go/go1.13.4.linux-amd64.tar.gz" "#curl" "#tar-zxf" "#dir")
GRADLE_HOME=$(cchs "echo https://services.gradle.org/distributions/gradle-6.0.1-bin.zip" "#curl" "#unzip" "#dir")
JAVA_HOME=$(cchs "curl -sL https://jdk.java.net/13/" "grep https://download.java.net/ | grep -v sha256 | grep linux | grep \\.tar\\.gz" "cut -d\\\" -f2" "#curl" "#tar-zxf" "#dir")
M2_HOME=$(cchs "echo http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz" "#curl" "#tar-zxf" "#dir")
NODE_HOME=$(cchs "echo https://nodejs.org/dist/v12.13.1/node-v12.13.1-linux-x64.tar.xz" "#curl" "#tar-xf" "#dir")
PATH=${ECLIPSE_HOME}:${GIT_HD:9}/bin:${GOROOT}/bin:${GRADLE_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${NODE_HOME}/bin:${PATH}'

#cchs "echo ${GIT_PRIMAL}" "#git-cd-cmd ${M2_HOME}/bin/mvn install"
#cchs "echo ${GIT_SUITE}" "#git-cd-cmd ./build.sh"
#cchs "echo / dump yes \#" "#git-cd ./run.sh"
#cchs "echo ${GOROOT}" "{}/bin/go help"
#cchs "echo ${GRADLE_HOME}" "{}/bin/gradle --version"
#cchs "echo ${JAVA_HOME}" "{}/bin/javac -version"
#cchs "echo ${M2_HOME}" "{}/bin/mvn --version"
#cchs "echo ${NODE_HOME}" "{}/bin/npm version"

#${ECLIPSE_HOME}/eclipse -data ~/workspace/
