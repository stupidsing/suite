replace() {
  sed 's/Board/Noun/g' |
  sed 's/board/noun/g' |
  cat
}

replace-file() {
  F0="${1}"
  F1=$(echo "${F0}" | replace)
  mkdir -p $(dirname "${F1}")
  cat "${F0}" | replace > /tmp/replaced
  mv /tmp/replaced "${F1}"
  echo ${F1}
}

while [ "${1}" ]; do
  F0="${1}"
  replace-file "${F0}"
  shift
done
