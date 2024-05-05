FROM eclipse-temurin:21-jdk-alpine
COPY build/libs/*.jar app.jar
RUN echo "keytool -trustcacerts -keystore '${JAVA_HOME}/lib/security/cacerts' -storepass crazypass -importcert -noprompt -alias -tmws -file /certificates/ws-trackmania-com.crt &&\
java -jar app.jar" > run.sh &&\
chmod +x run.sh
EXPOSE 8080

ENTRYPOINT "/run.sh"
