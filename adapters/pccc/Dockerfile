ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/pccc.tar .

CMD ["./pccc/bin/pccc",  "-envport", "PORT"]