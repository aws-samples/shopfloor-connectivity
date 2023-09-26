ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-kinesis-firehose-target.tar .

CMD ["./aws-kinesis-firehose-target/bin/aws-kinesis-firehose-target", "-envport", "PORT", "-info"]