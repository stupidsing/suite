# source <(curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/tools-path.sh | bash -)

curl -sL https://raw.githubusercontent.com/stupidsing/suite/master/src/main/sh/cache.sh

echo '
GH_HOME=$(cchs "echo https://github.com/cli/cli/releases/download/v1.3.1/gh_1.3.1_linux_amd64.tar.gz" @curl @tar-zxf @dir)
GIT_HD=$(cchs "echo git@github.com:stupidsing/home-data.git" @git-clone)
GOROOT=$(cchs "echo https://dl.google.com/go/go1.15.2.linux-amd64.tar.gz" @curl @tar-zxf @dir)
GRADLE_HOME=$(cchs "echo https://services.gradle.org/distributions/gradle-6.6.1-bin.zip" @curl @unzip @dir)
JAVA_HOME=$(cchs "curl -sL https://jdk.java.net/15/" "grep https://download.java.net/ | grep -v sha256 | grep linux-x64 | grep \\.tar\\.gz" "cut -d\\\" -f2" @curl @tar-zxf @dir)
M2_HOME=$(cchs "echo http://ftp.cuhk.edu.hk/pub/packages/apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz" @curl @tar-zxf @dir)
NODE_HOME=$(cchs "echo https://nodejs.org/dist/v12.18.4/node-v12.18.4-linux-x64.tar.xz" @curl @tar-xf @dir)
PATH=${GH_HOME}/bin:${GIT_HD:9}/bin:${GOROOT}/bin:${GRADLE_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${NODE_HOME}/bin:${PATH}

tp_android_avdmanager() {
	JAVA_HOME=$(tp_jdk10) \
	JAVA_OPTS="-XX:+IgnoreUnrecognizedVMOptions --add-modules java.se.ee" \
	$(tp_android_sdk_tools)/tools/bin/avdmanager $@
}

tp_android_bundletool() {
	java -jar $(cchs "echo https://github.com/google/bundletool/releases/download/1.0.0/bundletool-all-1.0.0.jar" @curl) $@
}

tp_android_emulator() {
	# sudo adduser ${USER} kvm
	$(tp_android_sdk_tools)/tools/emulator $@
}

tp_android_sdk_tools() {
	# "@do-cd JAVA_HOME=$(tp_jdk10) JAVA_OPTS='\''-XX:+IgnoreUnrecognizedVMOptions --add-modules java.se.ee'\''
	cchs "curl -sL https://developer.android.com/studio" "grep dl.google.com | grep commandlinetools-linux" "head -1" "cut -d\\\" -f2" @curl "@unzip cmdline-tools" "@cd pwd" \
	"@do-cd JAVA_HOME=$(tp_jdk8) JAVA_OPTS=-XX:+IgnoreUnrecognizedVMOptions cmdline-tools/tools/bin/sdkmanager \
	'\''build-tools;29.0.3'\'' emulator platform-tools '\''platforms;android-27'\'' '\''system-images;android-27;default;x86_64'\''"
}

tp_android_studio() {
	$(cchs "curl -sL https://developer.android.com/studio" "grep redirector.gvt1.com | grep linux.tar.gz" "head -1" "cut -d\\\" -f2" @curl @tar-zxf @dir)/bin/studio.sh $@
}

tp_apt_i() {
	PKG=${1} sh -c "dpkg -l \${PKG} > /dev/null || sudo apt install -y --force-yes --no-install-recommends \${PKG}"
}

tp_cdk() {
	# https://github.com/aws/aws-cdk
	$(cchs "echo npm-i-aws-cdk" @mkdir "@do-cd npm install aws-cdk")/node_modules/.bin/cdk $@
}

tp_cordova() {
	$(cchs "echo npm-i-cordova" @mkdir "@do-cd ${NODE_HOME}/bin/npm install cordova")/node_modules/.bin/cordova $@
}

tp_eclipse() {
	$(cchs "echo http://ftp.jaist.ac.jp/pub/eclipse/technology/epp/downloads/release/2020-09/R/eclipse-java-2020-09-R-linux-gtk-x86_64.tar.gz" @curl @tar-zxf @dir)/eclipse $@
}

tp_eclipse_cpp() {
	$(cchs "echo http://ftp.jaist.ac.jp/pub/eclipse/technology/epp/downloads/release/2020-09/R/eclipse-cpp-2020-09-R-linux-gtk-x86_64.tar.gz" @curl @tar-zxf @dir)/eclipse $@
}

tp_eksctl() {
	$(cchs "echo https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" @curl @tar-zxf)/eksctl $@
}

tp_geckodriver() {
	cchs "echo https://github.com/mozilla/geckodriver/releases/download/v0.26.0/geckodriver-v0.26.0-linux64.tar.gz" @curl @tar-zxf @dir
}

tp_google_java_format() {
	java -jar $(cchs "echo https://github.com/google/google-java-format/releases/download/google-java-format-1.8/google-java-format-1.8-all-deps.jar" @curl) $@
}

tp_gradle() {
	${GRADLE_HOME}/bin/gradle $@
}

tp_group_add() {
	G=${1} sh -c "groups | grep \${G} > /dev/null || sudo adduser \${USER} \${G}"
}

tp_hkex_securities_list() {
	tp_apt_i gnumeric
	cchs "curl -sL https://www.hkex.com.hk/eng/services/trading/securities/securitieslists/ListOfSecurities.xlsx" "ssconvert -I Gnumeric_Excel:xlsx -T Gnumeric_stf:stf_csv fd://0 fd://1"
}

tp_jdk8() {
	tp_apt_i openjdk-8-jdk
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
	local VER=$(cchs "curl -sL https://storage.googleapis.com/kubernetes-release/release/stable.txt")
	$(cchs "echo https://storage.googleapis.com/kubernetes-release/release/${VER}/bin/linux/amd64/kubectl" @curl "@do-chmod +x") $@
}

tp_minikube() {
	$(cchs "echo https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64" @curl "@do-chmod +x") $@
}

tp_minions() {
	$(cchs "echo https://github.com/blahgeek/Minions.git" @git-clone "@do-git-cd cargo build --release" "@git-cd pwd")/target/release/minions
}

tp_mirrormagic() {
	$(cchs "echo https://www.artsoft.org/RELEASES/unix/mirrormagic/mirrormagic-3.0.0.tar.gz" @curl @tar-zxf @dir "@do-cd make")/mirrormagic $@
}

tp_rocksndiamonds() {
	# https://www.artsoft.org/rocksndiamonds/news/
	$(cchs "echo https://www.artsoft.org/RELEASES/unix/rocksndiamonds/rocksndiamonds-4.1.3.0.tar.gz" @curl @tar-zxf @dir "@do-cd make")/rocksndiamonds
}

tp_slant() {
	$(cchs "echo https://www.chiark.greenend.org.uk/~sgtatham/puzzles/puzzles.tar.gz" @curl @tar-zxf @dir "@do-cd ./configure" "@do-cd make")/slant $@
}

tp_suite() {
	$(cchs "echo git@github.com:stupidsing/suite.git" @git-clone "@do-git-cd ./build.sh" "@git-cd pwd")/run.sh $@
}

tp_leafpad() {
	$(cchs "echo http://savannah.nongnu.org/download/leafpad/leafpad-0.8.17.tar.gz" @curl @tar-zxf @dir "@do-cd ./configure" "@do-cd make")/src/leafpad $@
}

tp_vms_empire() {
	$(cchs "echo git@gitlab.com:esr/vms-empire.git" @git-clone "@do-git-cd make" "@git-cd pwd")/vms-empire $@
}

tp_vscode() {
	$(cchs "echo https://az764295.vo.msecnd.net/stable/26076a4de974ead31f97692a0d32f90d735645c0/code-stable-1576682093.tar.gz" @curl @tar-zxf @dir)/bin/code $@
}

tp_wdp() {
	tp_apt_i wine
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
