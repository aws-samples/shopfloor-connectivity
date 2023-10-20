ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD ./build/distributions/aws-cloudwatch-metrics.tar.gz .

CMD ["./aws-cloudwatch-metrics/bin/aws-cloudwatch-metrics", "-envport", "PORT", "-info", "-info"]