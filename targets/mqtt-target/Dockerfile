ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc
ADD build/distributions/mqtt-target.tar.gz .
CMD ["./mqtt-target/bin/mqtt-target",  "-envport", "PORT", "-trace"]