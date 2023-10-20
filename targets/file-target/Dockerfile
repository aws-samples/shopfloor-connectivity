ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/file-target.tar.gz .

CMD ["./file-target/bin/file-target",  "-envport", "PORT"]