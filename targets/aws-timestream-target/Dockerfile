ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-timestream-target.tar.gz .

CMD ["./aws-timestream-target/bin/aws-timestream-target", "-envport", "PORT", "-info", "-info"]