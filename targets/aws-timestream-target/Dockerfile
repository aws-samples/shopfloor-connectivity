ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-timestream-target.tar .

CMD ["./aws-timestream-target/bin/aws-timestream-target", "-envport", "PORT", "-info", "-info"]