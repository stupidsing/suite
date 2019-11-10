# cchs "ls -al" "sort" "tail -3"

cchs() {
	F=/dev/null
	while [ "${1}" ]; do
		CMD="${1}"
		shift
		F=$(cchf "cat ${F} | ${CMD}")
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
