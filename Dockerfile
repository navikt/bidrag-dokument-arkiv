FROM busybox:1.36.1-uclibc as busybox

FROM gcr.io/distroless/java21-debian12:nonroot
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY --from=busybox /bin/sh /bin/sh
COPY --from=busybox /bin/printenv /bin/printenv

WORKDIR /app

COPY ./target/app.jar app.jar

EXPOSE 8080
ARG JDK_JAVA_OPTIONS
ENV TZ="Europe/Oslo"
ENV SPRING_PROFILES_ACTIVE=nais
ENV JDK_JAVA_OPTIONS="-Dhttp.proxyHost=webproxy.nais \
                -Dhttps.proxyHost=webproxy.nais \
                -Dhttp.proxyPort=8088 \
                -Dhttps.proxyPort=8088 \
                -Dhttp.nonProxyHosts=localhost|127.0.0.1|10.254.0.1|*.local|*.adeo.no|*.nav.no|*.aetat.no|*.devillo.no|*.oera.no|*.nais.io|*.aivencloud.com|*.intern.dev.nav.no"
CMD ["app.jar"]