ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-kinesis-target.tar.gz .

CMD ["./aws-kinesis-target/bin/aws-kinesis-target",  "-envport", "PORT", "-info"]