# cchs "ls -al" "sort" "tail -3"

cchs() {
	F=/dev/null
	while [ "${1}" ]; do
		CMD="${1}"
		shift
		if [ "${CMD}" == "#curl" ]; then
			URL=$(cat ${F})
			MD5=$(printf "${CMD}" | md5sum - | cut -d' ' -f1)
			SHORT=$(printf "${URL}" | tr /: _ | tr -dc '[\-.0-9A-Z_a-z]')
			DF="/data/tmp/${MD5}.${SHORT}"
			F=$(cchf "curl -sL '${URL}' > ${DF} && printf ${DF}")
		elif [ "${CMD}" == "#tar-zxf" ]; then
			TARF=$(cat ${F})
			TARDIR=${TARF}.d
			F=$(cchf "mkdir -p ${TARDIR} && tar zxf ${TARF} -C ${TARDIR} && printf ${TARDIR}")
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
