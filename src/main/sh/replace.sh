replace() {
  sed 's/Character/Integer/g' |
  sed 's/Char/Int/g' |
  sed 's/character/integer/g' |
  sed 's/char/int/g' |
  cat
}

replace-file() {
  F0=${1}
  F1=$(echo ${F0} | replace ${F0})
  cat ${F0} | replace > /tmp/replaced
  mv /tmp/replaced ${F1}
}

replace-file src/main/java/suite/adt/pair/CharCharPair.java
replace-file src/main/java/suite/adt/pair/CharObjPair.java
replace-file src/main/java/suite/primitive/CharMutable.java
replace-file src/main/java/suite/primitive/CharObjFunUtil.java
replace-file src/main/java/suite/primitive/CharPrimitiveFun.java
replace-file src/main/java/suite/primitive/CharPrimitivePredicate.java
replace-file src/main/java/suite/primitive/CharPrimitiveSink.java
replace-file src/main/java/suite/primitive/CharPrimitiveSource.java
replace-file src/main/java/suite/primitive/CharRethrow.java
#replace-file src/main/java/suite/primitive/Chars.java
#replace-file src/main/java/suite/primitive/Chars_.java
replace-file src/main/java/suite/streamlet/CharObjOutlet.java
replace-file src/main/java/suite/streamlet/CharObjStreamlet.java
