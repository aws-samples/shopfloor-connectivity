ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/slmp.tar.gz .

CMD ["./pccc/bin/slmp",  "-envport", "PORT"]