# cchs "ls -al" "sort" "tail -3"

cchs() {
	F=/dev/null
	while [ "${1}" ]; do
		CMD="${1}"
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			D=$(cat ${F})
			F=$(cchf "${D}${CMD:2}")
		elif [ "${CMD}" == "#curl" ]; then
			URL=$(cat ${F})
			MD5=$(printf "${CMD}" | md5sum - | cut -d' ' -f1)
			SHORT=$(printf "${URL}" | tr /: _ | tr -dc '[\-.0-9A-Z_a-z]')
			DF="/data/tmp/${MD5}.${SHORT}"
			F=$(cchf "curl -sL '${URL}' > ${DF} && printf ${DF}")
		elif [ "${CMD}" == "#dir" ]; then
			DIR=$(cat ${F})
			F=$(cchf "sh -c 'readlink -f ${DIR}/*'")
		elif [ "${CMD}" == "#git-clone" ]; then
			URL=$(cat ${F})
			MD5=$(printf "${CMD}" | md5sum - | cut -d' ' -f1)
			SHORT=$(printf "${URL}" | tr /: _ | tr -dc '[\-.0-9A-Z_a-z]')
			DF="/data/tmp/${MD5}.${SHORT}"
			F=$(cchf "git clone --depth 1 '${URL}' ${DF} && printf ${DF}")
		elif [ "${CMD:0:5}" == "#tar-" ]; then
			OPT=${CMD:5}
			TARF=$(cat ${F})
			TARDIR=${TARF}.d
			F=$(cchf "mkdir -p ${TARDIR} && tar ${OPT} ${TARF} -C ${TARDIR} && printf ${TARDIR}")
		else
			F=$(cchf "cat ${F} | ${CMD}")
		fi
	done
	cat ${F}
}

cchf() {
	CMD="${@}"
	MD5=$(printf "${CMD}" | md5sum - | cut -d' ' -f1)
	P=${MD5:0:2}
	DIR=~/.cmd-cache/${P}
	FP=${DIR}/${MD5}
	KF=${FP}.k
	VF=${FP}.v

	mkdir -p ${DIR}
	find ~/.cmd-cache/${P} -mtime 28 -print0 | xargs -0 rm -f

	if [ -f "${KF}" ] && diff <(printf "${CMD}") <(cat "${KF}"); then
		true
	else
		echo "START ${CMD}" >&2
		(sh -c "${CMD}" > "${VF}") && (printf "${CMD}" > "${KF}")
		echo "END~${?} ${CMD}" >&2
	fi

	printf "${VF}"
}
