DEPS=$(find src/ -name \*.java |
xargs grep -h ^import |
tr -d \; |
sort |
uniq |
sed 's#import ch.qos.logback.*#<groupId>ch.qos.logback</groupId><artifactId>logback-classic</artifactId>#g' |
sed 's#import com.fasterxml.jackson.databind.*#<groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId>#g' |
sed 's#import com.jcraft.jsch.*#<groupId>com.jcraft</groupId><artifactId>jsch</artifactId>#g' |
sed 's#import com.nativelibs4java.bridj.*#<groupId>com.nativelibs4java</groupId><artifactId>bridj</artifactId>#g' |
sed 's#import com.nativelibs4java.opencl.*#<groupId>com.nativelibs4java</groupId><artifactId>javacl</artifactId>#g' |
sed 's#import com.sun.jna.*#<groupId>net.java.dev.jna</groupId><artifactId>jna</artifactId>#g' |
sed 's#import io.vertx.*#<groupId>io.vertx</groupId><artifactId>vertx-core</artifactId>#g' |
sed 's#import javax.mail.*#<groupId>javax.mail</groupId><artifactId>mail</artifactId>#g' |
sed 's#import jline.console.*#<groupId>jline</groupId><artifactId>jline</artifactId>#g' |
sed 's#import net.spy.memcached.*#<groupId>net.spy</groupId><artifactId>spymemcached</artifactId>#g' |
sed 's#import org.apache.bcel.*#<groupId>org.apache.bcel</groupId><artifactId>bcel</artifactId>#g' |
sed 's#import org.apache.commons.logging.*#<groupId>commons-logging</groupId><artifactId>commons-logging</artifactId>#g' |
sed 's#import org.apache.log4j.*#<groupId>log4j</groupId><artifactId>log4j</artifactId>#g' |
sed 's#import org.apache.http.client.*#<groupId>org.apache.httpcomponents</groupId><artifactId>httpclient</artifactId>#g' |
sed 's#import org.junit.*#<groupId>junit</groupId><artifactId>junit</artifactId>#g' |
sed 's#import org.objectweb.asm.Opcodes*#<groupId>org.ow2.asm</groupId><artifactId>asm</artifactId>#g' |
sed 's#import org.objectweb.asm.util.*#<groupId>org.ow2.asm</groupId><artifactId>asm-util</artifactId>#g' |
sed 's#import org.slf4j.*#<groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId>#g' |
sed 's#import org.telegram.telegrambots.bots.*#<groupId>org.telegram</groupId><artifactId>telegrambots</artifactId>#g' |
sed 's#import redis.clients.*#<groupId>redis.clients</groupId><artifactId>Jedis</artifactId>#g' |
sort |
uniq |
grep '<artifactId>' |
while read DEP; do
  echo -n "<dependency>${DEP}<version>LATEST</version></dependency>"
done) &&

mv pom.xml pom0.xml &&
cat pom0.xml |
tr '\n' '@' |
sed "s#<dependencies>.*</dependencies>#<dependencies>${DEPS}</dependencies>#g" |
tr '@' '\n' |
xmllint --format - > pom.xml

mvn -DdownloadJavadocs=true -DdownloadSources=true eclipse:clean eclipse:eclipse dependency:resolve
