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
		local EX
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			local DIR=$(cat ${STATE})
			EX="${DIR}${CMD:2}"
		elif [ "${CMD:0:3}" == "@cd" ]; then
			local DIR=$(cat ${STATE})
			EX="cd ${DIR}/ && ${CMD:4}"
		elif [ "${CMD}" == "@curl" ]; then
			local URL=$(cat ${STATE})
			local FILE=${DCACHE}/$(url-dir "${URL}")
			local FILEI=${FILE}.inprogress
			[ -f ${FILE} ] || exec-logged "curl -sL '${URL}' > ${FILEI} && mv ${FILEI} ${FILE}"
			EX="printf %s ${FILE}"
		elif [ "${CMD}" == "@dir" ]; then
			local DIR=$(cat ${STATE})
			local LINK=$(sh -c "readlink -f ${DIR}/*")
			EX="printf %s ${LINK}"
		elif [ "${CMD:0:6}" == "@do-cd" ]; then
			local DIR=$(cat ${STATE})
			EX="cd ${DIR}/ && (${CMD:7}) 1>&2 && printf %s ${DIR}"
		elif [ "${CMD:0:9}" == "@do-chmod" ]; then
			local FILE=$(cat ${STATE})
			chmod ${CMD:10} ${FILE}
			EX="printf %s ${FILE}"
		elif [ "${CMD:0:10}" == "@do-git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			EX="V=${GIT:0:8}; cd ${DIR}/ && (${CMD:11}) 1>&2 && printf %s ${GIT}"
		elif [ "${CMD:0:5}" == "@exec" ]; then
			local DIR=$(cat ${STATE})
			local PREFIX=$(md5-dir "${DIR}:${CMD}")
			local O=${PREFIX}.o U=${PREFIX}.u W=${PREFIX}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			EX="cd ${O}/; (${CMD:6}) 1>&2 && printf %s ${O}"
			#choverlayx
		elif [ "${CMD}" == "@docker-build" ]; then
			local FILE=${STATE}
			local IMAGE=${CMD:13:}-$(cat ${FILE} | md5sum - | cut -d" " -f1)
			EX="cat ${FILE} | docker build -q -t cchs/${IMAGE} -"
		elif [ "${CMD:0:7}" == "@git-cd" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			EX="V=${GIT:0:8}; cd ${DIR}/ && ${CMD:8}"
		elif [ "${CMD:0:10}" == "@git-clone" ]; then
			local URL=$(cat ${STATE})
			local BRANCH=${CMD:11}
			[ "${BRANCH}" ] && local OPTS="-b ${BRANCH}"
			local DF=${DCACHE}/$(url-dir "${URL}@${BRANCH}")
			if [ "${CACHE}" == "off" ]; then
				mv ${DF}/ $(mktemp -d)
			fi
			if ! [ -d ${DF}/ ]; then
				exec-logged "git clone ${OPTS} ${URL} ${DF}/ --quiet"
				touch ${DF}.pulltime
			fi
			local TS0=$(date +%s)
			local TS1=$(stat -c %Y ${DF}.pulltime)
			if (( 900 < ${TS0} - ${TS1} )); then
				exec-logged "cd ${DF}/ && git pull --force --quiet"
				touch ${DF}.pulltime
			fi
			local COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			EX="printf %s ${COMMIT}:${DF}"
		elif [ "${CMD:0:9}" == "@git-exec" ]; then
			local GIT=$(cat ${STATE})
			local DIR=${GIT:9}
			local PREFIX=$(md5-dir "${GIT}:${CMD}")
			local O=${PREFIX}.o U=${PREFIX}.u W=${PREFIX}.w
			mkdir -p ${U}/ ${O}/ ${W}/
			mountpoint -q ${O}/ || WORKDIR=${W}/ choverlay_ ${DIR}/ ${U}/ ${O}/
			EX="V=${GIT:0:8}; cd ${DIR}/ && (${CMD:10}) 1>&2 && printf %s ${GIT}"
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
			EX="printf %s ${FILE}"
		elif [ "${CMD:0:6}" == "@mkdir" ]; then
			local NAME=$(cat ${STATE})
			local DIR=${DCACHE}/$(url-dir "${NAME}")
			mkdir -p ${DIR}
			EX="printf %s ${DIR}"
		elif [ "${CMD:0:5}" == "@tar-" ]; then
			local OPT=${CMD:5}
			local FILE=$(cat ${STATE})
			local DIR=${FILE}.d
			local DIRI=${DIR}.inprogress
			[ -d ${DIR} ] || exec-logged "mkdir -p ${DIRI} && tar ${OPT} ${FILE} -C ${DIRI} && mv ${DIRI} ${DIR}"
			EX="printf %s ${DIR}"
		elif [ "${CMD:0:6}" == "@unzip" ]; then
			local FILE=$(cat ${STATE})
			local DIR=${FILE}.d
			local DIRI=${DIR}.inprogress
			local TARGET=${DIRI}/${CMD:7}
			[ -d ${DIR} ] || exec-logged "mkdir -p ${TARGET} && unzip -d ${TARGET} -q ${FILE} && mv ${DIRI} ${DIR}"
			EX="printf %s ${DIR}"
		else
			EX="cat ${STATE} | ${CMD}"
		fi

		STATE=$(exec-memoized "${EX}")

		if [ ${?} != 0 ]; then
			echo FAIL >&2
			return 1
		fi
	done
	cat ${STATE}
}

choverlay_() {
	local L0=${1} UPPERDIR=${2} L1=${3}
	local NAME0=$(echo "${L0}" | sed s#/#_#g)
	local NAME1=$(echo "${L1}" | sed s#/#_#g)
	local METAFILE0=/tmp/choverlay.${NAME0}
	local METAFILE1=/tmp/choverlay.${NAME1}
	[ -f "${METAFILE0}" ] && local LDS0=$(cat ${METAFILE0}) || local LDS0=${L0}
	local LDS1=${LDS0}:${UPPERDIR}
	echo ${LDS1} > ${METAFILE1}
	if [ "${LOCAL}" ]; then
		fuse-overlayfs -o lowerdir=${LDS0},upperdir=${UPPERDIR},workdir=${WORKDIR-$(mktemp -d)} ${L1}
	else
		sudo mount -t overlay stack_${NAME1} -o lowerdir=${LDS0},upperdir=${UPPERDIR},workdir=${WORKDIR-$(mktemp -d)} ${L1}
	fi
	pushd ${L1}/ > /dev/null
}

# executes a command if not executed before; otherwise, return previous result
exec-memoized() {
	local CMD="${@}"
	local FP=$(md5-dir "${CMD}")
	mkdir -p ${FP}/

	local KF=${FP}.k
	local VF=${FP}.v

	if [ "${CACHE}" != "off" ] && [ -f ${KF} ] && diff <(printf %s "${CMD}") <(cat ${KF}); then
		true
	else
		exec-logged "${CMD}" | tee ${VF} 1>&2
		local RC=${PIPESTATUS[0]}
		[ ${RC} == 0 ] && printf %s "${CMD}" > ${KF}
	fi

	printf %s ${VF}
	return ${RC}
}

exec-logged() {
	local CMD="${@}"
	echo >&2
	echo "START ${CMD}" >&2
	sh -c "${CMD}"
	local RC=${?}
	echo >&2
	echo "END~${RC} ${CMD}" >&2
	return ${RC}
}

md5-dir() {
	local CMD="${@}"
	local MD5=$(printf %s "${CMD}" | md5sum - | cut -d" " -f1)
	local P=${MD5:0:2}
	local DIR=${CCACHE}/${P}
	echo ${DIR}/${MD5}
}

url-dir() {
	local MD5=$(printf %s "${1}" | md5sum - | cut -d" " -f1)
	local SHORT=$(printf %s "${1}" | url-dir-name)
	echo ${MD5:0:8}.${SHORT}
}

url-dir-name() {
	tr /:@ _ | tr -dc '[\-.0-9A-Z_a-z]'
}
