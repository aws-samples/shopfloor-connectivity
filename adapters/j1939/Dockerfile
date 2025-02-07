ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/j1939.tar.gz .

CMD ["./j1939/bin/j1939",  "-envport", "PORT"]