ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/rest.tar.gz .

CMD ["./sql/bin/rest",  "-envport", "PORT"]