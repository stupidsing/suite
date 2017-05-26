replace-ci() {
  sed 's/Character/Integer/g' |
  sed 's/Char/Int/g' |
  sed 's/Chr/Int/g' |
  sed 's/character/integer/g' |
  sed 's/char/int/g' |
  cat
}

replace-file() {
  C=${1}
  F0=${2}
  F1=$(echo ${F0} | replace-${C} ${F0})
  cat ${F0} | replace-${C} > /tmp/replaced
  mv /tmp/replaced ${F1}
}

replace-file ci src/main/java/suite/adt/map/ChrObjMap.java
replace-file ci src/main/java/suite/adt/map/ObjChrMap.java
replace-file ci src/main/java/suite/adt/pair/ChrChrPair.java
replace-file ci src/main/java/suite/adt/pair/ChrObjPair.java
replace-file ci src/main/java/suite/primitive/ChrMutable.java
replace-file ci src/main/java/suite/primitive/ChrObjFunUtil.java
replace-file ci src/main/java/suite/primitive/ChrPrimitiveFun.java
replace-file ci src/main/java/suite/primitive/ChrPrimitivePredicate.java
replace-file ci src/main/java/suite/primitive/ChrPrimitiveSink.java
replace-file ci src/main/java/suite/primitive/ChrPrimitiveSource.java
replace-file ci src/main/java/suite/primitive/ChrRethrow.java
replace-file ci src/main/java/suite/primitive/Chars.java
replace-file ci src/main/java/suite/primitive/Chars_.java
replace-file ci src/main/java/suite/streamlet/ChrObjOutlet.java
replace-file ci src/main/java/suite/streamlet/ChrObjStreamlet.java
