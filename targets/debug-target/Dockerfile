ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/debug-target.tar.gz .

CMD ["./debug-target/bin/debug-target",  "-envport", "PORT"]