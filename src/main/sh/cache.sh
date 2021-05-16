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
			STATE=$(exec-memoized "${DIR}${CMD:2}")
		elif [ "${CMD:0:3}" == "@cd" ]; then
			local DIR=$(cat ${STATE})
			STATE=$(exec-memoized "cd ${DIR}/; ${CMD:4}")
		elif [ "${CMD}" == "@curl" ]; then
			local URL=$(cat ${STATE})
			local FILE=${DCACHE}/$(url-dir "${URL}")
			local FILEI=${FILE}.inprogress
			[ -f ${FILE} ] || exec-logged "curl -sL '${URL}' > ${FILEI} && mv ${FILEI} ${FILE}"
			STATE=$(exec-memoized "printf ${FILE}")
		elif [ "${CMD}" == "@dir" ]; then
			local DIR=$(cat ${STATE})
			local LINK=$(sh -c "readlink -f ${DIR}/*")
			STATE=$(exec-memoized "printf ${LINK}")
		elif [ "${CMD:0:6}" == "@do-cd" ]; then
			local DIR=$(cat ${STATE})
			STATE=$(exec-memoized "cd ${DIR}/; ${CMD:7} 1>&2 && echo ${DIR}")
		elif [ "${CMD:0:9}" == "@do-chmod" ]; then
			local FILE=$(cat ${STATE})
			chmod ${CMD:10} ${FILE}
			STATE=$(exec-memoized "printf ${FILE}")
		elif [ "${CMD:0:10}" == "@do-git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:11} 1>&2 && echo ${GIT}")
		elif [ "${CMD:0:5}" == "@exec" ]; then
			local DIR=$(cat ${STATE})
			local PREFIX=$(md5-dir "${DIR}:${CMD}")
			local O=${PREFIX}.o U=${PREFIX}.u W=${PREFIX}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			STATE=$(exec-memoized "cd ${O}; ${CMD:6} 1>&2 && echo ${O}")
			#choverlayx
		elif [ "${CMD}" == "@docker-build" ]; then
			local FILE=${STATE}
			local IMAGE=${CMD:13:}-$(cat ${FILE} | md5sum - | cut -d" " -f1)
			STATE=$(exec-memoized "cat ${FILE} | docker build -q -t cchs/${IMAGE} -")
		elif [ "${CMD:0:7}" == "@git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:8}")
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
			STATE=$(exec-memoized "printf ${COMMIT}:${DF}")
		elif [ "${CMD:0:9}" == "@git-exec" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			local PREFIX=$(md5-dir "${GIT}:${CMD}")
			local O=${PREFIX}.o U=${PREFIX}.u W=${PREFIX}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			STATE=$(exec-memoized "V=${GIT:0:8}; cd ${DIR}/; ${CMD:10} 1>&2 && echo ${GIT}")
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
			local FILE=${DCACHE}/$(url-dir "${URL}")
			local FILEI=${FILE}.inprogress
			[ -f ${FILE} ] || exec-logged curl -sL "${URL}" > ${FILEI} && mv ${FILEI} ${FILE}
			STATE=$(exec-memoized "printf ${DF}")
		elif [ "${CMD:0:6}" == "@mkdir" ]; then
			local NAME=$(cat ${STATE})
			local DIR=${DCACHE}/$(url-dir "${NAME}")
			mkdir -p ${DIR}
			STATE=$(exec-memoized "printf ${DIR}")
		elif [ "${CMD:0:5}" == "@tar-" ]; then
			local OPT=${CMD:5}
			local FILE=$(cat ${STATE})
			local DIR=${FILE}.d
			local DIRI=${DIR}.inprogress
			[ -d ${DIR} ] || exec-logged "mkdir -p ${DIRI} && tar ${OPT} ${FILE} -C ${DIRI} && mv ${DIRI} ${DIR}"
			STATE=$(exec-memoized "printf ${DIR}")
		elif [ "${CMD:0:6}" == "@unzip" ]; then
			local FILE=$(cat ${STATE})
			local DIR=${FILE}.d
			local DIRI=${DIR}.inprogress
			local TARGET=${DIRI}/${CMD:7}
			[ -d ${DIR} ] || exec-logged "mkdir -p ${TARGET} && unzip -d ${TARGET} -q ${FILE} && mv ${DIRI} ${DIR}"
			STATE=$(exec-memoized "printf ${DIR}")
		else
			STATE=$(exec-memoized "cat ${STATE} | ${CMD}")
		fi
		if [ ${?} != 0 ]; then
			echo FAIL >&2
			return 1
		fi
	done
	cat ${STATE}
}

# executes a command if not executed before; otherwise, return previous result
exec-memoized() {
	local CMD="${@}"
	local FP=$(md5-dir "${CMD}")
	mkdir -p ${FP}/

	local KF=${FP}.k
	local VF=${FP}.v

	if [ "${CACHE}" != "off" ] && [ -f ${KF} ] && diff <(printf "${CMD}") <(cat ${KF}); then
		true
	else
		exec-logged "${CMD}" | tee ${VF} 1>&2
		local RC=${PIPESTATUS[0]}
		[ ${RC} == 0 ] && printf "${CMD}" > ${KF}
	fi

	printf ${VF}
	return ${RC}
}

exec-logged() {
	local CMD="${@}"
	echo "START ${CMD}" >&2
	sh -c "${CMD}"
	local RC=${?}
	echo "END~${RC} ${CMD}" >&2
	return ${RC}
}

md5-dir() {
	local CMD="${@}"
	local MD5=$(printf "${CMD}" | md5sum - | cut -d" " -f1)
	local P=${MD5:0:2}
	local DIR=${CCACHE}/${P}
	echo ${DIR}/${MD5}
}

url-dir() {
	local MD5=$(printf "${1}" | md5sum - | cut -d" " -f1)
	local SHORT=$(printf "${1}" | url-dir-name)
	echo ${MD5:0:8}.${SHORT}
}

url-dir-name() {
	tr /:@ _ | tr -dc '[\-.0-9A-Z_a-z]'
}
