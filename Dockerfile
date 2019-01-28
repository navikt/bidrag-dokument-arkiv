FROM navikt/java:11
LABEL maintainer="Team Bidrag" \
      email="nav.ikt.prosjekt.og.forvaltning.bidrag@nav.no"

ADD ./target/bidrag-dokument-arkiv*.jar app.jar

EXPOSE 8080
