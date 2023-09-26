ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/debug-target.tar .

CMD ["./debug-target/bin/debug-target",  "-envport", "PORT"]