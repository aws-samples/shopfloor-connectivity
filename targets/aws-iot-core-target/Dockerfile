ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-iot-core-target.tar.gz .

CMD ["./aws-iot-core-target/bin/aws-iot-core-target",  "-envport", "PORT", "-info"]