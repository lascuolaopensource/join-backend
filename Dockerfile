ARG BASE_IMAGE_TAG="8u212-b04-jdk-stretch"
ARG SBT_VERSION="1.3.3" 
ARG SCALA_VERSION="2.13.1" 

FROM mozilla/sbt

WORKDIR /root

ADD . join-backend/

WORKDIR join-backend/

RUN mv backend-variables.conf conf/env/prod.conf \
&& sbt stage

CMD target/universal/stage/bin/sos -Dconfig.resource=env/prod.conf -Dhttp.port=9000 -Dlogger.resource=logback.xml -Dplay.evolutions.db.default.autoApply=true -Dplay.http.secret.key='QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241ABR5W:1uDFN];Ik@n'