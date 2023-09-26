ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-iot-analytics-target.tar .

CMD ["./aws-iot-analytics-target/bin/aws-iot-analytics-target",  "-envport", "PORT", "-info"]