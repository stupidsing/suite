# cchs "ls -al" "sort" "tail -3"

CCACHE=~/.cmd-cache
DCACHE=~/.dl-cache
mkdir -p ${CCACHE}/ ${DCACHE}/

find ${CCACHE}/ -type f -mtime 28 -print0 | xargs -0 echo rm -f 1>&2
find ${DCACHE}/ -maxdepth 1 -type f -mtime 360 -type d -print0 | xargs -0 echo rm -rf 1>&2

cchs() {
	local STATE=/dev/null
	while [ "${1}" ]; do
		local CMD="${1}"
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			local DIR=$(cat ${STATE})
			local STATE=$(exec-memoized "${DIR}${CMD:2}")
		elif [ "${CMD:0:3}" == "@cd" ]; then
			local DIR=$(cat ${STATE})
			local STATE=$(exec-memoized "cd ${DIR}/; ${CMD:4}")
		elif [ "${CMD}" == "@curl" ]; then
			local URL=$(cat ${STATE})
			local DF=${DCACHE}/$(url-dir "${URL}")
			local DFI=${DF}.inprogress
			[ -f ${DF} ] || exec-logged "curl -sL '${URL}' > ${DFI} && mv ${DFI} ${DF}"
			local STATE=$(exec-logged "printf ${DF}")
		elif [ "${CMD}" == "@dir" ]; then
			local DIR=$(cat ${STATE})
			local LINK=$(sh -c "readlink -f ${DIR}/*")
			local STATE=$(exec-logged "printf ${LINK}")
		elif [ "${CMD:0:6}" == "@do-cd" ]; then
			local DIR=$(cat ${STATE})
			local STATE=$(exec-memoized "cd ${DIR}/; ${CMD:7} 1>&2; echo ${DIR}")
		elif [ "${CMD:0:9}" == "@do-chmod" ]; then
			local FILE=$(cat ${STATE})
			chmod ${CMD:10} ${FILE}
			local STATE=$(exec-logged "printf ${FILE}")
		elif [ "${CMD:0:10}" == "@do-git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			local STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:11} 1>&2; echo ${GIT}")
		elif [ "${CMD:0:5}" == "@exec" ]; then
			local DIR=$(cat ${STATE})
			local MD5=$(printf "${DIR}:${CMD}" | md5sum - | cut -d" " -f1)
			local O=${CCACHE}/${MD5}.o U=${CCACHE}/${MD5}.u W=${CCACHE}/${MD5}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			local STATE=$(exec-memoized "cd ${O}/; ${CMD:6} 1>&2; echo ${O}")
			#choverlayx
		elif [ "${CMD}" == "@docker-build" ]; then
			local DOCKERNAME=${CMD:13:}-$(cat "${STATE}" | md5sum - | cut -d" " -f1)
			local STATE=$(exec-memoized "cat ${STATE} | docker build -q -t cchs/${DOCKERNAME} -")
		elif [ "${CMD:0:7}" == "@git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			local STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:8}")
		elif [ "${CMD:0:10}" == "@git-clone" ]; then
			local URL=$(cat ${STATE})
			local BRANCH=${CMD:11}
			[ "${BRANCH}" ] && local OPTS="-b ${BRANCH}"
			local DF=${DCACHE}/$(url-dir "${URL}@${BRANCH}")
			if ! [ -d ${DF} ]; then
				exec-logged "git clone ${OPTS} ${URL} ${DF} --quiet"
				touch ${DF}.pulltime
			fi
			local TS0=$(date +%s)
			local TS1=$(stat -c %Y ${DF}.pulltime)
			if (( 900 < ${TS0} - ${TS1} )); then
				exec-logged "cd ${DF}/ && git pull --force --quiet"
				touch ${DF}.pulltime
			fi
			local COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			local STATE=$(exec-logged "printf ${COMMIT}:${DF}")
		elif [ "${CMD:0:9}" == "@git-exec" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			local MD5=$(printf "${GIT}:${CMD}" | md5sum - | cut -d" " -f1)
			local O=${CCACHE}/${MD5}.o U=${CCACHE}/${MD5}.u W=${CCACHE}/${MD5}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			local STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:10} 1>&2; echo ${GIT}")
			#choverlayx
		elif [ "${CMD:0:10}" == "@maven-get" ]; then
			#local REPO=https://repo.maven.apache.org/maven2
			local RGAV=$(cat ${STATE})
			local REPO=$(echo ${RGAV} | cut -d# -f1)
			local GROUPID=$(echo ${RGAV} | cut -d# -f2)
			local ARTIFACTID=$(echo ${RGAV} | cut -d# -f3)
			local VERSION=$(echo ${RGAV} | cut -d# -f4)
			local P=$(echo ${GROUPID} | sed s#\\.#/#g)
			local URL="${REPO}/${P}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.pom"
			local DF=${DCACHE}/$(url-dir "${URL}")
			local DFI=${DF}.inprogress
			[ -f ${DF} ] || exec-logged curl -sL "${URL}" > ${DFI} && mv ${DFI} ${DF}
			local STATE=$(exec-logged "printf ${DF}")
		elif [ "${CMD:0:6}" == "@mkdir" ]; then
			local S=$(cat ${STATE})
			local DIR=${DCACHE}/$(url-dir "${S}")
			mkdir -p ${DIR}
			local STATE=$(exec-logged "printf ${DIR}")
		elif [ "${CMD:0:5}" == "@tar-" ]; then
			local OPT=${CMD:5}
			local TARFILE=$(cat ${STATE})
			local TARDIR=${TARFILE}.d
			local TARDIRI=${TARDIR}.inprogress
			[ -d ${TARDIR} ] || exec-logged "mkdir -p ${TARDIRI} && tar ${OPT} ${TARFILE} -C ${TARDIRI} && mv ${TARDIRI} ${TARDIR}"
			local STATE=$(exec-logged "printf ${TARDIR}")
		elif [ "${CMD:0:6}" == "@unzip" ]; then
			local ZIPFILE=$(cat ${STATE})
			local ZIPDIR=${ZIPFILE}.d
			local ZIPDIRI=${ZIPDIR}.inprogress
			local TARGET=${ZIPDIRI}/${CMD:7}
			[ -d ${ZIPDIR} ] || exec-logged "mkdir -p ${TARGET} && unzip -d ${TARGET} -q ${ZIPFILE} && mv ${ZIPDIRI} ${ZIPDIR}"
			local STATE=$(exec-logged "printf ${ZIPDIR}")
		else
			local STATE=$(exec-memoized "cat ${STATE} | ${CMD}")
		fi
		if [ ${?} != 0 ]; then
			echo FAIL >2
			return 1
		fi
	done
	cat ${STATE}
}

# executes a command if not executed before; otherwise, return previous result
exec-memoized() {
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
		exec-logged "${CMD}" | tee "${VF}" 1>&2 && printf "${CMD}" > "${KF}"
	fi

	printf "${VF}"
}

exec-logged() {
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
