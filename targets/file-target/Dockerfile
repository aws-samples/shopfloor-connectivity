ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/file-target.tar .

CMD ["./file-target/bin/file-target",  "-envport", "PORT"]