ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-iot-http-target.tar.gz .

CMD ["./aws-iot-http-target/bin/aws-iot-http-target",  "-envport", "PORT", "-info"]