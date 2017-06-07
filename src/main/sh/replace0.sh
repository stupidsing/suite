replace-word() {
  FROM="${1}"
  shift
  TO="${1}"
  shift
  FILES=$(find $* -print0 | xargs -0 grep -l "${FROM}")
  echo ${FILES}
  echo ${FILES} | xargs sed "s/${FROM}/${TO}/g" -i
}

replace_() {
  REPLACER="${1}"
  F0="${2}"
  F1=$(echo "${F0}" | sh -c "${REPLACER}")
  mkdir -p $(dirname "${F1}")
  cat "${F0}" | sh -c "${REPLACER}" > /tmp/replaced &&
  (! [ "${REMOVEOLD}" ] || rm -f "${F0}") &&
  mv /tmp/replaced "${F1}"
  echo ${F1}
}

replace() {
  REPLACER="${1}"
  shift
  while [ "${1}" ]; do
    F0="${1}"
    shift
    replace_ "${REPLACER}" "${F0}"
  done
}

replace-files() {
  while read F; do
    replace_ "${1}" "${F}"
  done
}

# replace "sed 's/board/Board/g'" file.txt
# find -name file\*.txt -type f | replace-files "sed 's/chalk/Chalk/g' | sed 's/board/Board/g'"
