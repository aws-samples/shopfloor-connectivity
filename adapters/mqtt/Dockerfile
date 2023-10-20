ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/mqtt.tar.gz .

CMD ["./mqtt/bin/mqtt",  "-envport", "PORT"]