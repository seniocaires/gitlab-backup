FROM maven AS builder

ADD . /build

WORKDIR /build

RUN mvn dependency:resolve verify package

FROM java:8

WORKDIR /app

COPY --from=builder /build/target/gitlab-backup-jar-with-dependencies.jar /app/run.jar

ADD entrypoint /usr/local/container/entrypoint

RUN chmod +x /usr/local/container/entrypoint

ENV PATH /usr/local/container:$PATH

ENTRYPOINT ["entrypoint"]