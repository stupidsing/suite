DEPS=$(find src/ -name \*.java |
xargs grep -h ^import |
cut -d' ' -f2 |
tr -d \; |
sort |
uniq |
sed 's#com.fasterxml.jackson.databind.*#<groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId>#g' |
sed 's#com.jcraft.jsch.*#<groupId>com.jcraft</groupId><artifactId>jsch</artifactId>#g' |
sed 's#com.nativelibs4java.bridj.*#<groupId>com.nativelibs4java</groupId><artifactId>bridj</artifactId>#g' |
sed 's#com.nativelibs4java.opencl.*#<groupId>com.nativelibs4java</groupId><artifactId>javacl</artifactId>#g' |
sed 's#jline.console.*#<groupId>jline</groupId><artifactId>jline</artifactId>#g' |
sed 's#org.apache.commons.logging.*#<groupId>commons-logging</groupId><artifactId>commons-logging</artifactId>#g' |
sed 's#org.apache.log4j.*#<groupId>log4j</groupId><artifactId>log4j</artifactId>#g' |
sed 's#org.junit.*#<groupId>junit</groupId><artifactId>junit</artifactId>#g' |
sort | uniq |
grep artifactId |
while read DEP; do
  echo -n "<dependency>${DEP}<version>LATEST</version></dependency>"
done) &&

mv pom.xml pom0.xml &&
cat pom0.xml |
tr '\n' '@' |
sed "s#<dependencies>.*</dependencies>#<dependencies>${DEPS}</dependencies>#g" |
tr '@' '\n' |
xmllint --format - > pom.xml
