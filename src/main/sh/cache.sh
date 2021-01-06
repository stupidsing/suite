# cchs "ls -al" "sort" "tail -3"

CCACHE=~/.cmd-cache
DCACHE=~/.dl-cache
mkdir -p ${CCACHE}/ ${DCACHE}/

find ${CCACHE}/ -type f -mtime 28 -print0 | xargs -0 echo rm -f 1>&2
find ${DCACHE}/ -maxdepth 1 -type f -mtime 360 -type d -print0 | xargs -0 echo rm -rf 1>&2

cchs() {
	local F=/dev/null
	while [ "${1}" ]; do
		local CMD="${1}"
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			local D=$(cat ${F})
			local F=$(cchf "${D}${CMD:2}")
		elif [ "${CMD:0:3}" == "@cd" ]; then
			local D=$(cat ${F})
			local F=$(cchf "cd ${D}/; ${CMD:4}")
		elif [ "${CMD}" == "@curl" ]; then
			local URL=$(cat ${F})
			local DF=${DCACHE}/$(url-dir "${URL}")
			local DFI=${DF}.inprogress
			[ -f ${DF} ] || do-cmd "curl -sL '${URL}' > ${DFI} && mv ${DFI} ${DF}"
			local F=$(cchf "printf ${DF}")
		elif [ "${CMD}" == "@dir" ]; then
			local D=$(cat ${F})
			local LINK=$(sh -c "readlink -f ${D}/*")
			local F=$(cchf "printf ${LINK}")
		elif [ "${CMD:0:6}" == "@do" ]; then
			local D=$(cat ${F})
			local MD5=$(printf "${D}:${CMD}" | md5sum - | cut -d" " -f1)
			local F=$(cchf "cd ${D}/; ${CMD:4} 1>&2; echo ${D}")
			local O=${CCACHE}/${MD5}.o U=${CCACHE}/${MD5}.u
			mkdir -p ${U}/ ${O}/
			choverlay_ ${D}/ ${U}/ ${O}/
			local F=$(cchf "cd ${O}/; ${CMD:7} 1>&2; echo ${O}")
			#choverlayx
		elif [ "${CMD:0:6}" == "@do-cd" ]; then
			local D=$(cat ${F})
			local MD5=$(printf "${D}:${CMD}" | md5sum - | cut -d" " -f1)
			local F=$(cchf "cd ${D}/; ${CMD:7} 1>&2; echo ${D}")
		elif [ "${CMD:0:9}" == "@do-chmod" ]; then
			local FILE=$(cat ${F})
			chmod ${CMD:10} ${FILE}
			local F=$(cchf "printf ${FILE}")
		elif [ "${CMD:0:10}" == "@do-git-cd" ]; then
			local D=$(cat ${F})
			local F=$(cchf "V=${D:0:8}; cd ${D:9}/; ${CMD:11} 1>&2; echo ${D}")
		elif [ "${CMD}" == "@docker-build" ]; then
			local DOCKERNAME=${CMD:13:}-$(cat "${F}" | md5sum - | cut -d" " -f1)
			local F=$(cchf "cat ${F} | docker build -q -t cchs/${DOCKERNAME} -")
		elif [ "${CMD:0:7}" == "@git-cd" ]; then
			local D=$(cat ${F})
			local F=$(cchf "V=${D:0:8}; cd ${D:9}/; ${CMD:8}")
		elif [ "${CMD:0:10}" == "@git-clone" ]; then
			local URL=$(cat ${F})
			local B=${CMD:11}
			[ "${B}" ] && local OPTS="-b ${B}"
			local DF=${DCACHE}/$(url-dir "${URL}@${B}")
			if ! [ -d ${DF} ]; then
				do-cmd "git clone ${OPTS} ${URL} ${DF} --quiet"
				touch ${DF}.pulltime
			fi
			local D0=$(date +%s)
			local D1=$(stat -c %Y ${DF}.pulltime)
			if (( 900 < ${D0} - ${D1} )); then
				do-cmd "cd ${DF}/ && git pull --force --quiet"
				touch ${DF}.pulltime
			fi
			local COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			local F=$(cchf "printf ${COMMIT}:${DF}")
		elif [ "${CMD:0:10}" == "@maven-get" ]; then
			#local REPO=https://repo.maven.apache.org/maven2
			local RGAV=$(cat ${F})
			local REPO=$(echo ${RGAV} | cut -d# -f1)
			local GROUPID=$(echo ${RGAV} | cut -d# -f2)
			local ARTIFACTID=$(echo ${RGAV} | cut -d# -f3)
			local VERSION=$(echo ${RGAV} | cut -d# -f4)
			local P=$(echo ${GROUPID} | sed s#\\.#/#g)
			local URL="${REPO}/${P}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.pom"
			local DF=${DCACHE}/$(url-dir "${URL}")
			local DFI=${DF}.inprogress
			[ -f ${DF} ] || do-cmd curl -sL "${URL}" > ${DFI} && mv ${DFI} ${DF}
			local F=$(cchf "printf ${DF}")
		elif [ "${CMD:0:6}" == "@mkdir" ]; then
			local S=$(cat ${F})
			local DF=${DCACHE}/$(url-dir "${S}")
			mkdir -p ${DF}
			local F=$(cchf "printf ${DF}")
		elif [ "${CMD:0:5}" == "@tar-" ]; then
			local OPT=${CMD:5}
			local TARF=$(cat ${F})
			local TARDIR=${TARF}.d
			local TARDIRI=${TARDIR}.inprogress
			[ -d ${TARDIR} ] || do-cmd "mkdir -p ${TARDIRI} && tar ${OPT} ${TARF} -C ${TARDIRI} && mv ${TARDIRI} ${TARDIR}"
			local F=$(cchf "printf ${TARDIR}")
		elif [ "${CMD:0:6}" == "@unzip" ]; then
			local ZIPF=$(cat ${F})
			local ZIPDIR=${ZIPF}.d
			local ZIPDIRI=${ZIPDIR}.inprogress
			local TARGET=${ZIPDIRI}/${CMD:7}
			[ -d ${ZIPDIR} ] || do-cmd "mkdir -p ${TARGET} && unzip -d ${TARGET} -q ${ZIPF} && mv ${ZIPDIRI} ${ZIPDIR}"
			local F=$(cchf "printf ${ZIPDIR}")
		else
			local F=$(cchf "cat ${F} | ${CMD}")
		fi
	done
	cat ${F}
}

# executes a command if not executed before; otherwise, return previous result
cchf() {
	local CMD="${@}"
	local MD5=$(printf "${CMD}" | md5sum - | cut -d" " -f1)
	local P=${MD5:0:2}
	local DIR=${CCACHE}/${P}
	local FP=${DIR}/${MD5}
	local KF=${FP}.k
	local VF=${FP}.v

	mkdir -p ${DIR}/

	if [ "${CACHE}" != "off" ] && [ -f "${KF}" ] && diff <(printf "${CMD}") <(cat "${KF}"); then
		true
	else
		do-cmd "${CMD}" | tee "${VF}" 1>&2 && printf "${CMD}" > "${KF}"
	fi

	printf "${VF}"
}

do-cmd() {
	local CMD="${@}"
	echo "START ${CMD}" >&2
	sh -c "${CMD}"
	echo "END~${?} ${CMD}" >&2
}

url-dir() {
	local MD5=$(printf "${1}" | md5sum - | cut -d" " -f1)
	local SHORT=$(printf "${1}" | url-dir-name)
	echo ${MD5:0:8}.${SHORT}
}

url-dir-name() {
	tr /:@ _ | tr -dc '[\-.0-9A-Z_a-z]'
}
