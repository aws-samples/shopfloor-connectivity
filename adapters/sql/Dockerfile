ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/sql.tar.gz .

CMD ["./sql/bin/sql",  "-envport", "PORT"]