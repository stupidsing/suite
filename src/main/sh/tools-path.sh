# source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/tools-path.sh | bash -)

curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/cache.sh

echo '
GIT_HD=$(cchs "echo git@github.com:stupidsing/home-data.git" @git-clone)
GOROOT=$(cchs "echo https://dl.google.com/go/go1.13.4.linux-amd64.tar.gz" @curl @tar-zxf @dir)
GRADLE_HOME=$(cchs "echo https://services.gradle.org/distributions/gradle-6.0.1-bin.zip" @curl @unzip @dir)
JAVA_HOME=$(cchs "curl -sL https://jdk.java.net/13/" "grep https://download.java.net/ | grep -v sha256 | grep linux | grep \\.tar\\.gz" "cut -d\\\" -f2" @curl @tar-zxf @dir)
M2_HOME=$(cchs "echo http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz" @curl @tar-zxf @dir)
NODE_HOME=$(cchs "echo https://nodejs.org/dist/v12.13.1/node-v12.13.1-linux-x64.tar.xz" @curl @tar-xf @dir)
PATH=${GIT_HD:9}/bin:${GOROOT}/bin:${GRADLE_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${NODE_HOME}/bin:${PATH}

tp_android_avdmanager() {
	JAVA_HOME=$(tp_jdk10) \
	$(tp_android_sdk_tools)/tools/bin/avdmanager $@
}

tp_android_emulator() {
	# sudo adduser ${USER} kvm
	$(tp_android_sdk_tools)/tools/emulator $@
}

tp_android_sdk_tools() {
	JAVA_HOME=$(tp_jdk10) \
	cchs "curl -sL https://developer.android.com/studio" "grep dl.google.com | grep sdk-tools-linux" "head -1" "cut -d\\\" -f2" @curl @unzip "@cd pwd" \
	"@do-cd JAVA_HOME=${JAVA_HOME} JAVA_OPTS='\''-XX:+IgnoreUnrecognizedVMOptions --add-modules java.se.ee'\'' ./tools/bin/sdkmanager \
	'\''build-tools;23.0.3'\'' emulator platform-tools '\''platforms;android-23'\'' '\''system-images;android-23;default;x86_64'\''"
}

tp_android_studio() {
	$(cchs "curl -sL https://developer.android.com/studio" "grep dl.google.com | grep linux.tar.gz" "head -1" "cut -d\\\" -f2" @curl @tar-zxf @dir)/bin/studio.sh $@
}

tp_cordova() {
	$(cchs "echo https://cordova.apache.org/" @mkdir "@do-cd npm install cordova")/node_modules/.bin/cordova $@
}

tp_eclipse() {
	$(cchs "echo http://ftp.jaist.ac.jp/pub/eclipse/technology/epp/downloads/release/2019-12/R/eclipse-java-2019-12-R-linux-gtk-x86_64.tar.gz" @curl @tar-zxf @dir)/eclipse $@
}

tp_jdk8() {
	echo /usr/lib/jvm/java-8-openjdk-amd64
}

tp_jdk10() {
	echo > /tmp/install-certs.sh
	for P in dl dl-ssl; do
	echo "cat /dev/null | openssl s_client -showcerts -connect ${P}.google.com:443 -servername ${P}.google.com | openssl x509 | ./bin/keytool -import -keystore lib/security/cacerts -storepass changeit -noprompt -alias ${P}_google_com" >> /tmp/install-certs.sh
	done
	cchs "echo https://download.java.net/openjdk/jdk10/ri/openjdk-10+44_linux-x64_bin_ri.tar.gz" @curl @tar-zxf @dir "@do-cd sh /tmp/install-certs.sh"
}

tp_kubectl() {
	VER=$(cchs "echo https://storage.googleapis.com/kubernetes-release/release/stable.txt" @curl)
	$(cchs "echo https://storage.googleapis.com/kubernetes-release/release/${VER}/bin/linux/amd64/kubectl" @curl "@do-chmod +x") $@
}

tp_minikube() {
	$(cchs "echo https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64" @curl "@do-chmod +x") $@
}

tp_rocksndiamonds() {
	# https://www.artsoft.org/rocksndiamonds/news/
	$(cchs "echo https://www.artsoft.org/RELEASES/unix/rocksndiamonds/rocksndiamonds-4.1.3.0.tar.gz" @curl @tar-zxf @dir "@do-cd make")/rocksndiamonds
}

tp_suite() {
	GIT_SUITE=$(cchs "echo git@github.com:stupidsing/suite.git" @git-clone "@do-git-cd ./build.sh" "@git-cd pwd")
	${GIT_SUITE}/run.sh $@
}

tp_wdp() {
	wine $(cchs "echo https://stammel.net/spiele/wdp/wdp.exe" @curl)
}
'

#GIT_PIECES=$(cchs "echo git@github.com:stupidsing/pieces.git" @git-clone "@git-cd pwd")
#GIT_PRIMAL=$(cchs "echo git@github.com:stupidsing/primal.git" @git-clone "@do-git-cd ${M2_HOME}/bin/mvn install" "@git-cd pwd")
#cchs "echo ${GOROOT}" "{}/bin/go help"
#cchs "echo ${GRADLE_HOME}" "{}/bin/gradle --version"
#cchs "echo ${JAVA_HOME}" "{}/bin/javac -version"
#cchs "echo ${M2_HOME}" "{}/bin/mvn --version"
#cchs "echo ${NODE_HOME}" "{}/bin/npm version"
