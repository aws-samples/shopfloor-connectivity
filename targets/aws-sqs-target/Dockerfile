ARG baseimage=amazoncorretto:19
FROM ${baseimage}

WORKDIR /sfc

ADD build/distributions/aws-sqs-target.tar.gz .

CMD ["./aws-sqs-target/bin/aws-sqs-target", "-envport", "PORT", "-info", "-info"]