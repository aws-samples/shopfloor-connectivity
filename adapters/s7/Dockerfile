ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/s7.tar .

CMD ["./s7/bin/s7",  "-envport", "PORT"]