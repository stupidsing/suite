find $(dirname "${0}")/ -name \*.c -type f | xargs cat | grep '// gcc ' | cut -c4-999 | xargs -I {}  sh -c "{}"
