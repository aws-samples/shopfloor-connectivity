ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/nats.tar.gz .

CMD ["./nats/bin/nats",  "-envport", "PORT"]