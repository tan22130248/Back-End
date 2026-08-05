FROM eclipse-temurin:17-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && ffmpeg -version

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=45.0 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.awt.headless=true"
ENV SPRING_JPA_OPEN_IN_VIEW=false
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=none
ENV SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=5000
ENV SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=5000
ENV SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=5000
ENV SERVER_TOML_HTTP_MAX-THREADS=50
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_TOOL_OPTIONS -jar target/*.jar"]
