ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc
ADD build/distributions/nats-target.tar.gz .
CMD ["./nats-target/bin/nats-target",  "-envport", "PORT", "-trace"]