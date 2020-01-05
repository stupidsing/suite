# mvn -Dmaven.test.skip=true install assembly:single && docker build -t suite:latest . && docker run -e USER=${USER} -p 25 -p 8051 suite:latest

FROM openjdk:13-alpine
ADD target/suite-1.0-jar-with-dependencies.jar suite-1.0-jar-with-dependencies.jar
ENTRYPOINT ["sh", "-c"]
CMD ["java ${JAVA_OPTS} -cp suite-1.0-jar-with-dependencies.jar suite.ServerMain"]
