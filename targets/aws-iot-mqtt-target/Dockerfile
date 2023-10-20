ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc
ADD build/distributions/aws-iot-mqtt-target.tar.gz .
CMD ["./aws-iot-mqtt-target/bin/aws-iot-mqtt-target",  "-envport", "PORT", "-trace"]