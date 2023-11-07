ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/pccc.tar.gz .

CMD ["./pccc/bin/pccc",  "-envport", "PORT"]