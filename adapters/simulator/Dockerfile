ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/simulator.tar.gz .

CMD ["./sql/bin/simulator",  "-envport", "PORT"]